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

## Flujo de integración

Cada fase se desarrolla en un branch `feature/NN-descripcion` creado desde el `main` actualizado. Los cambios se dividen en commits atómicos con mensajes Conventional Commits en español. Antes de cada commit se revisan su alcance, pruebas, asunto y descripción; antes de cada merge se ejecutan las comprobaciones de la fase.

Las fases aprobadas se integran mediante `git merge --no-ff`, se vuelven a comprobar en `main` y se publican tanto el branch de trabajo como `main`. Los branches remotos se conservan para mantener trazabilidad.

## Seguridad

Las credenciales incluidas durante el desarrollo serán exclusivamente locales. No se deben versionar claves RSA privadas, datos reales de tarjetas, archivos `.env` ni secretos. Los datos sensibles tampoco deben aparecer en respuestas HTTP o logs.

Las instrucciones completas de instalación, ejecución y pruebas se añadirán conforme avance cada fase.
