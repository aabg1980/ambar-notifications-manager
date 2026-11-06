# EPIC: Tracking & Observability (Trazabilidad y Observabilidad)

> Épica derivada del slice `tracking` de [`docs/architecture-slices.md`](../architecture-slices.md).
> Historias tecnóagnósticas: no nombran tecnologías concretas (se usan términos genéricos como "proveedor de canal", "cola de mensajes").

**Descripción**

Aísla la recolección de eventos de entrega y métricas de consumo **sin impactar el rendimiento de la entrega principal** (responsabilidad declarada del slice). Esta épica cubre las tres responsabilidades explícitas del slice: recepción de webhooks de proveedores externos (aperturas, clics, rebotes, entregas confirmadas), persistencia de logs de auditoría por notificación, y publicación de eventos de dominio (`NotificationSent`, `NotificationFailed`, `EmailOpened`, entre otros). Sus componentes clave son `WebhookIngestionController` y `NotificationAuditRepository`.

**Objetivo de negocio**

Poder responder con evidencia confiable a la pregunta *"¿qué le pasó realmente a cada notificación que enviamos?"*, alimentar el análisis de entregabilidad por canal y proveer al resto del sistema un flujo de eventos de resultado que habilite la mejora continua de las campañas.

**Roles:** **Analista de entregabilidad**, **Analista de marketing**, **Responsable de entregabilidad**, **Analista de soporte al cliente**, **sistema consumidor de resultados** (actor de sistema).

## Historias de la épica

| ID | Título | Tipo |
|---|---|---|
| US-TRK-01 | Recepción de confirmaciones de entrega | Funcional |
| US-TRK-02 | Registro de aperturas | Funcional |
| US-TRK-03 | Registro de clics en enlaces | Funcional |
| US-TRK-04 | Registro de rebotes y fallos de entrega | Funcional |
| US-TRK-05 | Normalización de reportes multicanal en un modelo común | Funcional |
| US-TRK-06 | Auditoría consultable por notificación | Funcional |
| US-TRK-07 | Publicación de eventos de dominio de resultado | Instrumentación |

---

### US-TRK-01 — Recepción de confirmaciones de entrega

**Tipo:** Funcional

**Historia:** "Como Analista de entregabilidad, quiero que el sistema reciba y registre las confirmaciones de entrega que envían los proveedores de canal, para saber que cada notificación llegó efectivamente a destino."

**Criterios de aceptación:**

1. Dado un reporte de entrega confirmada emitido por un proveedor de canal, cuando el sistema lo recibe, entonces registra la notificación asociada con estado de resultado normalizado de tipo "entregado", incluyendo fecha/hora del evento.
2. Dado un reporte cuya notificación no existe en el sistema, cuando se recibe, entonces se rechaza con un resultado identificable y se deja constancia del intento, sin afectar el procesamiento de reportes válidos.
3. Dado un reporte duplicado (mismo identificador de evento del proveedor), cuando se recibe más de una vez, entonces se procesa una sola vez y las repeticiones se descartan de forma idempotente.
4. Dado un reporte con firma o credencial de origen inválida (cuando el proveedor la soporte), cuando se recibe, entonces se rechaza sin registrar eventos de entrega.
5. Dado un volumen alto de reportes entrantes, cuando se procesan, entonces la recepción no bloquea ni degrada el flujo principal de envío de notificaciones.

**Evidencia de derivación:** Responsabilidad "Recepción de webhooks de proveedores externos (… entregas confirmadas)" + componente `WebhookIngestionController`.

---

### US-TRK-02 — Registro de aperturas

**Tipo:** Funcional

**Historia:** "Como Analista de marketing, quiero que el sistema registre cuándo abre un destinatario una notificación, para medir el engagement real de las campañas."

**Criterios de aceptación:**

