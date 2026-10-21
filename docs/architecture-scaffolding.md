# Scaffolding de arquitectura

Convención de directorios y estándar de nombres. Al diseñar o crear código, la estructura y los nombres salen de este documento.

## Layout raíz

```
src/main/java/<base package>/
  common/                # slice transversal (utilidades compartidas, sin lógica de features)
  domain/                # definición de dominio compartida (kernel)
  features/
    <feature>/           # un slice vertical por feature
      domain/
      application/
      ports/
      adapters/
        in/rest/       # controllers
          dto/         # *Request / *Response
        out/
      infrastructure/
  AmbarNotificationsManagerApplication.java   # única clase de arranque Spring Boot

src/main/resources/
  application.yml          # config base
  application-<env>.yml    # config por perfil

src/test/java/<base package>/   # espejo exacto de src/main/java
docs/                           # documentación de arquitectura y decisiones
```

`<base package>` sigue la convención inversa de dominio (p.ej. `com.ambarsoluciones.notifications`).

## Slice por feature

- Un slice = una capacidad del negocio (`campaigns`, `dispatch`, `channels`, …), no una capa técnica.
- Todo lo de la feature vive dentro de su directorio. Si algo se necesita en dos features, sube a `common/` o a `domain/` compartido — no se importa entre features.
- Dentro del slice se aplica clean architecture: `domain/` → `application/` → `ports/` → `adapters/`/`infrastructure/`. Las flechas de dependencia van hacia `domain/`.

| Directorio | Contenido | Puede importar |
|---|---|---|
| `domain/` | entidades, value objects, reglas puras | solo JDK y `domain/` compartido |
| `application/` | casos de uso, orquestación | `domain/`, `ports/` |
| `ports/` | interfaces hacia el exterior | `domain/` |
| `adapters/in/rest/` | controllers REST | `application/`, `ports/` |
| `adapters/in/rest/dto/` | DTOs de entrada/salida (`*Request` / `*Response`) | solo JDK |
| `adapters/out/` | persistencia, clientes de canal | `ports/`, `domain/` |
| `infrastructure/` | config Spring, beans, propiedades | cualquier paquete del slice |
| `common/` | utilidades transversales y endpoints operativos (health) sin estado de feature | JDK; sus `adapters/` pueden usar Spring |

## Estándar de nombres

### Paquetes y clases

- Paquetes: `lowercase` sin separadores (`campaigns`, `adapters`, `in.rest`).
- Clases: `PascalCase`, en inglés. Sufijos por rol:
  - Entidades / modelos de dominio: sin sufijo (`Campaign`, `Notification`).
  - Value objects: sustantivo descriptivo (`ChannelType`, `RecipientAddress`).
  - Casos de uso: `<Verbo><Sustantivo>UseCase` (`CreateCampaignUseCase`).
  - Puertos: `PascalCase` del sustantivo (`NotificationChannel`, `CampaignRepository`).
  - Adaptadores: `<Tecnología|Proveedor><Puerto>` (`EmailNotificationChannel`, `JpaCampaignRepository`).
  - Controllers REST: `<Sustantivo>Controller` (`CampaignController`).
  - DTOs: `<Sustantivo>Request` / `<Sustantivo>Response`, siempre en `adapters/in/rest/dto/`.
  - Config Spring: `<Contexto>Config`.

### Campos y métodos

- `camelCase` en inglés. Métodos de caso de uso en forma de verbo (`createCampaign`, `dispatchPending`).
- No prefijar con `get`/`set` salvo beans Java/Spring obligatorios.

### Paquetes de prueba

- Espejo exacto de `src/main/java`. Sufijos: `*Test` (unitario), `*IT` (integración), `*RestTest` (capa REST).

### Recursos y perfiles

- Archivos de config: `application.yml`, `application-<env>.yml` (`dev`, `test`, `prod`).
- Migraciones / seeds: nombre numérico ordenado alfabéticamente.

## Reglas de dependencia (resumen)

1. `domain/` nunca importa Spring, JPA ni adapters.
2. `application/` nunca importa `adapters/` ni `infrastructure/`.
3. Un slice no importa internos de otro slice.
4. `common/` no conoce features.
5. La clase `@SpringBootApplication` es la única de arranque; no poner `@Component`/`@Service` fuera de `application/`, `adapters/` o `infrastructure/` (los beans viven en la capa exterior, no en el dominio).
