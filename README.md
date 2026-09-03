# Sistema de pedidos y pagos asíncronos

Monorepositorio para una prueba técnica basada en dos microservicios Spring Boot, Apache Kafka, PostgreSQL y un cliente React. `OrderMS` administrará los pedidos y su estado; `PaymentMS` simulará el cobro y comunicará el resultado de forma asíncrona.

## Estado del proyecto

El proyecto se implementará incrementalmente en seis fases:

1. Infraestructura base.
2. Generación y manejo de claves RSA.
3. Microservicio de pedidos.
4. Microservicio de pagos.
5. Frontend React.
6. Dockerización, validación E2E y documentación final.

## Estructura inicial

- `order-ms/`: API REST y persistencia de órdenes.
- `payment-ms/`: procesamiento asíncrono de pagos.
- `react-front/`: interfaz web del sistema.
- `infra/postgres/init.sql`: esquema inicial de PostgreSQL.
- `tools/rsa-key-generator/`: utilidad local para generar claves RSA.
- `docker-compose.yml`: infraestructura y, en fases posteriores, aplicación completa.

## Infraestructura local

Se requiere Docker con el complemento Docker Compose. Para preparar la configuración local:

```bash
cp .env.example .env
docker compose config
```

Los valores de `.env.example` son únicamente para desarrollo local. Antes de usar esta configuración fuera de un entorno de prueba, se deben sustituir las credenciales y aplicar una gestión segura de secretos.

Para iniciar solamente PostgreSQL, Zookeeper y Kafka:

```bash
docker compose up -d postgres zookeeper kafka
docker compose ps
```

PostgreSQL queda disponible en `localhost:5432` y Kafka en `localhost:9092`, salvo que se cambien sus puertos en `.env`. Durante la primera creación del volumen, PostgreSQL ejecuta `infra/postgres/init.sql` y crea la tabla `orders`, sus restricciones, índices y trigger de actualización.

Para inspeccionar los servicios o detenerlos:

```bash
docker compose logs -f postgres kafka
docker compose down
```

`docker compose down` conserva los volúmenes y sus datos. Use `docker compose down -v` únicamente cuando sea necesario eliminar también la información local.

## Claves RSA locales

PaymentMS requiere una clave privada RSA y el frontend usa la clave pública correspondiente para cifrar los datos de tarjeta en el navegador. Genere el par con Java 17:

```bash
java tools/rsa-key-generator/RsaKeyGenerator.java
```

Por defecto se crean estos archivos:

- `local-keys/public/public-key.pem`: clave pública SPKI que se servirá como `/public-key.pem`.
- `local-keys/private/private-key.pem`: clave privada PKCS#8 que se montará solo en PaymentMS.

Para usar otra ruta durante pruebas:

```bash
java tools/rsa-key-generator/RsaKeyGenerator.java --output-dir /tmp/order-payment-keys
```

La utilidad rechaza sobrescrituras accidentales. Use `--force` únicamente cuando desee reemplazar el par existente:

```bash
java tools/rsa-key-generator/RsaKeyGenerator.java --force
```

La carpeta `local-keys/` está ignorada por Git. No copie claves privadas, datos reales de tarjeta ni secretos al repositorio; en ambientes no locales deben inyectarse mediante una solución segura de secretos.

## Flujo de integración

Cada fase se desarrolla en un branch `feature/NN-descripcion` creado desde el `main` actualizado. Los cambios se dividen en commits atómicos con mensajes Conventional Commits en español. Antes de cada commit se revisan su alcance, pruebas, asunto y descripción; antes de cada merge se ejecutan las comprobaciones de la fase.

Las fases aprobadas se integran mediante `git merge --no-ff`, se vuelven a comprobar en `main` y se publican tanto el branch de trabajo como `main`. Los branches remotos se conservan para mantener trazabilidad.

## Seguridad

Las credenciales incluidas durante el desarrollo serán exclusivamente locales. No se deben versionar claves RSA privadas, datos reales de tarjetas, archivos `.env` ni secretos. Los datos sensibles tampoco deben aparecer en respuestas HTTP o logs.

Las instrucciones de los microservicios, frontend y pruebas integrales se añadirán conforme avance cada fase.
