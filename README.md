# Sistema de pedidos y pagos asíncronos

Aplicación de referencia compuesta por dos microservicios Spring Boot, PostgreSQL, Apache Kafka y un frontend React. OrderMS registra y consulta órdenes; PaymentMS descifra los datos de pago, simula el cobro y publica el resultado de forma asíncrona.

## Arquitectura y flujo

- `order-ms/`: API REST en `/api/orders`, persistencia y eventos Kafka.
- `payment-ms/`: consumidor de órdenes y simulador de pagos, sin API ni base de datos.
- `react-front/`: dashboard React servido por Nginx, con cifrado RSA en el navegador.
- `infra/postgres/init.sql`: esquema, restricciones, índices y trigger de PostgreSQL.
- `tools/rsa-key-generator/`: generador local de claves RSA de 2048 bits.
- `scripts/e2e.sh` y `postman/`: verificaciones integrales reproducibles.

El flujo es: React cifra la tarjeta y crea una orden `PENDIENTE`; OrderMS publica `order-placed`; PaymentMS procesa el pago y publica `payment-processed`; OrderMS actualiza la orden a `PAGADO` o `FALLO_PAGO`.

## Prerrequisitos

- Git y Docker con Docker Compose.
- Java 17 para generar las claves y ejecutar pruebas Maven fuera de Docker.
- Node.js 24 y npm para validar el frontend fuera de Docker.
- `curl`, `jq` y OpenSSL para el script E2E.
- Postman 12 o posterior, opcional para ejecutar la colección.

## Preparación

```bash
git clone https://github.com/dbreness/PR_NUE_ING_BE.git
cd PR_NUE_ING_BE
cp .env.example .env
java tools/rsa-key-generator/RsaKeyGenerator.java
```

Los valores de `.env.example` son solo para desarrollo local. La utilidad crea `local-keys/public/public-key.pem` (SPKI) y `local-keys/private/private-key.pem` (PKCS#8), y rechaza sobrescrituras. Para reemplazar deliberadamente el par use `--force`.

## Ejecución con Docker Compose

Valide la configuración y levante el sistema completo:

```bash
docker compose config
docker compose up --build -d --wait
docker compose ps
```

La interfaz queda disponible en `http://localhost:3000` y la API directa en `http://localhost:8081/api/orders`. Nginx dirige `/api` a OrderMS y publica únicamente la clave pública en `/public-key.pem`. La clave privada se monta como secreto de solo lectura exclusivamente en PaymentMS.

Las variables de `.env` permiten cambiar puertos, credenciales locales, demora y probabilidad de pago. `PAYMENT_SUCCESS_RATE` acepta valores de `0.0` a `1.0`; `PAYMENT_PROCESSING_DELAY_MS` se expresa en milisegundos.

## Pruebas automatizadas

Ejecute las pruebas y compilaciones de cada módulo:

```bash
cd order-ms && ./mvnw test && ./mvnw package && cd ..
cd payment-ms && ./mvnw test && ./mvnw package && cd ..
cd react-front
npm ci
npm run lint
npm test -- --run
npm run build
cd ..
```

Con el stack activo, valide creación cifrada, transición de estado, detalle, filtros, paginación y ordenamiento:

```bash
./scripts/e2e.sh
```

El script usa datos de tarjeta ficticios y admite `BASE_URL`, `POLL_INTERVAL_SECONDS` y `MAX_POLL_ATTEMPTS`. Ejemplo: `BASE_URL=http://localhost:3000 ./scripts/e2e.sh`.

Para Postman, importe `postman/order-processing.postman_collection.json` y ejecute la colección completa en orden. La variable `baseUrl` apunta por defecto a `http://localhost:3000`; los identificadores y el payload cifrado se generan durante la ejecución.

## Operación y diagnóstico

```bash
docker compose logs -f order-ms payment-ms
docker compose logs -f kafka postgres
docker compose down
```

`docker compose down` conserva los volúmenes. Use `docker compose down -v` solamente si desea borrar los datos locales; PostgreSQL ejecuta `init.sql` únicamente al crear un volumen vacío. Si un puerto está ocupado, cambie su valor en `.env` antes de iniciar el stack.

## Seguridad

No use datos reales de tarjeta ni las credenciales de ejemplo fuera del entorno local. Nunca versione `.env`, claves PEM ni secretos. Las respuestas públicas y los logs no deben contener PAN, CVV, expiración, datos descifrados ni `encryptedCardData`.
