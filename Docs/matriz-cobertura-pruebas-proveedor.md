# Matriz de cobertura de pruebas del proveedor

Actualización political-v15: 342 pruebas registradas, 340 superadas y dos omitidas. Regresiones para criterio público controlado por Java y admisión de publicaciones elegibles sin postura propia; respuesta real v14 reensamblada offline sin inferencia. [Detalle](../POC/cross-check-service/docs/openai/political-v15-review.md).

Actualización political-v14: 340 pruebas registradas, 338 superadas y dos omitidas. Regresión para evitar publicación automática de impresiones de investigación no verificadas e identificación de documentos omitidos. El cumplimiento editorial del modelo queda pendiente de prueba real. [Detalle](../POC/cross-check-service/docs/openai/political-v14-review.md).

Actualización political-v13: 339 pruebas registradas, 337 superadas y dos omitidas. Regresión Java y pruebas de esquema para explicación obligatoria, no nula ni vacía; no se ha repetido inferencia. [Detalle](../POC/cross-check-service/docs/openai/political-v13-review.md).

Actualización political-v12: 338 pruebas registradas, 336 superadas y dos omitidas. Tres nuevas regresiones cubren candidatos inaccesibles, exclusión temporal con evidencia citable e IDs elegibles. Se revisó el expediente real guardado sin otra inferencia; no equivale a aceptación real del modelo. [Detalle](../POC/cross-check-service/docs/openai/political-v12-review.md).

## Actualización political-v11 — 01/10/2026

Suite completa: 335 pruebas registradas, 333 superadas, dos opcionales omitidas y cero fallos. Navegador independiente: 16 escenarios offline superados. Se añaden pruebas de expediente, adquisición HTML/PDF, límites de red, fechas, ensamblado, duplicados y recuperación por etapas. La ejecución real tuvo una investigación y una síntesis; terminó INVALID_ANALYSIS_OUTPUT por periodo y referencias inválidas. No se considera cubierta la calidad semántica de todas las respuestas ni aprobada la aceptación real. [Detalle](../POC/cross-check-service/docs/openai/political-v11-live-review.md).

Fecha de revisión: 26/09/2026.

Estado: diagnóstico de cobertura y propuesta de ampliación. Este documento no implica que los huecos señalados estén implementados o probados.

## Alcance y lectura

La revisión comprende el dominio Java, la aplicación, el adaptador OpenAI, las pruebas de integración HTTP y los scripts de navegador del frontend. Se basa en la inspección del código y las pruebas existentes, no en una medición de cobertura de líneas o ramas.

Las 179 pruebas de la última ejecución integrada son el total del proyecto, no 179 variantes de respuestas de OpenAI. Incluyen pruebas parametrizadas y una prueba opcional de navegador activada con `-Dbrowser.temporal=true`. Otros scripts de navegador se ejecutan por separado. No debe usarse ese total como porcentaje de cobertura del proveedor.

«Cobertura comprobada» identifica casos con pruebas existentes. No significa cobertura exhaustiva de todas las combinaciones. Una validación implementada sin una prueba específica no se considera un caso suficientemente verificado.

## Matriz

