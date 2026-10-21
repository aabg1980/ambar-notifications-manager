# AGENTS.md

## Qué es este repo

Microservicio para gestionar **campañas de notificaciones** multicanal (email, WhatsApp y otros).

Estado actual: scaffold del proyecto Spring Boot + directorios de slices creados; aún no hay lógica de negocio.

## Stack

- Java 25 (toolchain Gradle; se auto-provisiona con foojay-resolver), Spring Boot 4.1.1
- Gradle 9.8.1 via wrapper `./gradlew`
- No hay servicios externos ni credenciales configurables aún

**Quirk Spring Boot 4:** los starter web son `spring-boot-starter-webmvc` / `spring-boot-starter-webmvc-test`, NO `spring-boot-starter-web` (ya no existe con ese nombre).

## Comandos

```bash
./gradlew build        # compilar + tests
./gradlew test         # solo tests
./gradlew bootRun      # arrancar la app
```

## Ramas

- Trabajar siempre en `feature/*` creada desde `develop`
- `master` es sólo para releases; no hacer commits directos
- Ejemplo: `git checkout develop && git checkout -b feature/campanas-batch`

## Arquitectura (a implementar)

**Clean architecture + slice architecture por feature.** Cada feature es un slice vertical autocontenido; dentro de cada slice se aplica clean architecture (las dependencias apuntan hacia el dominio). Hay una definición de dominio compartida y un slice `common`.

**Al diseñar o crear código, sigue [`docs/architecture-scaffolding.md`](docs/architecture-scaffolding.md)** — convención de directorios, estándar de nombres y reglas de dependencia. Los cinco slices del dominio (`templates`, `campaigns`, `dispatch`, `providers`, `tracking`) están definidos en [`docs/architecture-slices.md`](docs/architecture-slices.md). Ambos son la fuente de verdad; este archivo sólo resume.

```
src/main/java/.../
  common/              # slice compartido: utilidades transversales, base compartida
  domain/              # definición de dominio compartida (kernel): Campaign, Notification, ChannelType
  features/
    <feature>/         # un slice por feature (p.ej. campaigns, dispatch, channels)
      domain/          # reglas puras de la feature
      application/     # casos de uso
      ports/           # interfaces hacia el exterior
      adapters/
        in/web/        # controllers REST
        out/           # implementaciones: EmailChannel, WhatsAppChannel, persistencia
      infrastructure/  # config Spring de la feature
```

### Reglas

- **Dependencias hacia dentro**: `domain/` de un slice no importa Spring ni adapters; `application/` sólo su dominio y ports; los adapters implementan los ports.
- **Los slices no se importan entre sí** por sus internos. Lo compartido vive en `domain/` (compartido) o en `common/`.
- Un slice no debe tocar el dominio ni los adapters de otro slice.

### Decisiones clave

- **Cada canal es un adapter** que implementa un puerto común (`NotificationChannel`). Añadir un canal nuevo = añadir un adapter, sin tocar el dominio.
- **Campaña ≠ notificación**: la campaña agrupa destinatarios, plantilla y programación; la notificación es el envío individual a un canal.
- Los tipos de canal se modelan como enum/valor de dominio cerrado (`EMAIL`, `WHATSAPP`, …), no como strings sueltos.

## Tests

- Unit tests sobre `domain/` y `application/` sin levantar Spring
- Tests de adapter con mocks del puerto contrario
- Spring Boot tests sólo para la capa web/infra
- Ubicación espejo bajo `src/test/java/...`

## Estilo

- Seguir convenciones Spring Boot por defecto salvo lo indicado arriba
- Nombres en inglés en código; comentarios y docs de equipo en español
