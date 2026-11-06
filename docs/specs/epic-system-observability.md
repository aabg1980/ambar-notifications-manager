# EPIC: Instrumentación y Métricas del Sistema (Transversal)

> Épica transversal derivada del flujo completo `campaigns → dispatch → providers → tracking` de [`docs/architecture-slices.md`](../architecture-slices.md) y del slice `common` de [`architecture-scaffolding.md`](../architecture-scaffolding.md).
> Historias tecnóagnósticas: no nombran tecnologías concretas.

**Descripción**

Épica transversal que cubre la observabilidad **del conjunto** del flujo `campaigns → dispatch → providers → tracking`, complementando las métricas locales que cada slice reporta por su cuenta. Sus historias se justifican por el flujo completo: cada decisión de campaña, cada intento de despacho, cada interacción con un proveedor y cada resultado observado deben poder correlacionarse, medirse y vigilarse como un todo, no como silos. Se apoya en el slice `common` (que alberga "utilidades transversales y endpoints operativos (health) sin estado de feature") y en la telemetría que produce el slice `tracking`.

**Objetivo de negocio**

Garantizar operativamente el servicio de notificaciones: conocer en todo momento si estamos cumpliendo nuestros compromisos de entrega (SLIs/SLOs), detectar degradaciones antes de que los clientes las reporten, y retener la telemetría necesaria para el análisis histórico y la mejora de la operación.

**Roles:** **Operador de plataforma**, **Responsable del servicio de notificaciones**, **Analista de entregabilidad**.

## Historias de la épica

| ID | Título | Tipo |
|---|---|---|
| US-OBS-01 | Trazabilidad extremo a extremo de una notificación | Instrumentación |
| US-OBS-02 | SLI/SLO de tasa de entrega exitosa | Métricas |
| US-OBS-03 | SLI/SLO de latencia extremo a extremo | Métricas |
| US-OBS-04 | Tablero de operación del flujo de envío | Métricas |
| US-OBS-05 | Alerta por caída de tasa de entrega | Métricas |
| US-OBS-06 | Alerta por crecimiento del backlog de despacho | Métricas |
| US-OBS-07 | Retención y consulta histórica de telemetría | Métricas |
| US-OBS-08 | Instrumentación transversal homogénea | Instrumentación |

---

### US-OBS-01 — Trazabilidad extremo a extremo de una notificación

**Tipo:** Instrumentación

**Historia:** "Como Operador de plataforma, quiero seguir el recorrido completo de una notificación individual a través de todos los slices del sistema (campaña → despacho → proveedor → resultado), para diagnosticar incidentes sin reconstruir la historia a mano desde cuatro registros distintos."

**Criterios de aceptación:**

1. Dado una notificación creada por el desglose de una campaña, cuando inicia su recorrido, entonces se le asigna una referencia de traza única que acompaña a cada paso posterior (orden de envío, intento de despacho, interacción con proveedor, resultado observado).
2. Dado cualquier registro de log o métrica generado en cualquier slice del flujo, cuando se emite, entonces incluye la referencia de traza de la notificación y el identificador de la campaña origen.
3. Dado un Operador de plataforma con una referencia de traza, cuando la consulta, entonces obtiene la secuencia consolidada de pasos con sus marcas de tiempo y estados, con la capacidad de identificar en qué slice se produjo una demora o fallo.
4. Dado un fallo con reintentos (incluidos los que llegan a cola de mensajes fallidos), cuando se consulta la traza, entonces cada intento aparece como paso distinto con su resultado, no como un único evento sobrescrito.
5. Dado el flujo normal de una notificación exitosa, cuando se consulta la traza, entonces se distinguen al menos las fases: creación en campaña, despacho, interacción con proveedor y resultado final.

**Justificación (flujo completo):** una notificación nace en `campaigns`, se ejecuta en `dispatch`, se entrega vía `providers` y se observa en `tracking`. Sin una referencia común, ningún actor puede reconstruir el recorrido completo entre slices que no se importan entre sí.

---

### US-OBS-02 — SLI/SLO de tasa de entrega exitosa

**Tipo:** Métricas

**Historia:** "Como Responsable del servicio de notificaciones, quiero medir la tasa de entrega exitosa del sistema contra un objetivo definido (SLO), para saber si estamos cumpliendo nuestro compromiso de entrega con los clientes."

**Criterios de aceptación:**

