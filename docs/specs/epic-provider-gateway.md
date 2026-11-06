# EPIC: Provider Gateway & Routing

> Épica derivada del slice `providers` de [`docs/architecture-slices.md`](../architecture-slices.md).
> Historias tecnóagnósticas: no nombran tecnologías concretas (colas, proveedores y adaptadores se referencian de forma genérica).

**Descripción**

Aísla la integración técnica con proveedores externos de canales (email, notificaciones push, mensajería instantánea) detrás de un puerto común: enruta cada mensaje al adaptador del canal que le corresponde, traduce las respuestas heterogéneas de los proveedores a un formato de estado agnóstico y ofrece failover automático entre proveedores de un mismo canal.

**Objetivo de negocio**

Entregar cada notificación al proveedor externo adecuado de forma resiliente y desacoplada: que un cambio de proveedor, una caída puntual o la adición de un nuevo canal no obliguen a reescribir la lógica de envío, y que el estado final de cada envío sea interpretable de forma única por todo el sistema.

**Componentes clave que anclan las historias:** `EmailChannelAdapter`, `PushChannelAdapter`, `WhatsAppChannelAdapter` y el puerto común de canal (más las responsabilidades de enrutamiento por canal, abstracción de proveedores, failover y mapeo de estados agnósticos).

## Historias de la épica

| ID | Título | Tipo |
|---|---|---|
| US-PRV-01 | Enrutamiento por canal | Funcional |
| US-PRV-02 | Envío de email tras el puerto común | Funcional |
| US-PRV-03 | Envío de notificaciones push tras el puerto común | Funcional |
| US-PRV-04 | Envío de mensajes de mensajería instantánea tras el puerto común | Funcional |
| US-PRV-05 | Failover entre proveedores del mismo canal | Funcional |
| US-PRV-06 | Mapeo de respuestas a estados agnósticos | Funcional |
| US-PRV-07 | Trazabilidad de invocaciones a proveedores | Instrumentación |
| US-PRV-08 | Métricas del gateway de proveedores | Métricas |

---

### US-PRV-01 — Enrutamiento por canal

**Tipo:** Funcional

**Historia:** "Como Operador de campañas, quiero que cada notificación se despache automáticamente al adaptador del canal que le corresponde (email, push, mensajería instantánea), para que un único punto de entrada soporte todos los canales sin lógica de envío duplicada."

**Criterios de aceptación:**

1. Dada una notificación con un canal asignado (conjunto cerrado de tipos de canal del dominio), cuando se solicita el envío, entonces el sistema invoca el adaptador de ese canal.
2. Si el canal indicado no está soportado, el sistema rechaza el envío con un error claro y no intenta un envío por otro canal.
3. El enrutamiento decide solo por el tipo de canal de la notificación; la misma notificación siempre se dirige al mismo canal declarado.
4. Añadir soporte para un canal nuevo no requiere modificar la lógica de enrutamiento central (se resuelve con un nuevo adaptador de canal).

**Evidencia de derivación:** Responsabilidad "Implementar el patrón Strategy o Router para despachar el mensaje según el canal" (`docs/architecture-slices.md`, slice `providers`).

---

### US-PRV-02 — Envío de email tras el puerto común

**Tipo:** Funcional

**Historia:** "Como Operador de campañas, quiero que las notificaciones por email se envíen a través de un proveedor de email externo detrás de la interfaz común de canal, para que las campañas lleguen al correo del destinatario sin acoplar el sistema a un proveedor concreto."

**Criterios de aceptación:**

1. Dada una notificación por email (destinatario, asunto y contenido), cuando se invoca el adaptador de email, entonces el mensaje se envía al proveedor de email configurado.
2. La respuesta del proveedor se traduce a un resultado de envío del sistema (estado agnóstico), no al formato crudo del proveedor.
3. Si la llamada al proveedor falla, el adaptador reporta un fallo de envío con motivo, sin filtrar detalles internos del proveedor al resto del sistema.
4. La configuración del proveedor (credenciales, remitente) es intercambiable sin modificar la lógica de orquestación de envío.

**Evidencia de derivación:** Componente `EmailChannelAdapter` y responsabilidad "Abstraer las librerías/SDKs de proveedores externos mediante un puerto común" (`docs/architecture-slices.md`, slice `providers`).

---

### US-PRV-03 — Envío de notificaciones push tras el puerto común

**Tipo:** Funcional

**Historia:** "Como Operador de campañas, quiero que las notificaciones push se envíen a través de un proveedor de notificaciones push externo detrás de la interfaz común de canal, para llegar a los dispositivos de los destinatarios sin acoplar el sistema a un proveedor concreto."

**Criterios de aceptación:**

1. Dada una notificación push (dispositivo/registro del destinatario, título y contenido), cuando se invoca el adaptador push, entonces el mensaje se envía al proveedor de push configurado.
2. La respuesta del proveedor se traduce a un resultado de envío del sistema (estado agnóstico).
3. Un token/dispositivo inválido o no registrado se reporta como fallo de envío identificable (no como error genérico) y no genera reintentos.
4. La configuración del proveedor es intercambiable sin modificar la lógica de orquestación de envío.

**Evidencia de derivación:** Componente `PushChannelAdapter` y responsabilidad de abstracción de proveedores vía puerto común (`docs/architecture-slices.md`, slice `providers`).

---

### US-PRV-04 — Envío de mensajes de mensajería instantánea tras el puerto común

**Tipo:** Funcional

