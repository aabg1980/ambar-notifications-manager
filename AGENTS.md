# AGENTS.md

## Qué es este repo

Microservicio para gestionar **campañas de notificaciones** multicanal (email, WhatsApp y otros).

Estado actual: **greenfield**. Sólo existen `LICENSE` y `.gitignore`. No hay código, build ni tests que verificar — todo lo de abajo son convenciones del proyecto a seguir al crear código.

## Stack

- Java 25, Spring Boot 4.1
- Gradle (wrapper `./gradlew` cuando exista)
- No hay servicios externos ni credenciales configurables aún

## Ramas

- Trabajar siempre en `feature/*` creada desde `develop`
- `master` es sólo para releases; no hacer commits directos
- Ejemplo: `git checkout develop && git checkout -b feature/campanas-batch`

## Arquitectura (a implementar)

**Clean architecture + slice architecture por feature.** Cada feature es un slice vertical autocontenido; dentro de cada slice se aplica clean architecture (las dependencias apuntan hacia el dominio). Hay una definición de dominio compartida y un slice `common`.

**Al diseñar o crear código, sigue [`docs/architecture-scaffolding.md`](docs/architecture-scaffolding.md)** — convención de directorios, estándar de nombres y reglas de dependencia. Es la fuente de verdad; este archivo sólo resume.

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