| ID | Área | Cobertura comprobada | Huecos principales | Prioridad |
| --- | --- | --- | --- | --- |
| C01 | Respuesta válida | Creación de sesión, lectura del informe y seguimiento. | Recorrer los seis veredictos documentales a través del adaptador real, con proveedor HTTP simulado. | 1 |
| C02 | JSON incorrecto | Texto no JSON, `null`, objeto vacío, array y algunos tipos incorrectos. | Campos omitidos, nulos y tipos incorrectos campo por campo, incluidos los anidados. | 1 |
| C03 | JSON ambiguo | El lector está configurado para rechazar claves duplicadas y contenido sobrante. | Pruebas específicas de claves duplicadas, objetos consecutivos y bloques Markdown. La configuración no sustituye estas pruebas. | 1 |
| C04 | Enumerados | Rechazo de veredicto numérico; reglas de estados derivados. | Valores desconocidos en veredicto, posición y disponibilidad; variantes de mayúsculas/minúsculas. | 1 |
| C05 | Referencias | Fuentes inexistentes, IDs duplicados, doble clasificación y exclusiones incompatibles. | Pasar más variantes por la deserialización del proveedor, además de las pruebas del dominio. | 1 |
| C06 | Fechas | Fechas de publicación incompletas o imposibles, `null`, límites inclusivos, periodos y corte incoherentes. | Fechas inválidas en `asOf`, límites del periodo y `consultedAt`; más casos límite. | 1 |
| C07 | Clasificación y veredicto final | Pluralidades, empates, ausencia de muestra y conservación de otros veredictos. | Combinaciones inválidas adicionales entre disponibilidad, unidades y metadatos. | 1 |
| C08 | Estados del turno | `completed`, `in_progress`, `failed`, `cancelled`, `waiting`. | `queued`, estado desconocido, turno ausente, sesión incorrecta y subagente inesperado. | 2 |
| C09 | Mensaje final | Lectura del turno correcto e ignorar comentarios. | Mensaje final ausente, duplicado, incompleto, contenido vacío, rechazo del modelo y tipos de contenido inesperados. | 2 |
| C10 | Paginación | Recorrido correcto de varias páginas. | Cursor repetido, cursor ausente, `has_more` inválido y agotamiento del límite. | 2 |
| C11 | Errores HTTP | Varios códigos, ausencia de reintento y protección de detalles internos. | `408`, `410` y errores en cada etapa, especialmente después de enviar la consulta. | 2 |
| C12 | Transporte y límites | Timeout inicial, cuerpo HTTP excesivo y límite de concurrencia. | Conexión interrumpida, cuerpo truncado, límites exactos y tamaño máximo del informe. | 2 |
| C13 | Concurrencia | Saturación y recuperación de capacidad. | Dos seguimientos simultáneos sobre la misma sesión y liberación de su bloqueo. | 3 |
| C14 | Frontend y recuperación | Errores seleccionados, timeout, rechazo temporal y nueva consulta correcta. | Rechazo temporal durante un seguimiento y reintento manual de ese error concreto. | 3 |
| C15 | Calidad del análisis | Revisiones manuales de respuestas reales y diez casos sintéticos con expectativas documentadas. | Sin evaluación semántica automatizada de fechas reales, atribución, alcance, idioma y elección fundamentada del veredicto. | 4 |

## Evidencias de la revisión

Rutas relativas a la raíz del repositorio:

| Áreas | Archivos principales |
| --- | --- |
| C01–C06, C08–C12 | `POC/cross-check-service/infrastructure/src/test/java/com/crosscheck/infrastructure/openai/OpenAiPoliticalAnalysisServiceTest.java` |
| C02–C07 | `POC/cross-check-service/domain/src/test/java/com/crosscheck/domain/analysis/AnalysisReportTest.java` |
| C07 | `POC/cross-check-service/application/src/test/java/com/crosscheck/application/features/analysis/start/FinalVerdictPolicyTest.java` |
| C01, C11, C14 | `POC/cross-check-service/bootstrap/src/test/java/com/crosscheck/infrastructure/openai/OpenAiApiTest.java` y `POC/cross-check-service/bootstrap/src/test/java/com/crosscheck/bootstrap/AnalysisApiTest.java` |
| Gestión de consultas y sesiones | `POC/cross-check-service/application/src/test/java/com/crosscheck/application/features/analysis/start/StartAnalysisHandlerTest.java` |
| C13 | `POC/cross-check-service/infrastructure/src/test/java/com/crosscheck/infrastructure/openai/OpenAiPoliticalAnalysisServiceTest.java` |
| C14 | `POC/webapp/scripts/verify.cjs`, `verify-verdicts.cjs`, `verify-timeouts.cjs` y `verify-temporal-error.cjs` en la misma carpeta |
| C15 | `POC/cross-check-service/docs/openai/political-regression-cases.md` y `POC/cross-check-service/docs/openai/publications-live-review.md` |

La inspección del comportamiento implementado se apoyó también en `OpenAiPoliticalAnalysisService.java`, `OpenAiJson.java` y `OpenAiTransport.java`, dentro de `POC/cross-check-service/infrastructure/src/main/java/com/crosscheck/infrastructure/openai`.

## Límites de la validación automática

Una fecha puede ser válida y coherente dentro del JSON, pero no coincidir con la página citada. Una clasificación editorial puede cumplir el contrato y atribuir al artículo un argumento que no contiene. Las pruebas unitarias del contrato no establecen la verdad de la conclusión política.

Conviene separar tres niveles:

