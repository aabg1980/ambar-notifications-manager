# Ambar Notifications Manager

Microservicio para gestionar **campañas de notificaciones** multicanal (email, WhatsApp y otros).

## Funcionalidades

| Slice | Qué cubre |
|---|---|
| `templates` | CRUD y versionado de plantillas, renderizado de variables, placeholders por versión |
| `campaigns` | Ciclo de vida de campañas (Draft → Scheduled → Executing → Completed / Paused / Cancelled), audiencia y batching |
| `dispatch` | Despacho async masivo, reintentos con backoff, DLQ, throttling por proveedor |
| `providers` | Adaptadores de canales (SendGrid/SES, FCM/APNs, Twilio/Meta), routing y failover |
| `tracking` | Webhooks de proveedores, auditoría por notificación, eventos de dominio |

Además, `common` expone un healthcheck operativo:

```
GET /api/v1/health/ping
→ { "status": "pong", "serviceName": "ambar-notifications-manager", "timestamp": "..." }
```

## Stack

- Java 25 (toolchain Gradle, auto-provisionado)
- Spring Boot 4.1.1
- Gradle 9.8.1 (`./gradlew`)

> **Nota Spring Boot 4:** los starter web son `spring-boot-starter-webmvc` / `spring-boot-starter-webmvc-test`.

## Comandos

```bash
./gradlew build     # compilar + tests
./gradlew test      # solo tests
./gradlew bootRun   # arrancar la app
```

## Arquitectura

Clean architecture + slice architecture por feature (`common/`, `domain/` compartido y `features/<slice>/`).

- Convención de directorios, estándar de nombres y reglas de dependencia: [`docs/architecture-scaffolding.md`](docs/architecture-scaffolding.md)
- Definición de los slices del dominio: [`docs/architecture-slices.md`](docs/architecture-slices.md)
- Guía para agentes / contribuidores: [`AGENTS.md`](AGENTS.md)
