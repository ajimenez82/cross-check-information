# Propuesta de valoración por afirmaciones

Estado: diseño para revisión, 28/09/2026. No implementado en Java ni publicado en OpenAI. El agente activo conserva political-v8 y el contrato v2. Esta fase no modifica la UI, los ocho resultados finales, las fechas ni el timeout.

## Problema y cambio propuesto

En la prueba de political-v8, el proveedor eligió NO_SINGLE_VERDICT para una sola proposición sobre oferta de alquiler, justificándolo por discrepancias entre estudios y falta de evidencia nacional homogénea. El código era válido, pero su uso incumplía las instrucciones.

Se propone que el proveedor valore cada afirmación por separado y que Java derive el código documental global. NO_SINGLE_VERDICT y los dos resultados derivados del posicionamiento de publicaciones dejan de ser opciones del proveedor. Una explicación de desacuerdo no se convierte automáticamente en evidencia insuficiente: el modelo debe valorar la calidad y pertinencia de las pruebas, pues algunas controversias sí permiten una conclusión respaldada o refutada.

## Contrato propuesto

[Esquema del fragmento](proposals/claim-assessment.schema.json) y [casos de revisión](proposals/claim-assessment.examples.json). proposalVersion identifica este borrador, no una versión activa de OpenAI, del token ni del informe completo.

| Campo | Función |
|---|---|
| analysisTarget | Proposición sustantiva a contrastar, preservando contexto, periodo, alcance y matices de la consulta. |
| decomposition.kind | SINGLE o MULTIPLE. |
| decomposition.basis | SINGLE_PROPOSITION, EXPLICIT_CONJUNCTION o EXPLICIT_DIMENSIONS. El desacuerdo entre fuentes no es una base admitida. |
| decomposition.explanation | Explicación breve de la separación, visible para auditoría; no se solicita razonamiento interno. |
| claims[].id | Identificador único de afirmación. |
| claims[].inputExcerpt | Fragmento literal de la entrada que permite localizar la afirmación. |
| claims[].proposition | Afirmación completa y evaluable, manteniendo sujetos y calificadores compartidos. |
| claims[].verdict | status, explanation, documentarySupport y sourceIds por afirmación. |

Los estados documentales por afirmación son SUPPORTED, REFUTED, MISLEADING, INSUFFICIENT_EVIDENCE y OPINION. Este fragmento sustituiría la decisión global del proveedor dentro de un futuro contrato completo; no se deben devolver dos veredictos globales independientes. El catálogo de fuentes sigue fuera del fragmento y se valida contra las referencias de cada afirmación.

## Política de agregación para Java

| Situación validada | Resultado documental global |
|---|---|
| Una afirmación | Su estado documental. Nunca NO_SINGLE_VERDICT. |
| Varias afirmaciones, mismo estado | Ese estado, conservando explicación y evidencia de cada una. |
| Varias afirmaciones, estados distintos | NO_SINGLE_VERDICT, mostrando qué conclusión corresponde a cada afirmación. |
| Varias afirmaciones, todas insuficientes | INSUFFICIENT_EVIDENCE. |
| Descomposición inválida o referencias incoherentes | Rechazo técnico; no cambio silencioso a evidencia insuficiente. |

Ejemplo válido: «La medida aumentó la frecuencia y redujo el precio». Si la primera afirmación está respaldada y la segunda refutada, corresponde NO_SINGLE_VERDICT. Si ambas están respaldadas, corresponde SUPPORTED. Si una está respaldada y la otra no tiene evidencia suficiente, se preservan ambas conclusiones bajo NO_SINGLE_VERDICT; esta es una decisión explícita de la propuesta para revisar.

El ejemplo del alquiler conserva una única afirmación causal. La coexistencia de estudios favorables y contrarios no crea dos afirmaciones del usuario. Tampoco se deben convertir las distintas fuentes o territorios investigados en nuevas preguntas que el usuario no planteó.