1. **Contrato y reglas deterministas:** estructura, tipos, referencias, fechas internas y cálculo del veredicto derivado. Se comprueban con pruebas locales.
2. **Integración y recuperación:** estados del proveedor, errores, sesiones y presentación en el navegador. Se comprueban con un proveedor HTTP simulado y componentes reales del servicio.
3. **Calidad semántica:** fidelidad a las fuentes, correspondencia con la proposición, alcance temporal y fundamento del veredicto. Requiere casos con fuentes y expectativas revisadas. Los diez casos documentados actualmente no son resultados obtenidos del modelo ni una evaluación automatizada superada.

## Orden propuesto de ampliación

1. **Contrato del informe (C01–C07):** pruebas parametrizadas por campo y regla. Incluir respuestas válidas, inválidas y límites para evitar rechazos excesivos.
2. **Protocolo del proveedor (C08–C12):** turnos, mensajes finales, paginación y errores por etapa. Verificar también el estado de ejecución comunicado y la ausencia de reintentos.
3. **Recuperación (C13–C14):** exclusión mutua por sesión, liberación de recursos, seguimiento fallido y reintento manual.
4. **Calidad semántica (C15):** convertir los fallos reales en casos de evaluación con fuentes identificables y resultados esperados, sin confundirlos con pruebas de contrato.

Cada bloque se presentará antes de ejecutarlo y se cerrará para revisión del usuario. Las pruebas locales no requieren consultas de pago. Las evaluaciones reales del agente se acordarán por separado.

## Criterios de cierre

- Cada caso acordado tiene un identificador, comportamiento esperado y prueba o evaluación asociada.
- Los casos inválidos comprueban el rechazo correcto, sin valoración factual, filtración de contenido sensible ni reintento automático.
- Los casos válidos y los límites aceptados siguen funcionando.
- Los huecos pendientes se mantienen explícitos, distinguiendo ausencia de implementación y ausencia de pruebas.
- Los fallos reales observados se incorporan a regresiones técnicas o evaluaciones semánticas, según corresponda.
- No se declara cobertura total del proveedor por el número de pruebas superadas.

Este documento no cambia el código ni añade pruebas. El primer bloque propuesto para ejecución es el contrato del informe.

### Regresión temporal de political-v7 — 28/09/2026

OpenAiTracedReportTest incorpora cinco ejecuciones adicionales: dos relaciones temporales con publishedAt desconocida, retrospectiva publicada después del periodo sin corte histórico, rechazo de evidencia posterior a un corte explícito y rechazo del informe real contradictorio guardado. Resultado de la clase: 27 pruebas correctas, cero omisiones.

scripts/verify-agent-schema.py comprueba 33 casos sin inferencia, incluida la aceptación de ejemplos completos, fechas desconocidas, rechazo del informe real, combinaciones COUNT incompatibles y su representación como exclusiones explícitas en ejemplos sintéticos. No valida fidelidad a fuentes ni todas las reglas semánticas de Java. No se volvió a ejecutar la batería completa ni el navegador en esta fase, al no cambiar el código de producción ni la interfaz.

### Diseño pendiente: veredicto por afirmaciones — 28/09/2026

Se preparó Docs/propuesta-veredicto-por-afirmaciones.md, un fragmento JSON Schema, ejemplos y una política ejecutable de diseño con 18 casos locales correctos. Propone reservar NO_SINGLE_VERDICT a la agregación en Java de varias afirmaciones con estados documentales diferentes. Incluye decisiones pendientes sobre posicionamiento para consultas múltiples y anclaje al texto original; no acredita corrección semántica.

No está implementado en el servicio ni en el frontend. El contrato activo sigue siendo v2 y la revisión local political-v8. No se publicó el diseño ni se ejecutaron inferencias. Consultar la propuesta antes de la siguiente fase.

### Implementación local del contrato v3 — 28/09/2026

Estado vigente: implementado y probado con proveedor simulado; no publicado en OpenAI. El esquema completo está en Docs/proposals/political-report-v3.schema.json y su ejemplo ficticio en political-report-v3.example.json. No se envía verdict global: claimAnalysis contiene objetivo, descomposición y conclusiones por afirmación. El fragmento proposalVersion del diseño no forma parte del informe v3.

ClaimAnalysis valida cardinalidad, tipos de conclusión, duplicados y anclajes literales únicos y no solapados contra el mensaje recibido. Deriva el código y conserva la explicación, respaldo y referencias de cada afirmación. OpenAiClaimReport reutiliza las validaciones de fuentes, trazabilidad y fechas. El lector se selecciona explícitamente con report-schema-version=3; no hay fallback al formato anterior.

