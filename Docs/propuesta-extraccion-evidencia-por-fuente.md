# Propuesta: evidencia por fuente antes de sintetizar

Estado actualizado (01/10/2026): contrato, persistencia por etapas, captura independiente e integración implementados en political-v11. Véase el [expediente y sus límites](../POC/cross-check-service/docs/evidence-dossier.md). El contrato público sigue en v3. Las pruebas automáticas pasan, pero la síntesis real fue rechazada por referencias y elegibilidad temporal; la aceptación real no está cerrada. [Revisión v11](../POC/cross-check-service/docs/openai/political-v11-live-review.md). El resto conserva el diseño objetivo y no implica que toda interpretación semántica esté resuelta.

## Problema y decisión propuesta

La [prueba de political-v10](../POC/cross-check-service/docs/openai/political-v10-live-review.md) demuestra que añadir instrucciones no garantiza una respuesta fiable: el agente reformuló un anclaje literal, cruzó FEDEA con el identificador del BOE, devolvió fechas incorrectas y clasificó una cautela metodológica como oposición. Java detectó el anclaje, pero no se debe atribuir al validador actual una detección de los otros errores.

Proponemos separar la investigación y la síntesis mediante un expediente de evidencia persistido y revisable. El servicio controla los identificadores y los metadatos aceptados; el modelo interpreta el contenido y redacta. La comprobación estructural puede automatizarse, pero la verdad de una interpretación no se demuestra porque un JSON sea válido.

Se propone empezar con dos etapas de modelo por consulta que llegue a informe: investigación con extracción estructurada y síntesis del expediente aceptado. La extracción agrupa las fuentes en una llamada; no implica una llamada por documento. Una aclaración termina antes de sintetizar. La disponibilidad concreta de herramientas y la configuración del proveedor se comprobarán al implementar, antes de publicar.

## Flujo y responsabilidades

1. **Conservar la entrada.** Java guarda la pregunta y el contexto recibido como ahora. La etapa de investigación devuelve el objetivo interpretado y los anclajes literales que lo vinculan a la entrada. Java aplica la validación actual antes de avanzar. No corrige un anclaje mediante similitud ni cambia la proposición. La síntesis recibe las afirmaciones aceptadas y no vuelve a crearlas.
2. **Investigar y extraer.** El modelo localiza documentos y devuelve registros separados por fuente, con identidad, contenido utilizado y hallazgos. Una publicación y el estudio que cita son registros distintos cuando se consultan ambos. Una referencia indirecta conserva esa condición y no se convierte en una lectura del original.
3. **Construir el expediente.** Java asigna IDs estables a las fuentes aceptadas y a sus hallazgos. No basta con numerar de nuevo el JSON final: la asociación documento–fragmento–hallazgo debe quedar fijada antes de sintetizar. La deduplicación no elimina versiones o publicaciones distintas solo porque compartan estudio.
4. **Validar el expediente.** Se comprueban identidad, fechas respaldadas, acceso, referencias y coherencia de las decisiones contables. Se separan metadatos comprobados de afirmaciones extraídas por el modelo. Si una fecha exacta no está respaldada, se conserva su precisión conocida y publishedAt será null; no se inventa el día. Un fallo estructural impide avanzar y conserva el diagnóstico.
5. **Sintetizar.** El modelo recibe las afirmaciones y la evidencia aceptadas, con referencias a hallazgos. No introduce fuentes ni fechas nuevas. Debe indicar qué respalda, matiza o contextualiza cada conclusión, y distinguir incertidumbre de oposición. La ausencia de evidencia suficiente puede llevar a un informe válido de insuficiencia; un fallo técnico no se transforma en ese veredicto.
6. **Ensamblar y validar.** Java genera las fuentes y metadatos públicos desde el expediente, resuelve las referencias a hallazgos y mantiene las reglas de valoración y posicionamiento actuales. Después ejecuta la validación final. Solo un resultado válido pasa a COMPLETED.

El frontend conserva su contrato público v3 y la disposición actual. Los nuevos documentos intermedios necesitan un contrato interno versionado propio; no se insertan sin más en el esquema final del agente actual. La revisión de conversación debe cambiar cuando se active este flujo.

