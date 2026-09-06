#!/usr/bin/env bash

set -Eeuo pipefail

BASE_URL="${BASE_URL:-http://localhost:3000}"
API_URL="${BASE_URL%/}/api/orders"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-1}"
MAX_POLL_ATTEMPTS="${MAX_POLL_ATTEMPTS:-30}"

fail() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

log() {
    printf '==> %s\n' "$1"
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "Se requiere el comando '$1'."
}

assert_json() {
    local document="$1"
    local expression="$2"
    local message="$3"

    jq -e "$expression" >/dev/null <<<"$document" || fail "$message"
}

cleanup() {
    rm -f -- "$PUBLIC_KEY_FILE"
    rmdir -- "$TEMP_DIRECTORY" 2>/dev/null || true
}

require_command curl
require_command jq
require_command openssl

TEMP_DIRECTORY="$(mktemp -d)"
PUBLIC_KEY_FILE="$TEMP_DIRECTORY/public-key.pem"
trap cleanup EXIT

log "Comprobando frontend y obteniendo la clave pública"
curl --fail --silent --show-error "${BASE_URL%/}/" >/dev/null
curl --fail --silent --show-error "${BASE_URL%/}/public-key.pem" --output "$PUBLIC_KEY_FILE"
openssl pkey -pubin -in "$PUBLIC_KEY_FILE" -noout >/dev/null 2>&1 \
    || fail "La clave pública servida no es un PEM SPKI válido."

encrypt_card_data() {
    local card_json

    card_json="$(jq -cn \
        --arg cardNumber "4111111111111111" \
        --arg expiration "12/30" \
        --arg cvv "123" \
        '{cardNumber: $cardNumber, expiration: $expiration, cvv: $cvv}')"

    printf '%s' "$card_json" \
        | openssl pkeyutl -encrypt \
            -pubin \
            -inkey "$PUBLIC_KEY_FILE" \
            -pkeyopt rsa_padding_mode:oaep \
            -pkeyopt rsa_oaep_md:sha256 \
            -pkeyopt rsa_mgf1_md:sha256 \
        | openssl base64 -A
}

create_order() {
    local product_name="$1"
    local amount="$2"
    local encrypted_card_data
    local request_body

    encrypted_card_data="$(encrypt_card_data)" \
        || fail "No fue posible cifrar los datos de prueba."
    request_body="$(jq -cn \
        --arg productName "$product_name" \
        --arg amount "$amount" \
        --arg encryptedCardData "$encrypted_card_data" \
        '{
            productName: $productName,
            quantity: 2,
            amount: ($amount | tonumber),
            encryptedCardData: $encryptedCardData
        }')"

    CREATE_RESPONSE="$(curl --fail --silent --show-error \
        --request POST \
        --header 'Content-Type: application/json' \
        --data "$request_body" \
        "$API_URL")" || fail "No fue posible crear la orden."

    unset encrypted_card_data request_body
    assert_json "$CREATE_RESPONSE" '.id > 0' "La creación no devolvió un identificador."
    assert_json "$CREATE_RESPONSE" '.status == "PENDIENTE"' \
        "La orden no fue creada inicialmente como PENDIENTE."
    assert_json "$CREATE_RESPONSE" 'has("encryptedCardData") | not' \
        "La respuesta pública expuso encryptedCardData."
}

wait_for_terminal_status() {
    local order_id="$1"
    local attempt
    local response
    local status

    for ((attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt += 1)); do
        response="$(curl --fail --silent --show-error "$API_URL/$order_id")" \
            || fail "No fue posible consultar la orden $order_id."
        status="$(jq -r '.status' <<<"$response")"

        if [[ "$status" == "PAGADO" || "$status" == "FALLO_PAGO" ]]; then
            FINAL_RESPONSE="$response"
            FINAL_STATUS="$status"
            return
        fi

        [[ "$status" == "PENDIENTE" ]] \
            || fail "La orden $order_id devolvió el estado inesperado '$status'."
        sleep "$POLL_INTERVAL_SECONDS"
    done

    fail "La orden $order_id no alcanzó un estado terminal."
}