## Posicionamiento de publicaciones

Para SINGLE con resultado documental insuficiente se conserva la regla actual: derivar SUPPORTED_BY_PUBLICATIONS o QUESTIONED_BY_PUBLICATIONS únicamente si existe una pluralidad única en la muestra válida sobre esa misma proposición. Empates, predominio mixto o ausencia de posición mantienen INSUFFICIENT_EVIDENCE. El reparto no prueba la verdad factual.

Para MULTIPLE se propone mantener inicialmente el resultado documental agregado, sin mezclar votos sobre afirmaciones diferentes ni derivar un resultado editorial global. Una ampliación posterior podría asociar evaluaciones a claimId, con desglose por afirmación. Esta decisión necesita revisión antes de implementar el contrato completo; el fragmento actual no resuelve ese mapeo. La posición visual del panel se mantiene.

## Comprobaciones locales y límites

El prototipo exige SINGLE con una afirmación y MULTIPLE con al menos dos (máximo ocho en este borrador). Comprueba IDs únicos, proposiciones no duplicadas literalmente tras normalizar espacios y mayúsculas, referencias conocidas y fragmentos presentes, únicos y no solapados en la entrada original. La entrada y el catálogo se pasan desde fuera del resultado del proveedor.

Estas comprobaciones rechazan fragmentos inventados, la repetición de toda la pregunta para crear dos afirmaciones y la base SOURCE_DISAGREEMENT. No detectan paráfrasis duplicadas, divisiones artificiales bien redactadas ni cambios de sentido en analysisTarget o proposition. Un anclaje literal es una ayuda de auditoría, no una prueba de independencia semántica.

El criterio de fragmentos no solapados es conservador: puede rechazar consultas legítimas con elipsis, repeticiones o sujetos compartidos difíciles de localizar. Antes de producción hay que acordar cómo pedir aclaración o representar esos casos. Para seguimientos, tampoco basta con anclar al último mensaje: habrá que definir una entrada contextual verificable usando la conversación. No se inventará esa entrada a partir de la salida del modelo.

Los estados iguales no garantizan explicaciones homogéneas; la UI y el resumen deben conservar las conclusiones individuales. El modelo sigue siendo responsable de interpretar evidencia. El prototipo no evalúa fuentes, fechas, veracidad ni calidad de redacción.

## Pruebas realizadas

[verify-claim-proposal.py](../POC/cross-check-service/scripts/verify-claim-proposal.py) es una política ejecutable de diseño, fuera de la aplicación. Requiere Python y jsonschema 4.26.0. Se ejecutó usando el validador previamente instalado en bootstrap/target/schema-validation-deps mediante PYTHONPATH; no se añade una dependencia al servicio Java.

Resultado: 18 casos correctos. Incluyen una afirmación con fuentes discrepantes, dimensiones con estados distintos, estados iguales, insuficiencia conjunta, combinación respaldada/insuficiente, códigos no permitidos del proveedor, cardinalidad, duplicados, fragmentos inventados o solapados y referencias inexistentes.

El caso v8_observed_verdict_on_single_claim conserva el objeto verdict real de la respuesta tardía y lo inserta en un envoltorio de una afirmación para comprobar que el nuevo fragmento rechaza NO_SINGLE_VERDICT. No es una conversión válida de todo el informe real ni una prueba de integración. El caso de insuficiencia es sintético; no se corrigió la respuesta real para hacerla pasar.

## Siguiente fase, pendiente de aprobación

Revisar especialmente la agregación de estados diferentes, el tratamiento de publicaciones con varias afirmaciones y las limitaciones del anclaje. Después preparar el contrato completo, implementar DTOs y política en Java, conservar el detalle por afirmación en API/UI y probar con proveedor simulado. La migración del agente y otra inferencia real serían pasos posteriores. No se ha modificado el agente, su revisión local ni la configuración activa en esta fase.

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