1. Dado un periodo de medición definido (p. ej. por ventana móvil diaria y mensual), cuando se calcula la tasa de entrega exitosa, entonces es el cociente entre notificaciones con resultado "entregado" confirmado y notificaciones intentadas, desglosable por canal.
2. Dado el cálculo de la tasa, cuando se publica, entonces se expone junto con el objetivo (SLO) configurado y el estado de cumplimiento (dentro / en riesgo / incumplido).
3. Dado un resultado pendiente (enviada pero sin confirmación del proveedor), cuando se calcula la tasa, entonces no se contabiliza como éxito ni como fallo hasta tener resultado definitivo, o se reporta como categoría explícita "sin confirmar".
4. Dado un desglose solicitado, cuando se consulta, entonces la tasa es filtrable por canal, campaña y proveedor de canal, sin recalcular manualmente.
5. Dado el histórico de mediciones, cuando se consulta, entonces se conserva la serie de la tasa en el tiempo dentro de la política de retención de telemetría (ver US-OBS-07).

**Justificación (flujo completo):** la tasa de entrega exitosa es el resultado agregado de las cuatro etapas del flujo; ningún slice por sí solo puede calcularla.

---

### US-OBS-03 — SLI/SLO de latencia extremo a extremo

**Tipo:** Métricas

**Historia:** "Como Responsable del servicio de notificaciones, quiero medir la latencia desde que una notificación queda lista para envío hasta que se confirma su entrega, contra un objetivo definido, para detectar degradaciones de velocidad en cualquier etapa del flujo."

**Criterios de aceptación:**

1. Dada una notificación, cuando se confirma su entrega, entonces se calcula su latencia extremo a extremo (desde disponibilidad para envío hasta confirmación de entrega) y se agrega en percentiles (p. ej. mediana, p95, p99) por ventana de medición.
2. Dado el agregado de latencias, cuando se publica, entonces se compara contra el objetivo (SLO) configurado con estado de cumplimiento visible.
3. Dado un caso de reintento, cuando se mide la latencia, entonces se puede distinguir la latencia total de la latencia del último intento, para no ocultar el efecto de los reintentos.
4. Dado un desglose solicitado, cuando se consulta, entonces la latencia es descomponible por etapa del flujo (espera de despacho, procesamiento, interacción con proveedor) para localizar el cuello de botella.
5. Dado el histórico de mediciones, cuando se consulta, entonces se conserva la serie de latencia dentro de la política de retención de telemetría (ver US-OBS-07).

**Justificación (flujo completo):** la latencia atraviesa las cuatro etapas del flujo; es intrínsecamente transversal.

---

### US-OBS-04 — Tablero de operación del flujo de envío

**Tipo:** Métricas

**Historia:** "Como Operador de plataforma, quiero un tablero de operación que muestre en vivo el estado del flujo de envío completo (volumen en cada etapa, éxitos, fallos y pendientes), para vigilar la salud del servicio y anticipar saturaciones."

**Criterios de aceptación:**

1. Dado el tablero en operación, cuando se visualiza, entonces muestra indicadores por etapa del flujo: notificaciones pendientes de despacho, en interacción con proveedor, confirmadas, fallidas y pendientes de confirmación.
2. Dado el tablero, cuando se visualiza, entonces los indicadores son desglosables por canal y por campaña activa.
3. Dado el tablero, cuando se visualiza, entonces incluye el estado de los componentes operativos de salud del sistema (endpoints de salud) y el estado de las colas de mensajes (incluida la cola de mensajes fallidos).
4. Dado un intervalo de actualización definido, cuando el tablero está en uso, entonces los indicadores se actualizan con una frecuencia apta para operación en tiempo casi real (definir umbral en la conversación con el equipo).
5. Dado un indicador del tablero, cuando se selecciona, entonces permite navegar a la vista detallada correspondiente (p. ej. lista de notificaciones fallidas de un canal).

**Justificación (flujo completo):** el tablero debe mostrar el estado en vivo de cada etapa del flujo; es la vista operativa del conjunto.

---

### US-OBS-05 — Alerta por caída de tasa de entrega

**Tipo:** Métricas

**Historia:** "Como Operador de plataforma, quiero recibir una alerta cuando la tasa de entrega exitosa cae por debajo del umbral definido, para intervenir antes de que los clientes reporten el problema."

**Criterios de aceptación:**

1. Dada la tasa de entrega exitosa medida (US-OBS-02), cuando cae por debajo del umbral configurado durante la ventana de evaluación definida, entonces se emite una alerta al canal de notificación operativa configurado.
2. Dada una alerta emitida, cuando se notifica, entonces incluye el valor actual, el umbral, el periodo afectado y el desglose por canal para acelerar el diagnóstico.
3. Dada una condición de alerta que persiste, cuando se mantiene, entonces la alerta no se re-emite de forma repetitiva e invasiva (mecanismo de supresión o escalamiento definido en la conversación).
4. Dada una recuperación de la tasa por encima del umbral, cuando se produce, entonces se emite la correspondiente notificación de resolución de la alerta.
5. Dado un cambio de umbral aprobado, cuando se aplica, entonces queda registrado quién y cuándo lo modificó (auditoría de configuración de alertas).