## Contenido mínimo del expediente interno

Los siguientes nombres son propuestas de campos, no un esquema ejecutable todavía.

| Grupo | Campos propuestos | Para qué sirven |
|---|---|---|
| Identidad | sourceId, requestedUrl, resolvedUrl, title, publisher, authors, documentVersion | Mantener la relación con el documento exacto y distinguir autor de editor. |
| Acceso | accessStatus, retrievedAt, contentHash, contentRef | Saber qué contenido se pudo leer y conservar una referencia al extracto o documento disponible. retrievedAt lo aporta el servicio, no el modelo. |
| Fecha | publicationDate, datePrecision, dateEvidence, dateKind | Distinguir publicación, resolución, actualización y periodo estudiado. dateEvidence referencia el contenido que respalda el dato. |
| Hallazgo | findingId, sourceId, excerptRef, paraphrase, attribution, method | Vincular cada interpretación a una fuente y un pasaje; distinguir descripción, correlación, causalidad, predicción y cita de tercero. |
| Alcance | outcome, geography, studyPeriod, policy, temporalRelation | Evitar convertir anuncios en parque arrendado o un estudio anterior en evidencia del periodo solicitado. |
| Postura | propositionId, scopeMatch, stanceOwner, stanceHolder, stance, rationale, findingIds | Separar postura y solidez empírica, preservando los requisitos COUNT/EXCLUDE. |
| Revisión | validationStatus, validationIssues | Registrar qué se comprobó, qué sigue siendo una interpretación y por qué se excluyó una entrada. |

Los fragmentos deben ser breves y suficientes para revisar el hallazgo; no se propone conservar ni publicar copias completas indiscriminadamente. Los localizadores deben referirse a la versión guardada o a una página/sección identificable, no a números de línea efímeros de una herramienta.

## Qué puede comprobar Java y qué no

| Comprobación | Garantía posible | Límite |
|---|---|---|
| Anclaje literal | El fragmento aparece una sola vez en los textos permitidos. | No prueba que la interpretación preserve el sentido. |
| Identidad de referencia | Un findingId pertenece a un sourceId y la síntesis no puede reasignarlo a otra fuente. | No prueba que el modelo interpretara correctamente el pasaje. |
| Fragmento | Su texto coincide con el contenido recuperado y está ligado a su hash. | Una cita real puede usarse fuera de contexto. |
| Fecha | Fecha válida, precisión y tipo; coincidencia con un dato editorial inequívoco disponible. | Extraer una fecha del texto no prueba por sí solo que sea la publicación. Los casos ambiguos quedan sin fecha exacta. |
| Posicionamiento | COUNT cumple alcance, periodo y atribución; referencias completas y sin duplicación. | Decidir si un autor realmente apoya o cuestiona exige interpretación y evaluación semántica. |
| Síntesis | Todas las referencias existen y proceden del expediente aceptado. | No demuestra que cada conclusión esté suficientemente respaldada. |

**Condición necesaria:** el contenido que se pretende verificar debe estar realmente disponible para el servicio. Un extracto que el propio modelo afirma haber leído no constituye una comprobación independiente. La implementación tendrá que resolver la recuperación autorizada del documento o el acceso al contenido de investigación del proveedor. Si no está disponible, se registra como no verificable; no se etiqueta como verificado ni se sustituye por una cita inventada. La adquisición debe limitar tamaño, tiempo, redirecciones y destinos permitidos, especialmente si incorpora URLs facilitadas por el usuario.

## Posicionamiento: conservar las reglas acordadas

- Una limitación metodológica no se convierte automáticamente en QUESTIONS. Hace falta un argumento propio que cuestione la proposición concreta; después se comprueba su alcance y periodo.
- Una publicación sin postura puede ser NO_EXPLICIT_POSITION si cumple elegibilidad. La falta de acceso no equivale a neutralidad.
- Una postura explícita puede contarse aunque la prueba causal sea insuficiente. Eso no convierte la afirmación en un hecho probado.
- El argumento nacional no exige mediciones en todas las regiones, pero tampoco se inventa a partir de un dato local.
- SINGLE evaluado sin entradas contables conserva AVAILABLE y exclusiones. MULTIPLE conserva la regla de posicionamiento UNAVAILABLE.
- El servicio sigue derivando el veredicto final; no cambia colores, etiquetas ni ubicación del panel.