La API incorpora claimAnalysis nullable mediante DTOs propios. Los informes v1/v2 y el proveedor dev conservan claimAnalysis null. El frontend acepta ausencia/null en el historial y muestra los nuevos detalles junto a la valoración final cuando existen, con referencias navegables y etiquetas españolas. El lugar del panel de publicaciones no cambia.

SINGLE conserva la política de pluralidad editorial cuando su conclusión documental es insuficiente; además, una muestra disponible debe declarar exactamente la misma proposición que la afirmación. MULTIPLE exige publicación UNAVAILABLE sin unidades ni exclusiones, hasta disponer de clasificación por afirmación: no se mezclan posiciones de cuestiones diferentes. La conclusión documental individual permanece visible aunque el resultado final de SINGLE se derive de publicaciones.

Limitación activa del lector v3: los anclajes se comprueban contra el mensaje actual, incluso en seguimientos. Una respuesta que reutilice fragmentos de otro turno se rechaza; no se sintetiza contexto verificable a partir de la salida del modelo. El caso «¿y después?» está cubierto como rechazo del fixture, no como funcionalidad conversacional resuelta. Antes de activar v3 debe revisarse el tratamiento de seguimientos contextuales y la adaptación de las instrucciones del agente.

Verificación: Maven verify con pruebas de navegador y repeticiones dirigidas tras ampliar los casos; informes finales de 244 pruebas, cero fallos, errores u omisiones. npm run build correcto. 36 comprobaciones locales del esquema correctas. El recorrido v3 se prueba con API Java real, upstream local simulado y navegador en escritorio/móvil; también referencias, historial tras recarga, respuestas inválidas y compatibilidad con claimAnalysis ausente/null. Capturas en POC/webapp/review/integration/claims-1536.png y claims-390.png. Se mantiene la regresión del error temporal de v2.

La configuración activa sigue siendo OPENAI_REPORT_SCHEMA_VERSION=2 y POLITICAL_AGENT_REVISION=political-v8. No se cambiaron el agente remoto, las instrucciones activas ni las credenciales y no se ejecutaron inferencias de pago. La aceptación del esquema v3 por OpenAI y la calidad semántica del modelo siguen sin comprobarse.

### Contexto de seguimientos y aclaraciones v3 — 28/09/2026

Implementado y verificado localmente, pendiente de publicación del agente. La configuración activa sigue en esquema 2 y political-v8. El servicio no solicita al proveedor v2 el nuevo formato.

El servidor conserva instantáneas de contexto: último mensaje relevante, objetivo aceptado, proposiciones y pregunta de aclaración pendiente. Cada token incluye un contextId autenticado y cifrado; no transporta el texto de la conversación. La instantánea se vincula también a sessionId. Un token antiguo referencia su propia instantánea; no se sobrescribe con la última respuesta.

La POC usa memoria del proceso, máximo 1000 instantáneas y 32000 caracteres por contexto. La caducidad coincide con la del token (por defecto dos horas desde la respuesta); se purgan entradas vencidas al acceder al almacén y se expulsan las más antiguas cuando se alcanza el límite. No hay persistencia, distribución entre instancias ni garantía de conservación tras reinicios. Los tokens ya vencidos conservan el error de expiración existente.

Si el token válido referencia contexto perdido por reinicio o expulsión, el servicio devuelve una aclaración local CONTEXT_UNAVAILABLE sin llamar al proveedor. conversationToken queda null, la UI pide la consulta completa y el siguiente envío inicia una sesión nueva. El contexto no se reconstruye a partir del historial modificable del navegador.

La entrada de v3 es un sobre de datos con currentInput y previousContext. Los anclajes se buscan en el mensaje actual y en los textos de la instantánea del servidor, no en una pregunta combinada inventada por el proveedor. Se permiten fragmentos de mensajes anteriores guardados. La pregunta de aclaración del agente no sirve como anclaje. No se verifica automáticamente la fidelidad semántica del objetivo anterior ni la conservación correcta de cada matiz; se verifica procedencia e integridad del contexto.