**Historia:** "Como Operador de campañas, quiero que los mensajes de mensajería instantánea (p. ej. WhatsApp) se envíen a través de un proveedor de mensajería externo detrás de la interfaz común de canal, para que las campañas lleguen al chat del destinatario sin acoplar el sistema a un proveedor concreto."

**Criterios de aceptación:**

1. Dada una notificación de mensajería instantánea (destinatario y contenido del mensaje), cuando se invoca el adaptador del canal, entonces el mensaje se envía al proveedor de mensajería configurado.
2. La respuesta del proveedor se traduce a un resultado de envío del sistema (estado agnóstico).
3. Un número/cuenta de destinatario inválido o fuera de la ventana de contacto del proveedor se reporta como fallo identificable y no como error genérico.
4. La configuración del proveedor es intercambiable sin modificar la lógica de orquestación de envío.

**Evidencia de derivación:** Componente `WhatsAppChannelAdapter` y responsabilidad de abstracción de proveedores vía puerto común (`docs/architecture-slices.md`, slice `providers`).

---

### US-PRV-05 — Failover entre proveedores del mismo canal

**Tipo:** Funcional

**Historia:** "Como Operador de plataforma, quiero que, si el proveedor primario de un canal falla o está disponible con degradación, el sistema envíe automáticamente por un proveedor secundario configurado, para mantener la entrega de las campañas durante caídas puntuales del proveedor."

**Criterios de aceptación:**

1. Dado un canal con proveedor primario y secundario configurados, cuando el proveedor primario falla (error transitorio o indisponibilidad), entonces el sistema intenta el envío por el proveedor secundario automáticamente.
2. El failover no requiere intervención manual ni modificación de la orden de envío original.
3. Si ambos proveedores fallan, el envío se reporta como fallo para que el despacho aplique su política de reintentos/DLQ.
4. El resultado del envío refleja el proveedor que realmente procesó el mensaje (trazabilidad del proveedor usado).
5. La configuración de primario/secundario es por canal y configurable por ambiente.

**Evidencia de derivación:** Responsabilidad "Manejo de failover entre proveedores si un canal primario falla" (`docs/architecture-slices.md`, slice `providers`).

---

### US-PRV-06 — Mapeo de respuestas a estados agnósticos

**Tipo:** Funcional

**Historia:** "Como Operador de plataforma, quiero que todas las respuestas de los proveedores se traduzcan a un conjunto cerrado y agnóstico de estados de envío (p. ej. DELIVERED, FAILED, BOUNCED), para que todo el sistema interprete el resultado de un envío de forma única, sea cual sea el proveedor."

**Criterios de aceptación:**

1. Dada una respuesta de cualquier proveedor, cuando se procesa, entonces se traduce a uno de los estados agnósticos definidos por el sistema (conjunto cerrado).
2. Situaciones equivalentes en proveedores distintos (p. ej. buzones inexistentes) producen el mismo estado agnóstico (p. ej. BOUNCED).
3. Una respuesta no reconocida o un error inesperado del proveedor se traduce a un estado de fallo del sistema, nunca se expone el formato crudo del proveedor.
4. El estado agnóstico acompaña al resultado del envío hacia el despacho y la observabilidad del sistema.

**Evidencia de derivación:** Responsabilidad "Mapeo de respuestas de proveedor a un formato de estado agnóstico (`DELIVERED`, `FAILED`, `BOUNCED`)" (`docs/architecture-slices.md`, slice `providers`).

---

### US-PRV-07 — Trazabilidad de invocaciones a proveedores

**Tipo:** Instrumentación

**Historia:** "Como Operador de plataforma, quiero que cada invocación a un proveedor deje un registro estructurado (canal, proveedor, duración, resultado), para diagnosticar incidencias de entrega sin exponer credenciales ni datos personales."

**Criterios de aceptación:**

1. Cada invocación a un proveedor genera un registro estructurado con: canal, proveedor invocado, duración de la llamada y resultado (éxito/fallo y estado agnóstico obtenido).
2. Los registros enlazan con el identificador de correlación de la notificación correspondiente.
3. Los registros nunca contienen credenciales ni datos personales en claro del destinatario.
4. Un fallo de invocación queda registrado con un motivo resumido y clasificado (transitorio vs. permanente).

**Evidencia de derivación:** Derivada de la responsabilidad de integración con proveedores externos del slice `providers` (`docs/architecture-slices.md`), como capa de observabilidad del propio gateway.

---

### US-PRV-08 — Métricas del gateway de proveedores

**Tipo:** Métricas

**Historia:** "Como Operador de plataforma, quiero métricas del gateway de proveedores (latencia de envío, tasa de éxito/fallo por proveedor, activaciones de failover y errores por tipo), para vigilar la calidad de cada integración y detectar proveedores degradados antes de que afecten las campañas."

**Criterios de aceptación:**

1. Se expone la latencia de envío por proveedor y canal (distribución o percentiles de la llamada al proveedor).
2. Se expone la tasa de éxito y fallo de envío por proveedor, desagregada por estado agnóstico (p. ej. DELIVERED, FAILED, BOUNCED).
3. Se cuenta el número de activaciones de failover por canal y el proveedor de origen del failover.
4. Los errores se agrupan por tipo (transitorio, permanente, cuota/limitación) para distinguir causas.
5. Las métricas están disponibles para monitoreo y alertas sin afectar el rendimiento del envío.

**Evidencia de derivación:** Instrumentación del slice `providers` sobre sus responsabilidades de failover y mapeo de estados (`docs/architecture-slices.md`), según el alcance de métricas de este workstream.
