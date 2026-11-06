# EPIC: Campaign Lifecycle (Ciclo de Vida de Campañas)

> Épica derivada del slice `campaigns` de [`docs/architecture-slices.md`](../architecture-slices.md).
> Historias tecnóagnósticas: no nombran tecnologías concretas.

**Descripción**

Épica que agrupa la lógica de negocio de creación, programación, ejecución y cierre de campañas de notificación masivas o dirigidas: audiencia, reglas de negocio, máquina de estados y desglose en órdenes de envío.

**Objetivo de negocio**

Permitir que el equipo configure y controle campañas de notificación de extremo a extremo —desde el borrador hasta su finalización— con reglas de negocio explícitas, transiciones de estado controladas y visibilidad del avance, garantizando que solo se envíe lo aprobado, a la audiencia correcta y en el momento correcto.

**Roles:** **Administrador de Campañas** (define, programa y controla campañas), **Operador del servicio** (salud operativa).

## Historias de la épica

| ID | Título | Tipo |
|---|---|---|
| US-CMP-01 | Crear campaña en borrador | Funcional |
| US-CMP-02 | Definir la audiencia de la campaña | Funcional |
| US-CMP-03 | Programar la campaña | Funcional |
| US-CMP-04 | Pausar y reanudar una campaña | Funcional |
| US-CMP-05 | Cancelar una campaña | Funcional |
| US-CMP-06 | Desglose de la campaña en órdenes de envío | Funcional |
| US-CMP-07 | Trazabilidad del ciclo de vida de la campaña | Instrumentación |
| US-CMP-08 | Métricas del ciclo de vida y alertas de campañas estancadas | Métricas |

---

### US-CMP-01 — Crear campaña en borrador

**Tipo:** Funcional

**Historia:** "Como Administrador de Campañas, quiero crear una campaña en estado borrador definiendo su nombre, canal, plantilla asociada, fecha de inicio prevista y frecuencia, para configurar una iniciativa de notificación sin que se envíe nada todavía."

**Criterios de aceptación:**

1. La campaña se crea en estado `Draft` y no genera ningún envío mientras esté en ese estado.
2. La creación exige nombre, canal (del conjunto cerrado de canales soportados), plantilla asociada y fecha de inicio prevista.
3. La plantilla asociada debe estar vigente y soportar el canal elegido; en caso contrario la creación se rechaza indicando el conflicto.
4. Se puede definir la frecuencia de envío: única o recurrente con una cadencia (p. ej. diaria, semanal, mensual).
5. Mientras la campaña esté en `Draft`, todas sus reglas de negocio son editables.

**Evidencia de derivación:** Responsabilidad "Reglas de negocio de la campaña (fecha de inicio, frecuencia, canal asignado, plantilla asociada)" y componente clave `CampaignAggregate`.

---

### US-CMP-02 — Definir la audiencia de la campaña

**Tipo:** Funcional

**Historia:** "Como Administrador de Campañas, quiero definir el público objetivo de la campaña indicando un segmento de destinatarios o una lista explícita, para que solo reciban notificaciones las personas adecuadas."

**Criterios de aceptación:**

1. La audiencia se define por un segmento predefinido o por una lista explícita de destinatarios, y una campaña no puede combinar ambos modos a la vez.
2. La resolución de la audiencia en la lista concreta de destinatarios se realiza al inicio de la ejecución, de modo que refleje los cambios del segmento hasta ese momento.
3. Una campaña sin audiencia definida no puede pasar a estado `Scheduled`.
4. Antes de programar, se puede obtener una estimación del número de destinatarios que resultaría de resolver la audiencia.

**Evidencia de derivación:** Responsabilidad "Definición del público objetivo / audiencia (segments o lista de destinatarios)" y componente clave `AudienceResolverPort`.

---

### US-CMP-03 — Programar la campaña

**Tipo:** Funcional

**Historia:** "Como Administrador de Campañas, quiero programar la campaña para que pase de borrador a programada y su envío se ejecute automáticamente en la fecha indicada, para no depender de una acción manual en el momento del lanzamiento."

**Criterios de aceptación:**

1. Solo una campaña en `Draft` puede pasar a `Scheduled`; la transición exige audiencia, plantilla, canal y una fecha de inicio futura válida.
2. Una vez en `Scheduled`, los cambios en canal, plantilla o audiencia no se permiten sin antes devolver la campaña a `Draft` o cancelarla.
3. Al alcanzar la fecha de inicio, la campaña pasa automáticamente a `Executing` sin intervención manual.
4. Una campaña recurrente programada registra su próxima ejecución según la cadencia definida.

**Evidencia de derivación:** Responsabilidad "Gestión de estados de la campaña: Draft, Scheduled, Executing" y componente clave `CampaignScheduler`.

