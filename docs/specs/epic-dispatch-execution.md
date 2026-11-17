# EPIC: Dispatch & Execution

> Épica derivada del slice `dispatch` de [`docs/architecture-slices.md`](../architecture-slices.md).
> Historias tecnóagnósticas: no nombran tecnologías concretas (colas, proveedores y adaptadores se referencian de forma genérica).

**Descripción**

Aísla el consumo masivo de órdenes de envío, el control de flujo (throttling) y la resiliencia en la entrega individual de mensajes. Es el slice de alto tráfico: recibe lo que `campaigns` decide enviar y lo ejecuta notificación por notificación, vinculando datos del destinatario, contenido renderizado y canal, con reintentos y manejo de mensajes fallidos.

**Objetivo de negocio**

Ejecutar cada notificación individual de forma masiva, confiable y controlada: sin pérdida de mensajes, sin duplicados innecesarios y sin saturar a los proveedores externos, de modo que las campañas se entreguen a tiempo y el equipo de operaciones tenga visibilidad de cada fallo.

**Componentes clave que anclan las historias:** `NotificationMessageConsumer`, `DeliveryRetryPolicy` (más las responsabilidades de consumo de colas, orquestación individual, reintentos con backoff exponencial, DLQ y throttling por proveedor).

## Historias de la épica

| ID | Título | Tipo |
|---|---|---|
| US-DSP-01 | Consumo automático de órdenes de envío | Funcional |
| US-DSP-02 | Orquestación del envío individual | Funcional |
| US-DSP-03 | Reintentos con backoff exponencial | Funcional |
| US-DSP-04 | Manejo de mensajes fallidos definitivos (DLQ) | Funcional |
| US-DSP-05 | Throttling y control de concurrencia por proveedor | Funcional |
| US-DSP-06 | Trazabilidad por intento de envío | Instrumentación |
| US-DSP-07 | Métricas de ejecución del despacho | Métricas |

---

### US-DSP-01 — Consumo automático de órdenes de envío

**Tipo:** Funcional

**Historia:** "Como Operador de campañas, quiero que el sistema consuma automáticamente las órdenes de envío publicadas en la cola de mensajes, para que cada notificación programada se ejecute sin intervención manual y a escala masiva."

**Criterios de aceptación:**

1. Dada una orden de envío disponible en la cola, cuando el sistema la consume, entonces la retira de la cola y la procesa exactamente una vez en esa ejecución (sin reprocesar la misma orden consumida).
2. Si la cola está vacía, el sistema permanece a la espera sin generar errores ni consumir recursos de forma innecesaria.
3. Si una orden llega con datos incompletos (falta destinatario, canal o plantilla asociada), el sistema la rechaza con un motivo claro y no intenta enviarla.
4. Un mensaje individual problemático no detiene el procesamiento del resto de la cola: el consumo continúa con las órdenes siguientes.

**Evidencia de derivación:** Responsabilidad "Consumo de eventos o colas de mensajes" y componente `NotificationMessageConsumer` (`docs/architecture-slices.md`, slice `dispatch`).

---

### US-DSP-02 — Orquestación del envío individual

**Tipo:** Funcional

**Historia:** "Como Operador de campañas, quiero que, para cada destinatario, el sistema vincule sus datos, el contenido renderizado de la plantilla y el canal asignado en una única notificación individual, para que el envío sea completo, personalizado y consistente."

**Criterios de aceptación:**

1. Dados los datos de un destinatario, una plantilla con su contenido renderizado y un canal asignado, cuando el sistema orquesta el envío, entonces produce una notificación individual que integra los tres elementos.
2. El contenido enviado refleja los datos del destinatario correspondiente (personalización por destinatario, sin mezclar datos entre destinatarios de la misma campaña).
3. La notificación se entrega al canal indicado en la orden para su envío al proveedor externo.
4. Si el contenido renderizado no está disponible o el canal no es válido, el envío se marca como fallido con motivo y no se envía un mensaje incompleto.

**Evidencia de derivación:** Responsabilidad "Orquestación de envío individual: vincular datos + plantilla renderizada + canal" (`docs/architecture-slices.md`, slice `dispatch`).

---

### US-DSP-03 — Reintentos con backoff exponencial

**Tipo:** Funcional

**Historia:** "Como Operador de campañas, quiero que los envíos que fallen por errores transitorios se reintenten automáticamente con esperas crecientes (backoff exponencial), para maximizar la tasa de entrega sin saturar al proveedor."

**Criterios de aceptación:**

1. Dado un envío que falla por un error transitorio (p. ej. indisponibilidad temporal del proveedor), cuando corresponde un reintento, entonces el sistema vuelve a intentar el envío según una política configurable (número máximo de intentos y espera base).
2. La espera entre intentos crece exponencialmente respecto al intento anterior (p. ej. espera base, 2×, 4×…).
3. Un error permanente (p. ej. destinatario inválido) no genera reintentos: se trata como fallo definitivo desde el primer intento.
4. Agotado el número máximo de intentos, el mensaje se deriva al manejo de mensajes fallidos definitivos (DLQ) con el motivo del último fallo.

