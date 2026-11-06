# Especificación funcional — ambar-notifications-manager

Documentación funcional del microservicio de campañas de notificaciones: **épicas e historias de usuario tecnóagnósticas** (funcionalidad, instrumentación y métricas), derivadas de los slices definidos en [`architecture-slices.md`](../architecture-slices.md) según las reglas de [`architecture-scaffolding.md`](../architecture-scaffolding.md).

**Principios de esta especificación**

- **Tecnóagnósticas:** no nombran tecnologías ni proveedores concretos; usan términos genéricos ("cola de mensajes", "proveedor de canal", "adaptador").
- **Derivadas de los slices:** cada historia se ancla a una responsabilidad o componente clave de `architecture-slices.md` (evidencia incluida en cada documento).
- **Tipos de historia:** Funcional | Instrumentación | Métricas.
- **Independientes de clientes:** no describen necesidades de ninguna aplicación cliente.

## Épicas

| Épica | Slice | Historias | Documento |
|---|---|---|---|
| Template Management | `templates` | US-TPL-01…08 | [epic-template-management.md](epic-template-management.md) |
| Campaign Lifecycle | `campaigns` | US-CMP-01…08 | [epic-campaign-lifecycle.md](epic-campaign-lifecycle.md) |
| Dispatch & Execution | `dispatch` | US-DSP-01…07 | [epic-dispatch-execution.md](epic-dispatch-execution.md) |
| Provider Gateway & Routing | `providers` | US-PRV-01…08 | [epic-provider-gateway.md](epic-provider-gateway.md) |
| Tracking & Observability | `tracking` | US-TRK-01…07 | [epic-tracking-observability.md](epic-tracking-observability.md) |
| Instrumentación y Métricas del Sistema | transversal (`common` + flujo completo) | US-OBS-01…08 | [epic-system-observability.md](epic-system-observability.md) |

**Total:** 6 épicas, 46 historias.

## Flujo de referencia

`campaigns` decide qué enviar → `dispatch` lo ejecuta → `providers` lo entrega → `tracking` observa el resultado. `templates` soporta a todos.

## Roles usados

Gestor de Contenidos, Administrador de Campañas, Operador de campañas, Operador de plataforma, Operador del servicio, Analista de entregabilidad, Analista de marketing, Responsable de entregabilidad, Analista de soporte al cliente, Responsable del servicio de notificaciones. Actores de sistema: Sistema de Despacho, sistema consumidor de resultados.

## Trazabilidad

Cada documento incluye la evidencia de derivación de cada historia (responsabilidad o componente de `architecture-slices.md`). La épica transversal justifica sus historias por el flujo completo entre slices.