## Entrega asíncrona, recuperación y coste

El diseño actual conserva un único sessionId/turnId por trabajo. No basta con añadir una segunda llamada dentro de inspect: se necesita un registro duradero por etapa con stageId, inputHash, providerSessionId, providerTurnId, submissionState, outputRef y validationStatus. La transición se persiste antes de iniciar la siguiente etapa.

Tras un reinicio se recupera la etapa pendiente, sin repetir una extracción ya aceptada ni iniciar otra síntesis por una respuesta perdida. Cada envío conserva su identidad estable y la reconciliación actual se aplica por etapa. Una entrega ambigua sigue reservando capacidad hasta confirmar su estado remoto. El frontend puede seguir mostrando RUNNING durante ambas etapas; no se añaden estados públicos en esta propuesta.

Las etapas comparten el plazo y los límites del trabajo: no se duplican automáticamente los diez minutos actuales. La implementación debe definir presupuestos de fuentes, tamaño de extractos y salida, con registro de consumo por etapa. Dos etapas añaden coste y latencia potenciales; no se promete ahorro por usar menos herramientas durante la síntesis. No se contempla un ciclo automático ilimitado de reparación ni un modelo juez adicional de pago.

## Casos de regresión y criterios de aceptación

La [manifestación de casos v10](proposals/source-evidence-regression-v10.json) referencia la respuesta original con SHA256 y distingue diagnóstico reproducido, hallazgos manuales y automatización pendiente. No modifica la respuesta original ni la presenta como informe válido.

| Caso | Resultado que debe demostrar la implementación |
|---|---|
| EV01: anclaje reformulado | Rechazo antes de sintetizar; texto original conservado. |
| EV02: argumento FEDEA atribuido a BOE | La referencia a un hallazgo no puede cambiar de fuente al ensamblar; añadir también un caso de extracción inicialmente mal atribuida para evaluar la revisión semántica. |
| EV03/EV04: fechas BOE y Banco de España | Fecha de publicación extraída de evidencia editorial fijada; resolución o día inventado no sustituyen publicación. |
| EV05: falta de estudios interpretada como oposición | Evaluación semántica revisable; no marcar PASS solo por eliminar QUESTIONS. Revisar también política, periodo y elegibilidad. |
| Fecha incompleta o conflicto | Mantener precisión conocida, publishedAt null y limitación, sin inventar componentes. |
| Fuente inaccesible | No producir hallazgos del contenido no leído ni votos neutrales por defecto. |
| Reinicio o respuesta perdida entre etapas | Una extracción y una síntesis como máximo, sin perder el expediente ni liberar una reserva remota ambigua. |
| Expiración antes de síntesis | No iniciar otra etapa fuera del plazo; conservar diagnóstico técnico. |
| Síntesis sin fuente suficiente | Insuficiencia solo si hay una investigación válida que lo sustenta; no ocultar un expediente corrupto como insuficiencia. |

Las 16 evaluaciones semánticas anteriores continúan pendientes. Estos casos adicionales son criterios de aceptación del diseño, no nuevas pruebas ejecutadas.

## Orden de implementación para las siguientes fases

1. **Contrato interno y pruebas offline:** definir expediente, referencias y ensamblador; fijar extractos de prueba y reproducir las regresiones sin proveedor. Revisar ejemplos y resultados antes de seguir.
2. **Orquestación persistente:** incorporar etapas, recuperación, límites y simulaciones de error. Demostrar ausencia de duplicados antes de usar OpenAI.
3. **Integración del proveedor:** confirmar cómo se obtiene contenido verificable y configurar extracción/síntesis; preparar las instrucciones y contratos para revisar antes de publicar.
4. **Prueba real limitada:** con autorización, publicar la revisión correspondiente y ejecutar una consulta; comparar calidad, consumo y tiempo. Detenerse para revisar antes de ampliar la batería.

La fase inicial terminó con el diseño y los casos. La fase posterior ha incorporado el contrato Java del expediente, la proyección de fuentes y 15 pruebas offline; todavía no ejecuta las etapas con el proveedor.
