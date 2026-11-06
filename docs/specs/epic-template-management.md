# EPIC: Template Management (Gestión de Plantillas)

> Épica derivada del slice `templates` de [`docs/architecture-slices.md`](../architecture-slices.md).
> Historias tecnóagnósticas: no nombran tecnologías concretas (se usan términos genéricos como "motor de plantillas", "canal de notificación").

**Descripción**

Épica que agrupa la definición, versionado y procesamiento de las plantillas de notificación multicanal (email, WhatsApp, SMS, push) y su contenido dinámico. Constituye el soporte de contenido para todas las campañas.

**Objetivo de negocio**

Disponer de un catálogo de contenido reutilizable, versionado y auditable, de modo que las campañas se construyan sobre plantillas consistentes y los mensajes personalizados se generen de forma predecible, con detección temprana de datos faltantes.

**Roles:** **Gestor de Contenidos** (crea y mantiene plantillas), **Operador del servicio** (salud operativa), **Sistema de Despacho** (actor de sistema consumidor, de otros slices).

## Historias de la épica

| ID | Título | Tipo |
|---|---|---|
| US-TPL-01 | Crear plantilla multicanal | Funcional |
| US-TPL-02 | Consultar y buscar plantillas | Funcional |
| US-TPL-03 | Editar plantilla generando una nueva versión | Funcional |
| US-TPL-04 | Archivar plantilla obsoleta | Funcional |
| US-TPL-05 | Definir placeholders requeridos por versión | Funcional |
| US-TPL-06 | Renderizar plantilla con datos del destinatario | Funcional |
| US-TPL-07 | Trazabilidad de operaciones sobre plantillas | Instrumentación |
| US-TPL-08 | Métricas de creación y procesamiento de plantillas | Métricas |

---

### US-TPL-01 — Crear plantilla multicanal

**Tipo:** Funcional

**Historia:** "Como Gestor de Contenidos, quiero crear una plantilla de notificación asociada a un canal de comunicación, para disponer de contenido reutilizable con variables dinámicas en mis campañas."

**Criterios de aceptación:**

1. La plantilla se crea con nombre, canal, contenido y las variables dinámicas que utilice el contenido.
2. El canal se elige del conjunto cerrado de canales soportados (email, WhatsApp, SMS, push); un canal fuera de ese conjunto rechaza la creación.
3. La creación genera automáticamente la versión inicial de la plantilla.
4. Se rechaza la creación de una plantilla con nombre duplicado para el mismo canal, indicando el conflicto.

**Evidencia de derivación:** Responsabilidad "CRUD de plantillas con soporte para múltiples canales (HTML/Email, Push, SMS, WhatsApp)".

---

### US-TPL-02 — Consultar y buscar plantillas

**Tipo:** Funcional

**Historia:** "Como Gestor de Contenidos, quiero consultar y buscar plantillas por nombre, canal o estado, para localizar el contenido existente antes de reutilizarlo o editarlo."

**Criterios de aceptación:**

1. El listado muestra nombre, canal, versión vigente y estado de cada plantilla.
2. La búsqueda permite filtrar por canal y por nombre (coincidencia parcial).
3. El detalle de una plantilla muestra su contenido, sus variables y su historial de versiones.
4. La consulta no modifica el estado ni el contenido de la plantilla.

**Evidencia de derivación:** Componente clave `TemplateRepository` (capacidad de consulta del repositorio de plantillas).

---

### US-TPL-03 — Editar plantilla generando una nueva versión

**Tipo:** Funcional

**Historia:** "Como Gestor de Contenidos, quiero que cada modificación del contenido de una plantilla genere una nueva versión, para preservar íntegro el contenido que usaron campañas anteriores."

**Criterios de aceptación:**

1. Editar el contenido de una plantilla publicada crea una versión nueva numerada secuencialmente; la versión anterior permanece íntegra y consultable.
2. La versión más reciente queda como versión vigente de la plantilla.
3. Las referencias existentes a una versión concreta no se ven afectadas por la creación de versiones nuevas.
4. El historial de versiones registra fecha del cambio y autor para cada versión.

**Evidencia de derivación:** Responsabilidad "Versionado de plantillas".

---

### US-TPL-04 — Archivar plantilla obsoleta

**Tipo:** Funcional

**Historia:** "Como Gestor de Contenidos, quiero archivar una plantilla que ya no se usa, para que deje de ofrecerse para nuevas campañas sin perder su historial."