**Justificación (flujo completo):** una caída de tasa puede originarse en cualquiera de las etapas; solo la medición agregada del flujo completo la detecta a tiempo.

---

### US-OBS-06 — Alerta por crecimiento del backlog de despacho

**Tipo:** Métricas

**Historia:** "Como Operador de plataforma, quiero recibir una alerta cuando el volumen de notificaciones pendientes de despacho crece de forma sostenida, para escalar capacidad o pausar campañas antes de que el backlog se vuelva crítico."

**Criterios de aceptación:**

1. Dado el volumen de notificaciones pendientes de despacho medido, cuando supera el umbral configurado o crece de forma sostenida durante la ventana de evaluación definida, entonces se emite una alerta operativa.
2. Dada una alerta emitida, cuando se notifica, entonces incluye el tamaño actual del backlog, su tendencia (creciente / estable) y la tasa de consumo actual de la cola de mensajes.
3. Dado un backlog que incluye mensajes en la cola de mensajes fallidos, cuando se emite la alerta, entonces distingue entre pendientes normales y acumulación de fallidos (condiciones de alerta distintas o desglose explícito).
4. Dada una recuperación del backlog por debajo del umbral, cuando se produce, entonces se emite la notificación de resolución de la alerta.
5. Dada una alerta de backlog, cuando se revisa, entonces permite identificar las campañas activas que más contribuyen al volumen pendiente.

**Justificación (flujo completo):** el backlog se acumula en el punto de encuentro entre `campaigns` (generación de órdenes) y `dispatch` (consumo de colas); su crecimiento sostenido anticipa incumplimientos de latencia y de tasa de entrega.

---

### US-OBS-07 — Retención y consulta histórica de telemetría

**Tipo:** Métricas

**Historia:** "Como Analista de entregabilidad, quiero que la telemetría del sistema se retenga según una política definida y sea consultable históricamente, para comparar el desempeño entre campañas y responder requerimientos de auditoría pasado el tiempo."

**Criterios de aceptación:**

1. Dada la política de retención configurada (periodos diferenciados para métricas agregadas y para eventos de auditoría por notificación), cuando se cumple el plazo de un dato, entonces se archiva o elimina según la política, de forma automática.
2. Dado un periodo histórico dentro de la retención, cuando un Analista consulta métricas (tasa de entrega, latencia, interacciones), entonces obtiene datos agregados por el periodo solicitado con la misma definición de cálculo que las métricas en vivo.
3. Dado un evento de auditoría por notificación dentro de la retención, cuando se consulta, entonces es accesible con el mismo detalle que en caliente (ver US-TRK-06).
4. Dado un cambio de política de retención aprobado, cuando se aplica, entonces queda registrado y no destruye datos de forma retroactiva sin aprobación explícita (p. ej. borrado masivo requiere confirmación).
5. Dado un volumen creciente de telemetría, cuando se almacena, entonces el costo o tamaño de almacenamiento se mantiene acotado por la política (sin crecimiento indefinido).

**Justificación (flujo completo):** la telemetría tiene valor analítico más allá de la ventana operativa inmediata; sin política de retención explícita, o se pierde historia o se crece sin control.

---

### US-OBS-08 — Instrumentación transversal homogénea

**Tipo:** Instrumentación

**Historia:** "Como Operador de plataforma, quiero que todos los slices del sistema instrumenten sus operaciones con estándares comunes (logs estructurados, métricas de operación y señales de salud), para que la observabilidad del conjunto sea consistente y no dependa de convenciones ad hoc por equipo."

**Criterios de aceptación:**

1. Dada una operación en cualquier slice del flujo (renderizado de plantilla, programación de campaña, despacho, interacción con proveedor, ingesta de reporte), cuando se ejecuta, entonces genera logs estructurados con campos comunes: referencia de traza (US-OBS-01), slice origen, operación, resultado y duración.
2. Dada cada etapa del flujo, cuando se ejecuta, entonces expone métricas de operación estándar: conteo de operaciones, conteo de errores y duración de las operaciones, con etiquetas comunes (slice, canal, operación).
3. Dado cada componente operativo del sistema, cuando se consulta su estado, entonces expone una señal de salud consistente (saludable / degradado / caído) a través de los endpoints operativos de salud del slice `common`.
4. Dado un slice nuevo que se incorpora al sistema, cuando se desarrolla, entonces cumple el estándar de instrumentación sin acuerdos adicionales (el estándar está documentado y es verificable en revisión).
5. Dado un log o métrica emitida, cuando se revisa, entonces no contiene datos sensibles del destinatario (direcciones de contacto o contenido del mensaje) en texto plano — se usa una referencia no sensible.

**Justificación (flujo completo):** sin estándares comunes de logs, métricas y salud, la trazabilidad (US-OBS-01) y los tableros (US-OBS-04) no pueden construirse de forma consistente. Se apoya en el slice `common`.