RUN_ID="$(date -u +%Y%m%d%H%M%S)-$$"
PRODUCT_PREFIX="Producto E2E $RUN_ID"
PRODUCT_FILTER="$(printf '%s' "$PRODUCT_PREFIX" | tr '[:upper:]' '[:lower:]')"
PRIMARY_PRODUCT="$PRODUCT_PREFIX Alto"
SECONDARY_PRODUCT="$PRODUCT_PREFIX Bajo"

log "Creando dos órdenes cifradas"
create_order "$PRIMARY_PRODUCT" "129.99"
PRIMARY_ID="$(jq -r '.id' <<<"$CREATE_RESPONSE")"

create_order "$SECONDARY_PRODUCT" "19.99"
SECONDARY_ID="$(jq -r '.id' <<<"$CREATE_RESPONSE")"

log "Esperando la transición asíncrona desde PENDIENTE"
wait_for_terminal_status "$PRIMARY_ID"
PRIMARY_FINAL_STATUS="$FINAL_STATUS"
assert_json "$FINAL_RESPONSE" '.id == '"$PRIMARY_ID" \
    "El detalle final no corresponde a la orden principal."

wait_for_terminal_status "$SECONDARY_ID"
SECONDARY_FINAL_STATUS="$FINAL_STATUS"
assert_json "$FINAL_RESPONSE" '.id == '"$SECONDARY_ID" \
    "El detalle final no corresponde a la orden secundaria."

log "Validando filtro parcial de producto sin distinguir mayúsculas"
FILTER_RESPONSE="$(curl --fail --silent --show-error --get "$API_URL" \
    --data-urlencode "productName=$PRODUCT_FILTER" \
    --data-urlencode 'page=0' \
    --data-urlencode 'size=10' \
    --data-urlencode 'sort=createdAt,desc')" \
    || fail "Falló la consulta filtrada por producto."
jq -e --argjson primaryId "$PRIMARY_ID" --argjson secondaryId "$SECONDARY_ID" \
    '(.items | any(.id == $primaryId)) and (.items | any(.id == $secondaryId))' \
    >/dev/null <<<"$FILTER_RESPONSE" \
    || fail "El filtro de producto no devolvió ambas órdenes."

log "Validando filtro exacto por estado"
STATUS_RESPONSE="$(curl --fail --silent --show-error --get "$API_URL" \
    --data-urlencode "status=$PRIMARY_FINAL_STATUS" \
    --data-urlencode "productName=$PRIMARY_PRODUCT" \
    --data-urlencode 'page=0' \
    --data-urlencode 'size=10')" \
    || fail "Falló la consulta filtrada por estado."
jq -e --argjson orderId "$PRIMARY_ID" --arg status "$PRIMARY_FINAL_STATUS" \
    '.items | any(.id == $orderId and .status == $status)' \
    >/dev/null <<<"$STATUS_RESPONSE" \
    || fail "El filtro de estado no devolvió la orden esperada."

log "Validando paginación y ordenamiento descendente por monto"
FIRST_PAGE="$(curl --fail --silent --show-error --get "$API_URL" \
    --data-urlencode "productName=$PRODUCT_FILTER" \
    --data-urlencode 'page=0' \
    --data-urlencode 'size=1' \
    --data-urlencode 'sort=amount,desc')" \
    || fail "Falló la consulta de la primera página."
SECOND_PAGE="$(curl --fail --silent --show-error --get "$API_URL" \
    --data-urlencode "productName=$PRODUCT_FILTER" \
    --data-urlencode 'page=1' \
    --data-urlencode 'size=1' \
    --data-urlencode 'sort=amount,desc')" \
    || fail "Falló la consulta de la segunda página."

assert_json "$FIRST_PAGE" \
    '.page == 0 and .size == 1 and .totalElements == 2 and .totalPages == 2' \
    "Los metadatos de la primera página no son correctos."
assert_json "$FIRST_PAGE" '.items[0].id == '"$PRIMARY_ID" \
    "El ordenamiento descendente no colocó primero el monto mayor."
assert_json "$SECOND_PAGE" '.page == 1 and .items[0].id == '"$SECONDARY_ID" \
    "La segunda página no contiene la orden de menor monto."

log "Flujo E2E completado correctamente"
printf 'Orden %s: %s\n' "$PRIMARY_ID" "$PRIMARY_FINAL_STATUS"
printf 'Orden %s: %s\n' "$SECONDARY_ID" "$SECONDARY_FINAL_STATUS"