### Preparación local de las instrucciones completas v3

La definición completa está preparada en `POC/cross-check-service/docs/openai/political-analysis-instructions-v3.md`
y `political-agent-v3.template.json` en esa misma carpeta. Mantiene el nombre del agente
`cross-check-political-v1`, el modelo `gpt-5.4-mini` y sus ajustes actuales. El nombre del
agente no identifica la versión del contrato.

Las instrucciones separan aclaración y análisis, preservan el contexto explícito del servidor,
exigen anclajes literales y veredictos por afirmación, y dejan la agregación global a Java.
Para varias afirmaciones no se combina el posicionamiento de publicaciones en una distribución.
Se conservan las reglas de fechas, fuentes, atribución y exclusión de publicaciones.

Preparación local desde la carpeta del servicio:

```powershell
./scripts/prepare-openai-agent.ps1 -Model gpt-5.4-mini -SchemaVersion 3
```

Genera `bootstrap/target/political-agent-v3.request.json`; no lee credenciales ni publica.
Omitir `-SchemaVersion` conserva la preparación v2. El borrador de contexto anterior se
mantiene como historial y no debe publicarse por separado.

Esta fase no activa v3 ni modifica el agente remoto. Antes de publicar, revisar la descomposición,
las preguntas de aclaración y las restricciones de posicionamiento. La validación local del
esquema no demuestra aceptación por OpenAI ni precisión semántica del modelo; la exclusividad
entre análisis y aclaración se comprueba también en Java. Publicación, cambio coordinado de
configuración/revisión y prueba real de pago quedan para fases posteriores.


### Publicación y activación de v3 — 29/09/2026

Estado vigente: la definición v3 se ha publicado en el agente existente y se ha
verificado mediante una lectura posterior de OpenAI. Las instrucciones y `text.format`
coinciden con `political-analysis-instructions-v3.md` y `political-agent-v3.template.json`.
El nombre continúa siendo `cross-check-political-v1`; se conservan `gpt-5.4-mini`,
razonamiento `low`, búsqueda web `live` y el nivel de servicio `default`.

La configuración local, el ejemplo y los valores predeterminados del perfil `openai`
usan ahora `OPENAI_REPORT_SCHEMA_VERSION: 3` y `POLITICAL_AGENT_REVISION: political-v9`.
La revisión identifica compatibilidad de conversaciones, no una versión seleccionable
remota. Las conversaciones anteriores deben sustituirse por una conversación nueva.
Si el servicio estaba arrancado, hay que reiniciarlo para cargar esta configuración;
si estaba parado, basta arrancarlo con los perfiles `openai,local`. Cualquier variable
de entorno o argumento explícito antiguo debe retirarse o actualizarse porque puede
prevalecer sobre el fichero local. El perfil `dev` sigue usando el proveedor simulado.

`prepare-openai-agent.ps1 -Model gpt-5.4-mini` prepara ahora v3 por defecto y genera
`bootstrap/target/political-agent-v3.request.json`. Para preparar expresamente la
versión histórica v2, usar `-SchemaVersion 2`; los archivos v2 se conservan para revisión
y compatibilidad. El script de preparación sigue sin publicar ni leer credenciales.

La publicación no ejecutó sesiones, turnos ni consultas de análisis. La aceptación del
esquema por OpenAI no demuestra la calidad de los resultados: queda pendiente una
prueba real controlada, autorizada por separado, para revisar descomposición, contexto,
fuentes, fechas, posicionamiento y tiempo de respuesta. No se da por resuelto el timeout.

Evidencia local de la operación, dentro de la carpeta ignorada `bootstrap/target`:
`agent-before-political-v9.json` conserva la definición anterior y `agent-v3-readback.json`
la lectura verificada. Una vuelta a v2 exige restaurar conjuntamente definición remota,
contrato local y revisión de conversación; cambiar solo un número no restaura el agente.