1. Dado un reporte de apertura emitido por un proveedor de canal que lo soporta, cuando se recibe, entonces se asocia a la notificación correspondiente con fecha/hora y se marca como evento de tipo "apertura".
2. Dado un destinatario que abre la misma notificación varias veces, cuando se reciben los reportes, entonces se contabiliza la primera apertura de forma separada de las aperturas repetidas.
3. Dado un reporte de apertura para una notificación en un canal que no soporta medición de aperturas, cuando llega por error, entonces se descarta con un resultado identificable y no contamina las métricas de engagement.
4. Dado un reporte de apertura recibido, cuando se procesa, entonces queda asociado al histórico auditable de la notificación (ver US-TRK-06).

**Evidencia de derivación:** Responsabilidad "Recepción de webhooks de proveedores externos (aperturas de email…)" + evento de dominio `EmailOpened` listado en "Publicación de eventos de dominio".

---

### US-TRK-03 — Registro de clics en enlaces

**Tipo:** Funcional

**Historia:** "Como Analista de marketing, quiero que el sistema registre los clics que hace un destinatario sobre los enlaces de una notificación, para identificar qué contenidos generan conversión."

**Criterios de aceptación:**

1. Dado un reporte de clic emitido por un proveedor de canal que lo soporta, cuando se recibe, entonces se registra asociado a la notificación con fecha/hora y el enlace clicado referenciado de forma genérica (identificador del enlace, sin depender del contenido de la plantilla).
2. Dado un destinatario que hace clic en varios enlaces de la misma notificación, cuando se reciben los reportes, entonces cada clic se registra como evento independiente.
3. Dado un reporte de clic con identificador de enlace desconocido o vacío, cuando se recibe, entonces se registra el evento con la indicación de dato incompleto y no se rechaza la totalidad del reporte.
4. Dado un reporte de clic recibido, cuando se procesa, entonces queda asociado al histórico auditable de la notificación (ver US-TRK-06).

**Evidencia de derivación:** Responsabilidad "Recepción de webhooks de proveedores externos (… clics…)".

---

### US-TRK-04 — Registro de rebotes y fallos de entrega

**Tipo:** Funcional

**Historia:** "Como Responsable de entregabilidad, quiero que el sistema registre los rebotes y fallos de entrega reportados por los proveedores, para detectar destinatarios inválidos y problemas de reputación de envío."

**Criterios de aceptación:**

1. Dado un reporte de rebote o fallo emitido por un proveedor de canal, cuando se recibe, entonces se registra con estado normalizado de tipo "fallido" e "indicación de rebote" (distinción genérica entre rebote permanente y temporal, cuando el proveedor la informe).
2. Dado un reporte de fallo con motivo del proveedor, cuando se registra, entonces el motivo se conserva como dato de diagnóstico sin exponer vocabulario propio del proveedor en el modelo de estado del sistema.
3. Dado un reporte de fallo para una notificación que ya fue marcada como fallida, cuando se recibe de nuevo, entonces no duplica el registro de fallo (idempotencia).
4. Dado un fallo registrado, cuando se procesa, entonces se publica el evento de dominio correspondiente para que el resto del sistema pueda reaccionar (ver US-TRK-07).

**Evidencia de derivación:** Responsabilidad "Recepción de webhooks de proveedores externos (… rebotes…)" + evento de dominio `NotificationFailed` listado en "Publicación de eventos de dominio".

---

### US-TRK-05 — Normalización de reportes multicanal en un modelo común

**Tipo:** Funcional

**Historia:** "Como Analista de entregabilidad, quiero que los reportes de todos los proveedores y canales se normalicen en un único modelo de evento de resultado, para poder analizar la entrega de forma homogénea sin conocer las particularidades de cada proveedor."

**Criterios de aceptación:**

1. Dado un reporte entrante de cualquier proveedor de canal soportado, cuando se normaliza, entonces produce un evento con la misma estructura: identificador de notificación, canal, tipo de resultado (entregado / fallido / abierto / clic / rebote), fecha/hora del evento y datos de diagnóstico opcionales.
2. Dado un proveedor de canal nuevo que se integra al sistema, cuando se conecta su canal de reportes, entonces la normalización se extiende sin modificar el modelo común ni el resto de los analizadores de eventos.
3. Dado un reporte con campos desconocidos o adicionales del proveedor, cuando se normaliza, entonces los campos extra se conservan como datos de diagnóstico sin romper la estructura común.
4. Dado un reporte imposible de normalizar (formato irreconocible), cuando se recibe, entonces se descarta con registro del motivo y sin afectar la ingesta de reportes válidos.