**Criterios de aceptación:**

1. Solo puede archivarse una plantilla que no esté asociada a una campaña programada o en ejecución; en caso contrario se rechaza la operación indicando el conflicto.
2. La plantilla archivada deja de aparecer en las búsquedas para nuevas campañas, pero sigue siendo consultable en modo histórico.
3. El archivo no elimina versiones ni contenido previo.
4. Una plantilla archivada puede reactivarse, volviendo a estar disponible para nuevas campañas.

**Evidencia de derivación:** Responsabilidad "CRUD de plantillas" (operación de baja controlada sobre `TemplateRepository`).

---

### US-TPL-05 — Definir placeholders requeridos por versión

**Tipo:** Funcional

**Historia:** "Como Gestor de Contenidos, quiero declarar qué variables son obligatorias en cada versión de la plantilla, para detectar datos faltantes antes de que se genere cualquier mensaje."

**Criterios de aceptación:**

1. Cada versión de plantilla permite marcar variables de su contenido como requeridas u opcionales.
2. Al guardar una versión, se valida que todo placeholder declarado como requerido aparezca en el contenido; si no, la operación se rechaza identificando la variable.
3. El conjunto de placeholders requeridos es consultable por versión, y dos versiones pueden declarar conjuntos distintos.
4. Una versión nueva hereda por defecto la marca de requerida de las variables que mantiene del contenido anterior, editable antes de guardar.

**Evidencia de derivación:** Responsabilidad "Definición de placeholders requeridos por versión".

---

### US-TPL-06 — Renderizar plantilla con datos del destinatario

**Tipo:** Funcional

**Historia:** "Como Sistema de Despacho, quiero obtener el contenido final de una versión de plantilla a partir de los datos de un destinatario, para entregar mensajes personalizados sin conocer el formato del contenido."

**Criterios de aceptación:**

1. Dada una versión de plantilla y un conjunto de datos de destinatario, el servicio devuelve el contenido con todas sus variables interpoladas.
2. Si falta el dato de una variable requerida, el renderizado se rechaza identificando la variable faltante y la versión afectada.
3. La ausencia de datos para variables opcionales no impide el renderizado (la variable se sustituye por un valor neutro definido como regla).
4. El renderizado es una operación de solo lectura: no modifica la plantilla ni sus versiones.

**Evidencia de derivación:** Responsabilidad "Renderizado e interpolación de variables" y componente clave `TemplateRenderService`.

---

### US-TPL-07 — Trazabilidad de operaciones sobre plantillas

**Tipo:** Instrumentación

**Historia:** "Como Operador del servicio, quiero que cada alta, nueva versión, archivo y reactivación de plantilla quede registrado con marca de tiempo y responsable, para reconstruir quién cambió el contenido y cuándo."

**Criterios de aceptación:**

1. Toda operación de creación, nueva versión, archivo y reactivación genera un registro con: operación, plantilla afectada, versión afectada, fecha y autor.
2. Los intentos de operación rechazados (p. ej. archivo de plantilla en uso) también generan registro con el motivo de rechazo.
3. Los registros son consultables por plantilla y por rango de fechas.
4. El registro almacena metadatos de la operación, no copias del contenido completo de la plantilla.

**Evidencia de derivación:** Responsabilidad "Versionado de plantillas" (trazabilidad de su ciclo de vida sobre `TemplateRepository`).

---

### US-TPL-08 — Métricas de creación y procesamiento de plantillas

**Tipo:** Métricas

**Historia:** "Como Operador del servicio, quiero métricas sobre la creación y el renderizado de plantillas, para detectar cuellos de botella y plantillas con datos faltantes recurrentes."

**Criterios de aceptación:**

1. Se contabilizan renderizados exitosos y fallidos, desglosados por plantilla y versión; la causa de fallo registrada distingue "variable requerida ausente" de otros fallos.
2. Se mide la duración del renderizado, agregada en percentiles (p. ej. mediana y cola larga).
3. Se contabilizan versiones creadas y plantillas archivadas por período.
4. Las métricas se exponen para consulta y monitorización, sin incluir datos personales de destinatarios ni el contenido de los mensajes.

**Evidencia de derivación:** Componente clave `TemplateRenderService` y responsabilidad "Renderizado e interpolación de variables" (medición de su procesamiento).