**Evidencia de derivación:** Responsabilidad "Estrategias de reintento con Exponential Backoff" y componente `DeliveryRetryPolicy` (`docs/architecture-slices.md`, slice `dispatch`).

---

### US-DSP-04 — Manejo de mensajes fallidos definitivos (DLQ)

**Tipo:** Funcional

**Historia:** "Como Operador de plataforma, quiero que los mensajes que agotan sus reintentos o son irrecuperables se deriven a una cola de mensajes fallidos (DLQ), para que ningún mensaje se pierda en silencio y pueda ser diagnosticado y reprocesado."

**Criterios de aceptación:**

1. Dado un mensaje que agota sus reintentos o falla con un error permanente, cuando se declara fallido definitivo, entonces se deriva a la cola de mensajes fallidos (DLQ).
2. Cada mensaje en la DLQ conserva los datos originales del envío y el motivo del fallo, suficientes para diagnosticar y reprocesar.
3. La derivación a DLQ no bloquea ni ralentiza el procesamiento de la cola principal.
4. Un mensaje en la DLQ puede ser reprocesado/reinyectado manualmente por el equipo de operaciones una vez corregida la causa.

**Evidencia de derivación:** Responsabilidad "Manejo de Dead Letter Queues (DLQ)" (`docs/architecture-slices.md`, slice `dispatch`).

---

### US-DSP-05 — Throttling y control de concurrencia por proveedor

**Tipo:** Funcional

**Historia:** "Como Operador de plataforma, quiero que el sistema respete límites de tasa y de envíos simultáneos por proveedor, para no saturar los servicios externos, cumplir sus cuotas y degradar con orden en lugar de fallar en masa."

**Criterios de aceptación:**

1. Dado un límite configurado por proveedor (máximo de envíos por ventana de tiempo y máximo de envíos simultáneos), cuando se alcanza el límite, entonces el sistema enlentece o encola los envíos pendientes en lugar de rechazarlos.
2. La saturación del límite de un proveedor no afecta el flujo de envíos hacia otros proveedores o canales.
3. Los límites son configurables por proveedor/canal y por ambiente, sin modificar el código del sistema.
4. Los envíos retrasados por throttling se recuperan automáticamente cuando hay capacidad disponible, en el orden de llegada.

**Evidencia de derivación:** Responsabilidad "Control de concurrencia y límites de tasa (throttling por proveedor)" (`docs/architecture-slices.md`, slice `dispatch`).

---

### US-DSP-06 — Trazabilidad por intento de envío

**Tipo:** Instrumentación

**Historia:** "Como Operador de plataforma, quiero que cada notificación tenga un identificador de correlación y que cada intento de envío genere un registro estructurado (intento, resultado, duración, motivo de fallo), para poder reconstruir la historia completa de cualquier envío sin exponer datos sensibles."

**Criterios de aceptación:**

1. Cada notificación individual porta un identificador de correlación único que acompaña a la orden desde el consumo hasta el resultado final.
2. Cada intento de envío genera un registro estructurado que incluye: número de intento, proveedor/canal, resultado, duración y motivo resumido del fallo si lo hubo.
3. Los registros no contienen credenciales ni datos personales en claro del destinatario (p. ej. direcciones o teléfonos enmascarados).
4. La generación de trazabilidad no degrada de forma perceptible el rendimiento del procesamiento de la cola.

**Evidencia de derivación:** Derivada de las responsabilidades de orquestación y reintentos del slice `dispatch` (`docs/architecture-slices.md`), como capa de observabilidad del propio despacho (no confundir con la auditoría de `tracking`).

---

### US-DSP-07 — Métricas de ejecución del despacho

**Tipo:** Métricas

**Historia:** "Como Operador de plataforma, quiero métricas de la ejecución del despacho (volumen procesado, tasa de éxito/fallo, reintentos, uso de DLQ, saturación de throttling y latencia por mensaje), para vigilar la salud del envío masivo y actuar antes de que las campañas se vean afectadas."

**Criterios de aceptación:**

1. El sistema expone métricas de: mensajes procesados, tasa de éxito y fallo de envío, cantidad y tasa de reintentos, mensajes derivados a DLQ y mensajes retrasados por throttling.
2. Se expone la latencia de procesamiento por mensaje (desde consumo hasta resultado del intento).
3. Las métricas están etiquetadas por canal y proveedor para poder desagregar por cada uno.
4. Las métricas se publican/actualizan sin bloquear el flujo principal de despacho y están disponibles para consulta de monitoreo.

**Evidencia de derivación:** Instrumentación del slice `dispatch` sobre sus responsabilidades de reintentos, DLQ y throttling (`docs/architecture-slices.md`), según el alcance de métricas de este workstream.