**Evidencia de derivación:** Se desprende de la responsabilidad "Recepción de webhooks de proveedores externos": el slice recibe webhooks de múltiples proveedores y canales y su objetivo es recolectar "eventos de entrega y métricas de consumo", lo que exige un modelo de evento homogéneo agnóstico al proveedor (coherente con el estado normalizado `DELIVERED`/`FAILED`/`BOUNCED` que `providers` entrega al resto del sistema).

---

### US-TRK-06 — Auditoría consultable por notificación

**Tipo:** Funcional

**Historia:** "Como Analista de soporte al cliente, quiero consultar el registro de auditoría completo de una notificación individual, para responder a un cliente qué ocurrió exactamente con su mensaje."

**Criterios de aceptación:**

1. Dado un identificador de notificación válido, cuando un Analista de soporte lo consulta, entonces el sistema muestra su línea de tiempo: creación dentro de la campaña, intentos de envío, resultados de entrega reportados y eventos de interacción (aperturas / clics), en orden cronológico.
2. Dado un identificador de notificación inexistente, cuando se consulta, entonces el sistema responde con un resultado claro de "no encontrado", sin información parcial engañosa.
3. Dado un registro de auditoría escrito, cuando se consulta posteriormente, entonces su contenido es inmutable: no puede ser modificado ni eliminado por operaciones de usuario.
4. Dado un volumen alto de registros de auditoría, cuando se consulta una notificación individual, entonces la consulta responde en un tiempo apto para uso interactivo de soporte.
5. Dado el acceso a la auditoría, cuando un Analista de soporte consulta, entonces solo puede ver notificaciones de las campañas o clientes que tiene autorizados (control de acceso).

**Evidencia de derivación:** Responsabilidad "Persistencia de logs de auditoría por notificación" + componente `NotificationAuditRepository`.

---

### US-TRK-07 — Publicación de eventos de dominio de resultado

**Tipo:** Instrumentación

**Historia:** "Como sistema consumidor de resultados (reportes, campañas, integraciones externas), quiero que el slice de tracking publique eventos de dominio cada vez que cambia el estado de una notificación, para que el resto del sistema reaccione y se integre sin acoplarse a los proveedores."

**Criterios de aceptación:**

1. Dado un resultado de entrega registrado (envío, entrega confirmada, fallo, rebote, apertura, clic), cuando se consolida, entonces el sistema publica el evento de dominio correspondiente (p. ej. `NotificationSent`, `NotificationFailed`, `EmailOpened`) en el mecanismo de eventos del sistema.
2. Dado un evento publicado, cuando lo recibe un consumidor, entonces el evento contiene identificador de notificación, campaña asociada, canal, tipo de resultado y fecha/hora, sin datos sensibles del destinatario en texto plano.
3. Dado un fallo en la publicación de un evento, cuando ocurre, entonces no se pierde el registro de auditoría (US-TRK-06) y la publicación se recupera de forma diferida sin bloquear la ingesta de reportes.
4. Dado un consumidor del flujo de eventos, cuando se suscribe, entonces puede distinguir claramente cada tipo de evento de resultado por su tipo, sin interpretar cadenas libres.

**Evidencia de derivación:** Responsabilidad "Publicación de eventos de dominio (`NotificationSent`, `NotificationFailed`, `EmailOpened`)".

---

## Restricción (Constraint) — Desacople de rendimiento del tracking

> "La recolección de eventos de entrega y métricas de consumo no debe impactar el rendimiento de la entrega principal" (objetivo declarado del slice `tracking` en `architecture-slices.md`).

**Implicación para todas las historias de esta épica:** toda la ingesta, persistencia y publicación de telemetría debe operar de forma asíncrona o diferida respecto al flujo de envío; ninguna operación de tracking puede ser un paso bloqueante en el camino crítico de entrega de una notificación. Verificable como criterio no funcional en pruebas de carga del flujo de envío (con tracking activo vs. inactivo, la latencia de envío no degrada de forma significativa).