La respuesta del proveedor v3 usa Docs/proposals/political-response-v3.schema.json: schemaVersion, analysis y clarification. Debe existir exactamente un resultado no nulo; Java rechaza ambos o ninguno. analysis contiene el informe v3 existente. clarification contiene question y reason (MISSING_PERIOD, MISSING_SCOPE, AMBIGUOUS_REFERENCE u OTHER). CONTEXT_UNAVAILABLE es exclusivo del servidor y se rechaza si lo envía el modelo. El esquema individual admite los dos campos nullable; la exclusividad la aplica Java.

La API mantiene HTTP 200 para una aclaración válida y devuelve analysis null, clarification y un token de continuación (salvo contexto perdido). No hay veredicto ni fuentes de un supuesto análisis. Las respuestas con análisis incluyen clarification null. El frontend conserva las aclaraciones en el historial, las muestra sin alerta técnica y permite contestar desde el campo de seguimiento, también tras recargar. Los informes antiguos siguen siendo compatibles.

Se añadieron instrucciones en docs/openai/political-v3-context.draft.md, expresamente no publicadas. Indican preservar el periodo al cambiar solo de territorio, pedir periodo ante «¿y después?», usar la respuesta a la aclaración y reemplazar el objetivo en un cambio claro de tema. La decisión semántica de aclarar sigue correspondiendo al proveedor; no se ha probado todavía con el modelo real ni se garantiza por estas pruebas.

Verificación: 253 pruebas Java correctas, sin errores, fallos u omisiones, incluyendo navegador con API Java real y upstream simulado; npm run build correcto; 39 comprobaciones locales de esquemas correctas. Se prueban análisis → aclaración → respuesta, recarga sin reenvíos, referencias, pérdida de contexto, aislamiento por sesión, límite de capacidad, expiración, cambio explícito de territorio, cambio de tema y rechazo de respuestas ambiguas de contrato. Las pruebas de contenido utilizan decisiones prefijadas; acreditan el transporte y las reglas, no la comprensión del modelo.

Capturas: POC/webapp/review/integration/clarification-1536.png y clarification-390.png. No se ejecutó ninguna inferencia de pago ni se publicaron cambios remotos. La siguiente fase debe componer y revisar las instrucciones completas y el esquema v3 antes de activarlo; el timeout permanece como trabajo separado.


### Trabajos asíncronos — fase 2

Se añaden AnalysisJobsTest (20 casos con H2 en fichero), OpenAiJobProviderTest (5 casos
contra HTTP local) y AnalysisJobsApiTest (3 casos de la API). Cubren recuperación, replay,
leases, contexto, retención, fallos transaccionales y aislamiento de credenciales.
Los detalles y límites de estas pruebas están en la [guía asíncrona](../POC/cross-check-service/docs/async-analysis.md).
No demuestran cobertura de todos los fallos de OpenAI ni semántica de resultados. No se
ejecutó inferencia real, polling del frontend ni apagado físico del equipo en esta fase.


### Frontend asíncrono — fase 3 (29/09/2026)

| Casos comprobados | Resultado y alcance |
|---|---|
| Cola → análisis → resultado, recarga, seguimiento y aclaración | Dos tamaños: 1536 y 390 px; contrato interceptado |
| Respuesta 202 perdida y recarga | Se repiten clave, credencial y cuerpo exactos |
| Desconexión y GET 503 | Se recupera el mismo trabajo sin otro POST |
| localStorage sin espacio | No se envía ninguna solicitud |
| 410 / 404 / 401 / 409 | Cuatro casos; terminan el seguimiento sin reenvío automático |
| Trabajo FAILED e informe inválido | Sin inferencia automática; recuperación explícita con la misma clave para contrato inválido |
| Solicitud antigua sin confirmación | No se repite pasadas 24 h |
| Dos pestañas y borrado local | Acceso compartido; borrar detiene recuperación sin cancelar el remoto |
| Dos seguimientos simultáneos | Se conserva un único nuevo trabajo por conversación |
| Demora superior a 90 s y pestaña oculta | Aviso sin fallo; pausa y reanudación del polling |
| Timeout HTTP de 15 s | Recuperación con la misma clave, sin crear otra identidad |
| Navegador → Vite → Spring Boot dev | Worker real, respuesta simulada, seguimiento con token cifrado y recarga |

17 escenarios correctos en `POC/webapp/scripts/verify-async.cjs`, sin inferencias de
pago. Compilación frontend correcta. La suite Java habitual pasó con 279 pruebas y
2 omisiones optativas; ambas regresiones de navegador se ejecutaron aparte, incluyendo
la corrección y repetición del mensaje de rechazo temporal. También se comprobaron
las ocho valoraciones y la trazabilidad con los scripts focalizados actualizados.