---

### US-CMP-04 — Pausar y reanudar una campaña

**Tipo:** Funcional

**Historia:** "Como Administrador de Campañas, quiero pausar y reanudar una campaña en ejecución, para detener temporalmente los envíos ante un incidente sin perder el avance conseguido."

**Criterios de aceptación:**

1. Una campaña en `Executing` puede pasar a `Paused` por acción del administrador; también puede reanudarse una campaña recurrente en pausa entre ejecuciones.
2. Mientras la campaña está en `Paused` no se emiten nuevas órdenes de envío.
3. Al reanudar, la campaña vuelve a `Executing` y retoma desde el punto donde se quedó, sin generar envíos duplicados de lo ya emitido.
4. Cada pausa y reanudación queda registrada con fecha, duración de la pausa y responsable.

**Evidencia de derivación:** Responsabilidad "Gestión de estados de la campaña: Executing, Paused" (transiciones sobre `CampaignAggregate`).

---

### US-CMP-05 — Cancelar una campaña

**Tipo:** Funcional

**Historia:** "Como Administrador de Campañas, quiero cancelar una campaña programada, en ejecución o pausada, para detener definitivamente una iniciativa que ya no aplica."

**Criterios de aceptación:**

1. Una campaña en `Scheduled`, `Executing` o `Paused` puede pasar a `Cancelled`; una campaña en `Draft` también puede cancelarse.
2. La cancelación es definitiva: la campaña no puede reanudarse ni volver a programarse; solo puede clonarse como campaña nueva si se requiere relanzar.
3. La cancelación no revoca los envíos ya emitidos, pero garantiza que no se generen nuevas órdenes de envío.
4. Toda cancelación registra un motivo y el responsable de la decisión.

**Evidencia de derivación:** Responsabilidad "Gestión de estados de la campaña: Cancelled" (transición sobre `CampaignAggregate`).

---

### US-CMP-06 — Desglose de la campaña en órdenes de envío

**Tipo:** Funcional

**Historia:** "Como Administrador de Campañas, quiero que la campaña se desglose en órdenes de envío individuales o por lotes según el tamaño de la audiencia, para poder seguir y controlar su ejecución por partes."

**Criterios de aceptación:**

1. Al iniciar la ejecución, la audiencia resuelta se desglose en órdenes de envío identificables, agrupadas individualmente o en lotes según el tamaño de la audiencia.
2. El tamaño de lote es configurable; una campaña de audiencia reducida puede desglosarse en órdenes individuales.
3. El avance de la campaña se expresa como órdenes totales, completadas y pendientes.
4. Una campaña recurrente genera un desglose independiente por cada ejecución programada.

**Evidencia de derivación:** Responsabilidad "Desglose de la campaña en órdenes individuales o de lote (batching)".

---

### US-CMP-07 — Trazabilidad del ciclo de vida de la campaña

**Tipo:** Instrumentación

**Historia:** "Como Operador del servicio, quiero que cada transición de estado de una campaña quede registrada con marca de tiempo, responsable y estado anterior y posterior, para reconstruir la historia completa de cualquier campaña."

**Criterios de aceptación:**

1. Toda transición de estado genera un registro con: estado origen, estado destino, fecha, y responsable o disparador (manual o automático).
2. Los intentos de transición inválidos (p. ej. reanudar una campaña cancelada) también quedan registrados como rechazos con su motivo.
3. La trazabilidad permite reconstruir la línea de tiempo completa de una campaña, incluyendo las pausas y su duración.
4. Los registros son consultables por campaña y por rango de fechas.

**Evidencia de derivación:** Responsabilidad "Gestión de estados de la campaña: Draft, Scheduled, Executing, Completed, Paused, Cancelled" (trazabilidad de las transiciones de `CampaignAggregate`).

---

### US-CMP-08 — Métricas del ciclo de vida y alertas de campañas estancadas

**Tipo:** Métricas

**Historia:** "Como Operador del servicio, quiero métricas del ciclo de vida de las campañas y alertas sobre campañas estancadas, para actuar a tiempo sobre las que no avanzan y entender los tiempos reales de mi operación."

**Criterios de aceptación:**

1. Se expone el número de campañas por estado y la duración de cada estado, además del tiempo total desde la creación hasta la finalización.
2. Se detecta como campaña estancada a una campaña en `Executing` cuyo avance de órdenes no progresa durante un umbral configurable.
3. La condición de estancamiento genera una alerta dirigida al operador, identificando la campaña y el tiempo sin avance.
4. Las métricas se actualizan al producirse cada transición de estado y cada avance en el desglose de órdenes.

**Evidencia de derivación:** Componentes clave `CampaignScheduler` y `CampaignAggregate` (medición de sus transiciones y del avance del desglose en órdenes).
