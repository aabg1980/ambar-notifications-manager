# Slices de arquitectura

Los cinco slices del dominio. Cada uno es un directorio bajo `features/` con el layout de [`architecture-scaffolding.md`](architecture-scaffolding.md). La estructura de directorios y el estándar de nombres viven ahí; aquí se define **qué** aísla cada slice.

Flujo principal: `campaigns` decide qué enviar → `dispatch` lo ejecuta → `providers` lo entrega → `tracking` observa el resultado. `templates` soporta a todos.

---

## 1. `templates` — Template Management Slice

Aísla la definición de las plantillas y el procesamiento de sus variables dinámicas.

- **Bounded Context / Subdominio:** Soporte / Contenido

**Responsabilidades**
- CRUD de plantillas con soporte para múltiples canales (HTML/Email, Push, SMS, WhatsApp).
- Versionado de plantillas.
- Renderizado e interpolación de variables (motor de plantillas: Handlebars, Thymeleaf o Mustache).
- Definición de placeholders requeridos por versión.

**Componentes clave**
- `TemplateRepository`
- `TemplateRenderService`

---

## 2. `campaigns` — Campaign Lifecycle Slice

Aísla la lógica de negocio de lo que significa "crear", "programar" o "lanzar" una campaña masiva o dirigida.

- **Bounded Context / Subdominio:** Core Domain (Gestión de Campañas)

**Responsabilidades**
- Definición del público objetivo / audiencia (segments o lista de destinatarios).
- Reglas de negocio de la campaña (fecha de inicio, frecuencia, canal asignado, plantilla asociada).
- Gestión de estados de la campaña: `Draft`, `Scheduled`, `Executing`, `Completed`, `Paused`, `Cancelled`.
- Desglose de la campaña en órdenes individuales o de lote (batching).

**Componentes clave**
- `CampaignAggregate` (entidad raíz)
- `CampaignScheduler`
- `AudienceResolverPort`

---

## 3. `dispatch` — Notification Dispatch & Execution Slice

Aísla el consumo masivo, el control de flujo (rate limiting) y la resiliencia en la entrega individual de mensajes. Es la rebanada de alto tráfico / performance.

- **Bounded Context / Subdominio:** Core Delivery

**Responsabilidades**
- Consumo de eventos o colas de mensajes (Kafka / RabbitMQ / SQS).
- Orquestación de envío individual: vincular datos + plantilla renderizada + canal.
- Estrategias de reintento con Exponential Backoff.
- Manejo de Dead Letter Queues (DLQ).
- Control de concurrencia y límites de tasa (throttling por proveedor).

**Componentes clave**
- `NotificationMessageConsumer`
- `DeliveryRetryPolicy`

---

## 4. `providers` — Provider Gateway & Routing Slice (Adaptadores de Canales)

Aísla la integración técnica con proveedores externos (SendGrid, AWS SES, Twilio, Firebase Cloud Messaging, Meta API).

- **Bounded Context / Subdominio:** Infraestructura / Adaptadores

**Responsabilidades**
- Implementar el patrón Strategy o Router para despachar el mensaje según el canal.
- Abstraer las librerías/SDKs de proveedores externos mediante un puerto común (`NotificationChannelPort`).
- Manejo de failover entre proveedores si un canal primario falla.
- Mapeo de respuestas de proveedor a un formato de estado agnóstico (`DELIVERED`, `FAILED`, `BOUNCED`).

**Componentes clave**
- `EmailChannelAdapter` (SendGrid / SES)
- `PushChannelAdapter` (FCM / APNs)
- `WhatsAppChannelAdapter` (Twilio / Meta)

---

## 5. `tracking` — Tracking & Observability Slice (Métricas y Telemetría)

Aísla la recolección de eventos de entrega y métricas de consumo sin impactar el rendimiento de la entrega principal.

- **Bounded Context / Subdominio:** Soporte / Analítica

**Responsabilidades**
- Recepción de webhooks de proveedores externos (aperturas de email, rebotes, clics, entregas confirmadas).
- Persistencia de logs de auditoría por notificación.
- Publicación de eventos de dominio (`NotificationSent`, `NotificationFailed`, `EmailOpened`).

**Componentes clave**
- `WebhookIngestionController`
- `NotificationAuditRepository`