Las intercepciones no prueban todos los fallos de una red real. La espera larga usa la
fecha local guardada, no una inferencia prolongada; el plazo HTTP sí se espera. La
validación del nuevo transporte contra OpenAI real sigue pendiente, igual que los
límites semánticos recogidos antes. Las pruebas históricas del transporte síncrono
`verify.cjs` y `verify-timeouts.cjs` no se usan como evidencia del frontend actual.


### Arranque real de sesiones asíncronas — 30/09/2026

La documentación oficial y la prueba real corrigen una limitación del mock anterior:
las sesiones sin entorno requieren input inicial. OpenAiJobProviderTest tiene ahora
6 casos; comprueba creación con input, seguimientos por events y recuperación por
metadata/input sin otra creación. AnalysisJobsTest pasa de 20 a 24 casos: añade
checkpoint anterior a la creación, pérdida de respuesta con reinicio, sesión recuperada
sin turno visible y rechazo confirmado sin reserva de capacidad.

mvn verify: 284 casos registrados, 282 superados y 2 optativos omitidos. Después de añadir
los últimos dos casos, ejecución focalizada de AnalysisJobsTest: 24/24 correctos.
No se volvió a ejecutar toda la suite por cambios exclusivamente en esas pruebas.

Prueba real: un POST de navegador, 16 GET, una sesión y un turno remoto. Informe mostrado
en unos 65 s con recarga en RUNNING y conservación tras una recarga final. Cinco fuentes,
una afirmación y valoración declarada INSUFFICIENT_EVIDENCE; no constituye verificación
factual. No demuestra duración real superior a 90 s ni caída real de Java durante inferencia.
[Informe completo](../POC/cross-check-service/docs/openai/async-live-review.md).


## Candidata political-v10: fuentes y síntesis (30/09/2026)

- Preparación local del agente y comparación exacta del JSON generado con la plantilla y las instrucciones: correctas.
- Validación offline con jsonschema 4.26.0: 36 comprobaciones existentes y 3 adicionales de respuesta v3 superadas; plantilla y contratos coincidentes.
- [SR-01 a SR-16: evaluación semántica](../POC/cross-check-service/docs/openai/political-v10-source-review-cases.md): NOT_RUN. Son escenarios sintéticos y criterios de revisión, no pruebas automáticas ejecutadas ni resultados del modelo.
- No se ha publicado la candidata, cambiado la revisión activa o realizado inferencia de pago. La mejora de exactitud y posicionamiento queda pendiente de evaluar.


## Publicación y prueba de political-v10 (30/09/2026)

Political-v10 ya está publicada; la configuración remota y las instrucciones de la sesión ejecutada coinciden con la versión local. El perfil, ejemplo y configuración local utilizan esa revisión. La única consulta real autorizada terminó en INVALID_ANALYSIS_OUTPUT: el agente reformuló el anclaje que debía copiar literalmente. El rechazo se reprodujo offline. También se detectaron referencias de fuentes cruzadas, dos fechas erróneas y una clasificación de postura no suficientemente justificada. No se considera resuelta la calidad de fuentes.

OpenAiApiTest: 3 pruebas superadas y 2 opcionales omitidas. Los 16 casos sintéticos siguen pendientes. No hubo segunda inferencia ni relajación de validadores. [Ejecución, evidencia y siguiente fase propuesta](../POC/cross-check-service/docs/openai/political-v10-live-review.md).


## Expediente de evidencia: contrato offline implementado

Se incorpora el contrato interno v1 EvidenceDossier y la proyección de fuentes/referencias EvidenceAssembler. Valida anclajes literales, identidad de referencias, coincidencia de fragmentos con capturas, precisión y revisión de fechas. La suite de dominio supera 56 pruebas, incluidas 15 nuevas, sin omisiones. EV01 tiene regresión automática; EV02 tiene comprobaciones estructurales; EV03/EV04 utilizan metadatos de prueba revisados. EV05 continúa pendiente de revisión semántica.

No hay adquisición de documentos, persistencia del expediente ni integración con OpenAI todavía. El frontend, contrato público v3 y agente political-v10 no cambian. [Implementación, garantías y límites](../POC/cross-check-service/docs/evidence-dossier.md).
