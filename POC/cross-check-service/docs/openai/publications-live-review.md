# Prueba de posicionamiento editorial — 25/09/2026

Consulta: «¿Los límites al precio del alquiler reducen la oferta de vivienda en alquiler en España? Analiza el periodo 2023–2025 y contrasta publicaciones con distintas posiciones».

Se realizó una única consulta real desde el frontend, sin reintentos. OpenAI completó el turno. Java rechazó su salida con `INVALID_ANALYSIS_OUTPUT`, HTTP 502, a los 48,257 segundos observados por el navegador. Request ID: `48d45445-45ed-47c6-a599-937b90fb80d4`. No fue un timeout.

## Resultado de la revisión

- **Error de formato:** la fuente S4 contiene `publishedAt: "2025-?"`. No es una fecha ISO válida para `LocalDate`. Debía devolver una fecha completa comprobada o `null`. La página de la UPC muestra 25/11/2025: https://www.upc.edu/es/sala-de-prensa/noticias/cinco-tesis-sobre-politica-actual-vivienda-en-espana-cpsv.
- **Respaldo documental:** contiene dos frases explicativas en español, coherentes con evidencia insuficiente; ya no repite el código técnico.
- **Idioma:** el contexto comienza con «We assess whether». Incumple la exigencia de texto visible íntegramente en español.
- **Periodo:** el informe fija `asOf` en 2025-12-31 y periodo de publicaciones en 2023–2025, pero incluye S1 con publicación en 2026. Una fuente posterior puede estudiar el periodo solicitado, pero no debe confundirse periodo estudiado, fecha de publicación y corte de evidencias. Esta combinación no es coherente tal como se presenta.
- **Posiciones propuestas, no validadas:** 2 SUPPORTS, 2 QUESTIONS, 1 NO_EXPLICIT_POSITION, 0 MIXED. No se repiten IDs entre unidades. El empate conservaría INSUFFICIENT_EVIDENCE si el informe fuera válido; no se debe presentar como un cálculo final de la API, que rechazó la salida.
- **Calidad de muestra pendiente:** revisar si cada unidad expresa una postura propia sobre la misma proposición y periodo, frente a resumir un estudio anterior o una intervención ajena. La ausencia de IDs repetidos no demuestra independencia de investigaciones o ausencia de reproducciones semánticas.

No se corrigió la respuesta a mano ni se relajó la validación para mostrarla. No se validó la conclusión política como resultado definitivo. El frontend mostró el error técnico y no una valoración factual.

## Consumo y artefactos

El turno remoto registró 208.555 tokens de entrada, de los que 174.592 estaban en caché, y 3.861 de salida. No se ha comprobado el importe facturado con herramientas.

En `bootstrap/target` (excluido de Git): `publications-live.log`, `publications-live-results.json`, `publications-recovered.json` y `publications-live-error.png`. La recuperación consultó la salida guardada sin una nueva inferencia. No se guardaron claves ni tokens de conversación en estos artefactos.

## Corrección aplicada

Las instrucciones distinguen periodo estudiado, fecha de publicación y corte de evidencias. Una publicación posterior puede estudiar retrospectivamente el periodo solicitado, pero debe explicitarse y no puede presentarse como conocida antes de su publicación. El periodo de la muestra corresponde a las fechas de sus publicaciones; si sus límites se desconocen, se devuelve `null`.

El esquema exige el formato completo `YYYY-MM-DD` para fechas conocidas; las instrucciones prohíben completar fechas parciales por suposición. Java mantiene la validación de fechas reales. Se sustituyó el ejemplo de apertura inglés por uno español y se reforzaron las reglas de elegibilidad, alcance y agrupación de reproducciones.

Se actualizaron las instrucciones y el esquema del agente remoto y se verificó su lectura posterior, conservando modelo, herramientas, razonamiento y nivel de servicio. La revisión local es `political-v4`; requiere reiniciar el servicio y comenzar una conversación nueva.

Validación: `mvn -q verify`, 169 pruebas sin fallos, errores ni omisiones. Incluye rechazo de cinco fechas incompletas o imposibles y aceptación de fecha desconocida como `null`. No se ejecutó una nueva inferencia de pago. La eficacia de las instrucciones sobre una respuesta real y la calidad de la muestra siguen pendientes de la próxima prueba autorizada.

## Repetición con political-v4 — 25/09/2026

Una única consulta real con el mismo texto, conversación nueva y sin reintentos. HTTP 200 en 61,729 segundos en el navegador (61,395 segundos en Java). Request ID: `2ba9b597-0517-4721-a020-17dfa4c906cd`. OpenAI completó el turno, Java validó el informe y el frontend mostró el resultado. No hubo timeout ni eventos de fallo de red registrados.

El agente devolvió `MISLEADING`; se mostraron cuatro unidades: 2 SUPPORTS (50 %), 1 QUESTIONS (25 %) y 1 MIXED (25 %). La aritmética y la presentación funcionaron. Este reparto no valida la clasificación semántica y esta prueba no ejercita la derivación desde INSUFFICIENT_EVIDENCE a los dos veredictos basados en publicaciones.

### Correcciones comprobadas y problemas pendientes

- No reaparecieron fechas parciales o imposibles. Cuatro fuentes tienen publishedAt null y una 2024-10-13; asOf y el periodo de la muestra son null. Esto evita el fallo de deserialización, pero no demuestra una extracción de fechas suficiente.
- El contexto comienza en español. Persiste «España-wide» en la explicación del veredicto; el requisito de idioma no queda completamente validado.
- selectionCriteria afirma que se seleccionaron publicaciones de 2024–2025. La publicación del Sindicat lleva fecha visible 16/03/2026. Puede aportar una evaluación retrospectiva, pero debe declararse correctamente esa fecha y su alcance. Fuente comprobada: https://sindicatdellogateres.org/es/informe-sobre-los-efectos-de-la-regulacion-de-alquileres-dos-anos-desde-la-entrada-en-vigor/.
- La unidad de EL PAÍS se clasifica QUESTIONS afirmando que niega evidencia de reducción de oferta. El texto consultado habla de falta de evidencia sobre reducción de picos de precios, una proposición distinta. Esta atribución no queda respaldada por el artículo y afecta al reparto. Fuente comprobada: https://elpais.com/economia/vivienda/2024-10-13/vivienda-e-intervencion-publica-razones-y-limitaciones.html.
- La clasificación MIXED del Sindicat requiere revisar la diferencia entre oferta total y desplazamiento al alquiler temporal; no se considera validada por contener ambos conceptos.
- MISLEADING se justifica principalmente por insuficiencia causal y dificultad para extrapolar. Debe revisarse frente a INSUFFICIENT_EVIDENCE: la falta de prueba general no demuestra por sí sola una afirmación engañosa. No se cambió manualmente el veredicto ni se validó una conclusión política.
- Se muestran identificadores de búsqueda como turn5view0 en las referencias. Son consistentes dentro del JSON, pero conviene normalizarlos a etiquetas de fuentes legibles.

### Cierre de la comprobación

La conexión completa funciona en esta ejecución; la calidad semántica del agente sigue pendiente. No se modificó de nuevo el agente ni se ejecutaron consultas adicionales. Próximo paso propuesto: precisar el criterio de MISLEADING, exigir correspondencia entre la proposición y el argumento citado, y revisar fechas y etiquetas de fuentes mediante casos locales antes de otra prueba de pago.

Artefactos en bootstrap/target, excluidos de Git: publications-v4-live.log, publications-v4-live-results.json y publications-v4-live-0.png. El resultado guardado omite el token de conversación. No se ha comprobado el coste facturado.
## Ajuste posterior: political-v5

Se precisaron los criterios de MISLEADING frente a INSUFFICIENT_EVIDENCE, la correspondencia entre variable y argumento de cada publicación, MIXED frente a simples matices, fechas retrospectivas, idioma y etiquetas de fuentes S1, S2, etc. Se añadió political-regression-cases.md con diez casos sintéticos y resultados esperados para revisión; no son respuestas del modelo ni una evaluación semántica automatizada.

La definición JSON preparada coincide con las instrucciones locales. Las instrucciones del agente remoto se actualizaron y verificaron por lectura posterior, conservando modelo, esquema, herramientas, razonamiento y nivel de servicio. Revisión local political-v5: reiniciar el servicio y usar una conversación nueva.

mvn -q verify terminó correctamente: 169 pruebas, sin fallos, errores ni omisiones. No se añadieron pruebas que simulen certificar la semántica de un prompt. No se ejecutó ninguna inferencia adicional; queda pendiente una prueba real autorizada. No se modificó el resultado previo para hacerlo pasar.
## Prueba real con political-v5 — 25/09/2026

Una consulta real con el mismo texto y conversación nueva, sin reintentos. HTTP 200 en 43,720 segundos en el navegador y 43,356 segundos en Java. Request ID: c177605e-8ab3-4e81-b30e-aa4590a7b27f. El frontend renderizó el resultado y no se registraron errores de red.

La respuesta final fue SUPPORTED_BY_PUBLICATIONS: cuatro unidades SUPPORTS (80 %) y una QUESTIONS (20 %). Se ejercitó la derivación del veredicto por Java desde evidencia insuficiente, con la advertencia de que la muestra no establece la verdad factual. El cálculo corresponde a las unidades recibidas, cuya elegibilidad sigue requiriendo revisión.

Mejoras observadas: referencias S1–S7 consistentes; prosa explicativa en español (con una errata «relajAR»); ausencia de MISLEADING por mera falta de prueba nacional; fuentes jurídicas y administrativas fuera del recuento. No hubo fechas sintácticamente inválidas ni timeout.

### Problemas que impiden cerrar la calidad

- Inconsistencia interna: el periodo de publicaciones va de 2024-02-29 a 2026-06-09, pero incluye S3 con publishedAt 2024-01-01 y S4 con 2022-11-01. El backend admite actualmente esta contradicción entre campos.
- Fecha de S4: el registro de UCL identifica el artículo como de 2023, no respalda la fecha 2022-11-01 usada. Su resumen sí describe ausencia de reducción en contratos y stock bajo la regulación de 2020; usarlo como antecedente no equivale a medir 2023–2025. Fuente: https://discovery.ucl.ac.uk/id/eprint/10181899/.
- Fecha de S6: el BOE indica publicación el 25/05/2023; el agente usó el 24/05/2023, fecha de la ley. Fuente: https://www.boe.es/buscar/doc.php?id=BOE-A-2023-12203.
- La fecha de S3 (01/01/2024) no se ha verificado: el acceso independiente al FMI devolvió HTTP 403. No se considera validada ni se infiere que el agente tuviera el mismo bloqueo.
- Aunque reconoce antecedentes y retrospectivas, la limitación «evidencia accesible entre 2023 y 2025» no describe correctamente la inclusión de un informe de 2026. También debe distinguir con más claridad recomendaciones o riesgos generales de efectos observados en el periodo solicitado.
- La postura de Cinco Días es compatible con SUPPORTS, pero una opinión no establece causalidad. Se revisó el texto de https://cincodias.elpais.com/opinion/2024-09-19/los-problemas-crecen-y-las-soluciones-urgen-en-el-mercado-de-la-vivienda.html. No se certificó exhaustivamente cada fuente ni la independencia de todos los estudios.

### Próximo paso propuesto

Añadir validación determinista en Java para que el periodo de la muestra incluya las fechas conocidas de sus unidades y el corte de evidencias no anteceda a las fuentes utilizadas. Esa validación detecta contradicciones internas, pero no prueba que una fecha coincida con su página. Mantener separada la verificación documental de fechas y la revisión semántica de alcance; no inventar correcciones ni reintentar automáticamente consultas de pago.

No se modificaron de nuevo las instrucciones ni el resultado recibido. Se detuvieron los servidores temporales. Artefactos locales excluidos de Git: publications-v5-live.log, publications-v5-live-results.json y publications-v5-live-0.png. No contienen el token de conversación; el coste facturado no se ha comprobado.
## Validación temporal determinista aplicada

Se incorporaron controles en AnalysisReport para fechas conocidas fuera del periodo de las unidades clasificadas y fuentes utilizadas posteriores a asOf. La contradicción interna de la muestra de political-v5 ahora queda cubierta por el rechazo de fechas fuera de sus límites. Se admiten límites inclusivos, fechas o periodos desconocidos y fuentes meramente excluidas posteriores al corte; una exclusión editorial no exime a una fuente citada como evidencia.

Validación local: mvn -q verify, 177 pruebas sin fallos ni errores. Ocho ejecuciones adicionales cubren ambos extremos del periodo, reproducciones agrupadas, límites inclusivos, null, fuentes documentales/excluidas y rechazo del adaptador sin reintento. No se ejecutaron inferencias de pago ni se modificó el agente remoto: continúa political-v5. Los dos documentos de Docs recogen el comportamiento y sus límites. La autenticidad de fechas y la semántica siguen requiriendo revisión de fuentes.
## Verificación del error temporal en frontend — 26/09/2026

Prueba integrada sin OpenAI: navegador real → API Java de prueba → adaptador OpenAI → servidor HTTP local que simula el proveedor. La respuesta simulada contiene una fuente publicada el 09/06/2026 y un corte de evidencias 31/12/2025. Se usa el validador real, no un error inyectado directamente en la interfaz.

Resultado: HTTP 502 INVALID_ANALYSIS_OUTPUT con referencia de diagnóstico coherente entre cabecera y cuerpo; sin analysis ni conversationToken. La interfaz muestra «El servicio ha devuelto un resultado que no se puede mostrar», sin valoración ni porcentajes. Tras esperar, sigue habiendo un único envío. Al iniciar Nueva conversación y enviar otra consulta, responde HTTP 200 y se muestra la valoración. Exactamente dos envíos del navegador y dos ejecuciones del proveedor local; sin errores JavaScript. No se comprobó el botón de reintento manual en esta prueba.

mvn -q verify -Dbrowser.temporal=true: 179 pruebas, sin fallos, errores ni omisiones. El test de navegador requiere Vite en 127.0.0.1:5179 (o APP_URL), Node y PLAYWRIGHT_MODULE apuntando al módulo Playwright; en ejecuciones ordinarias de Maven ese test opcional se omite. Script: POC/webapp/scripts/verify-temporal-error.cjs. El transporte del navegador se redirige al puerto aleatorio del Java real de prueba; no se simula su respuesta HTTP.

Capturas y diagnóstico: POC/webapp/review/integration/temporal-error.png, temporal-recovery.png y temporal-diagnostics.json. No se guardan tokens en el diagnóstico. No fue necesario cambiar el comportamiento del frontend. El agente remoto permanece en political-v5; no hubo llamadas de pago.
## Prueba real con political-v6 — 26/09/2026

Una única consulta sobre alquileres 2023–2025, conversación nueva y sin reintentos. HTTP 200 en 76,315 segundos. Request ID: 06752c62-82bc-4436-900a-fa946591458f. El frontend mostró INSUFFICIENT_EVIDENCE y tres unidades: una SUPPORTS, una QUESTIONS y una MIXED. El empate conserva el veredicto documental. No hubo fallo de contrato ni timeout, pero la calidad no queda validada.

Problemas observados:

- S1 devuelve 2023-05-24 como publicación. El BOE enlazado indica publicación 25/05/2023, entrada en vigor 26/05/2023 y texto consolidado actualizado en 2026. Persiste la confusión entre fecha de la ley, publicación y versión: https://boe.es/buscar/act.php?id=BOE-A-2023-12203.
- S2 tiene publishedAt null, pero la muestra fija from=2023-02-01. Las instrucciones exigen period null cuando una fecha de fuente clasificada no está verificada; el límite inicial no queda justificado por los metadatos devueltos. Java solo exige que las fechas conocidas estén dentro de los límites: no comprueba esta condición más estricta ni que los extremos sean las fechas mínima y máxima.
- Todas las fuentes y la muestra llevan consultedAt=2026-09-26T00:00:00Z. No se ha verificado metadata de herramienta que sustente esas horas; no deben considerarse horas de consulta fiables. Las instrucciones ya prohíben inventar medianoche. No se cambió la respuesta recibida.
- S2 estudia la regulación catalana de 2020–2022 y el propio informe lo reconoce como antecedente, pero lo cuenta en la muestra del periodo solicitado. El PDF corresponde al estudio anterior: https://discovery.ucl.ac.uk/10181899/1/Martinez_final_manuscript_postprint_UB.pdf. No queda demostrada la elegibilidad de esa unidad para 2023–2025.
- S3 se clasifica SUPPORTS por la opinión del Círculo de Economía que recoge el artículo; la explicación no acredita una postura propia de la publicación. Fuente: https://elpais.com/economia/2024-06-17/el-circulo-de-economia-pide-no-aplicar-el-tope-a-los-alquileres-por-sus-efectos-muy-perniciosos-sobre-la-oferta.html.
- S4 queda MIXED por incluir voces distintas. Es necesario distinguir una postura propia mixta de una cobertura de opiniones ajenas; no se ha validado exhaustivamente su clasificación.

Aspectos conservados: referencias S1–S4, explicación en español, asOf null y advertencias sobre alcance. No bastan para certificar el análisis ni el reparto.

Conclusión de esta ejecución: integración operativa, evaluación de calidad no superada. No se cambió el agente, no se repitió la consulta y no se comprobó el importe facturado. Artefactos en bootstrap/target: publications-v6-live-results.json, publications-v6-live-0.png y publications-v6-live.log. Se detuvieron los servidores temporales.

Siguiente propuesta para revisión: dejar de resolver estos incumplimientos únicamente añadiendo instrucciones. Separar metadatos comprobables y clasificación semántica; decidir si el periodo debe calcularlo Java y si consultedAt solo puede provenir de registros verificables. Para la muestra, exigir justificación trazable de postura propia y alcance antes de contabilizar unidades. Son propuestas, no cambios implementados en esta fase.
## Metadatos controlados por el adaptador Java — 26/09/2026

Se implementó OpenAiReport como representación de entrada estricta antes del informe de dominio. Calcula el periodo con las fuentes clasificadas (null si alguna fecha falta o no hay unidades) y elimina consultedAt del modelo al no disponer de registros verificables de acceso. Conserva validaciones de referencias, duplicados, fechas de publicación y corte. El perfil dev conserva metadatos ilustrativos.

mvn -q verify: 185 casos registrados, 184 ejecutados correctamente y uno omitido (navegador opcional); cero fallos y errores. Se añadieron seis pruebas de cálculo, reproducciones, exclusiones, fechas desconocidas, límites iguales, muestra vacía, descarte de horas no verificables y preservación de rechazos. No se ejecutó una inferencia real ni se repitió la prueba opcional de navegador. No hay garantía nueva sobre fechas de publicación ni sobre la prosa generada.

La propuesta sobre posturas propias, terceros citados y alcance está en Docs/propuesta-trazabilidad-clasificacion.md, sin implementar cambios semánticos ni de contrato. El agente remoto sigue en political-v6.
### Activación del contrato v2 — 28/09/2026

Estado vigente: contrato v2 publicado en el agente remoto y configuración local sincronizada. Los apartados anteriores conservan el historial de las fases y no describen el estado actual cuando indican que el lector está pendiente de activación.

El adaptador selecciona explícitamente el lector mediante OPENAI_REPORT_SCHEMA_VERSION (1 o 2); el perfil openai tiene valor predeterminado 2. No existe degradación automática al contrato antiguo. La configuración local usa OPENAI_REPORT_SCHEMA_VERSION: 2 y POLITICAL_AGENT_REVISION: political-v7. Esta revisión controla la compatibilidad del token de conversación; no representa una versión de OpenAI. El nombre remoto sigue siendo cross-check-political-v1.

Se publicaron las instrucciones y el formato estructurado de political-agent.template.json. Una lectura posterior confirmó igualdad exacta de las instrucciones y de text.format. OpenAI devuelve además text.verbosity: medium como propiedad normalizada. El agente mantiene gpt-5.4-mini, reasoning low, web_search en modo live y service_tier default.

El servicio remoto rechazó uniqueItems en el esquema estricto. Se retiró únicamente esa palabra clave de la plantilla enviada; los borradores de Docs/proposals conservan la restricción de diseño y Java sigue rechazando duplicados. La publicación corregida fue aceptada. El proveedor entrega assessments; Java valida y transforma COUNT/EXCLUDE en unidades y exclusiones con trazabilidad. Las fechas de consulta no verificadas siguen siendo null y el periodo de la muestra se calcula en Java.

Validación previa a la publicación: mvn -q verify -Dbrowser.temporal=true, 214 pruebas sin fallos, errores ni omisiones; además se repitió OpenAiApiTest con comprobación del desglose y las exclusiones en el navegador. En esta activación se verificaron el esquema aceptado y su contenido remoto, sin ejecutar inferencias de pago.

Pendiente: una prueba real controlada, acordada como siguiente fase, para evaluar salida v2, latencia y calidad de las atribuciones y del alcance. La aceptación del esquema y las pruebas simuladas no acreditan la fidelidad semántica a las fuentes. Al arrancar el servicio con openai,local se debe comenzar una conversación nueva; los tokens de revisiones anteriores no son compatibles.

### Primera prueba real con contrato v2 — 28/09/2026

Se ejecutó una única consulta desde el frontend: «¿Los límites al precio del alquiler reducen la oferta de vivienda en alquiler en España? Analiza el periodo 2023–2025 y contrasta publicaciones con distintas posiciones». Servicio con openai,local; esquema 2; revisión local political-v7; modelo gpt-5.4-mini.

Resultado: HTTP 502, INVALID_ANALYSIS_OUTPUT, 86,247 segundos observados en el navegador. Request ID: b3194b1a-f3bd-4a9f-b77e-3128f1ed9ee3. El proveedor completó el turno y Java rechazó posteriormente el informe; no fue un timeout. La UI mostró el error y su referencia, sin presentar un veredicto ni porcentajes. El código público conserva executionState UNKNOWN, aunque en este diagnóstico los logs confirman que el turno remoto terminó.

Se recuperó la salida ya existente mediante GET, comprobando la hora de la sesión y la consulta, sin otra inferencia. La deserialización estricta pasó. Un diagnóstico Java local con la respuesta original reprodujo el rechazo y comprobó cada evaluación:
- A1 / S1 (EsadeEcPol): COUNT + SUPPORTS, pero scope.match PARTIAL y temporalRelation BACKGROUND. Incumple dos condiciones de elegibilidad.
- A2 / S2 (BBVA Research): supera las reglas estructurales de la evaluación individual; esto no verifica la fidelidad de sus afirmaciones.
- A3 / S4 (FEDEA): supera esas mismas reglas individuales; pendiente de contraste semántico.
- A4 / S3 (CaixaBank Research): COUNT + QUESTIONS con scope.match PARTIAL. Incumple la condición de alcance. Su propia explicación mezcla cuestionar la eficacia de una política con cuestionar la proposición sobre reducción de oferta, por lo que también requiere revisión de polaridad contra la fuente.

El agente declaró INSUFFICIENT_EVIDENCE en el JSON, pero ese informe fue rechazado: no es un resultado aceptado por la aplicación. No se eliminan evaluaciones ni se recalculan porcentajes para hacer pasar la prueba. No se ha completado la revisión externa de las siete fuentes ni de sus fechas.

Consumo informado por la sesión, no importe facturado: 362493 tokens de entrada (270848 en caché), 6688 de salida (2995 de razonamiento), 369181 totales. Los elementos recuperados contienen 11 llamadas web_search_call. Una sola consulta de la aplicación puede implicar múltiples búsquedas y pasos internos. No se ha calculado su coste monetario.

Conclusión: prueba real no superada; el control de coherencia del backend sí detectó las contradicciones que motivaron la trazabilidad. Artefactos locales ignorados en bootstrap/target: publications-v7-live-results.json, publications-v7-live-error.png, publications-v7-live.log, publications-v7-report.json y publications-v7-usage.json. No se repitió la inferencia ni se cambiaron las instrucciones o las reglas de aceptación en esta fase.

Siguiente fase propuesta: revisar la representación de elegibilidad para reducir decisiones contradictorias (COUNT junto a PARTIAL/BACKGROUND), reproducir este caso sin conexión como regresión y estudiar un límite de trabajo de búsqueda. Mantener el rechazo de incoherencias y acordar el cambio antes de otra publicación o prueba de pago.

### Separación de relevancia temporal y fechas de publicación — 28/09/2026

Estado vigente: instrucciones y esquema reforzados publicados y verificados mediante lectura remota. Contrato JSON schemaVersion 2; revisión local de conversación political-v8; nombre remoto cross-check-political-v1. Se conservan el modelo, las herramientas y los parámetros de ejecución.

El periodo estudiado determina la pertinencia del contenido. publishedAt es un metadato independiente: puede ser null sin invalidar una publicación que estudia el contexto solicitado. Un estudio publicado en 2026 sobre 2023–2025 puede ser retrospectivo; una publicación de 2023 sobre 2020–2022 es un antecedente. La ventana calculada por Java describe fechas de publicación de la muestra, no el periodo de los hechos; queda null si alguna fecha de publicación es desconocida.

asOf solo limita la información disponible cuando se establece un corte histórico. Java rechaza evidencia con fecha conocida posterior al corte. Con fechas desconocidas, la comprobación de disponibilidad histórica continúa siendo responsabilidad del agente: las instrucciones exigen excluir material cuya disponibilidad anterior al corte no se pueda establecer. Java no verifica páginas ni deduce fechas ausentes.

La plantilla y los dos esquemas de Docs/proposals separan cada evaluación mediante anyOf en COUNT y EXCLUDE. COUNT exige MATCH, REQUESTED_PERIOD o RETROSPECTIVE, postura no nula y titular distinto de UNDETERMINED. EXCLUDE exige postura null y conserva la trazabilidad y explicación. Así, COUNT no admite simultáneamente PARTIAL o BACKGROUND. Las reglas de Java permanecen activas; no se eliminan unidades ni se cambian decisiones silenciosamente para aceptar un informe.

Las instrucciones exigen evaluar el alcance antes de decidir y prohíben cambiar etiquetas solo para encajar en COUNT. El esquema restringe combinaciones, pero no demuestra que MATCH, las fechas o las atribuciones sean verdaderas; todavía requiere evaluación con fuentes reales.

Verificación: 27 pruebas de OpenAiTracedReportTest correctas (cinco casos nuevos), más 33 comprobaciones locales de JSON Schema. La respuesta real rechazada de political-v7 se conserva como fixture de regresión; se comprueba que sigue fallando, sin convertirla en un resultado válido. También se prueban fecha desconocida, retrospectiva posterior al periodo, corte histórico y exclusiones explícitas. La API OpenAI aceptó el nuevo esquema y la lectura posterior coincidió con las instrucciones y text.format locales.

El script scripts/verify-agent-schema.py requiere Python y jsonschema 4.26.0. Para esta verificación se instaló el validador únicamente en bootstrap/target/schema-validation-deps y se añadió esa carpeta a PYTHONPATH. No es una dependencia del servicio Java.

No se ejecutaron inferencias de pago en esta fase. Pendiente: una nueva prueba real acordada y la revisión separada del consumo de búsquedas. Al arrancar el servicio, usar una conversación nueva por el cambio de revisión.

### Prueba real de political-v8 — 28/09/2026

Una única consulta desde el frontend, con la misma pregunta sobre límites al alquiler en España durante 2023–2025. Request ID: 3ad5f908-db61-457c-957d-9570923cbdbe. La API devolvió HTTP 504 / ANALYSIS_TIMEOUT a los 90,561 segundos; la UI mostró el error y no reintentó. No se entregó análisis ni token de continuación. Los servidores temporales quedaron detenidos.

Se consultó mediante GET el mismo turno, comprobando su hora de creación. OpenAI terminó posteriormente: completed_at menos created_at = 172 segundos. La respuesta recuperada se validó sin conexión con OpenAiJson.tracedReport: deserialización y validación completas correctas, tres unidades y ninguna exclusión. Esta recuperación diagnóstica no cambia el resultado HTTP 504 de la prueba ni constituye una recuperación automática en la aplicación.

La evaluación de calidad NO se supera:
- A1 declara studyPeriod 2020-2022 y temporalRelation REQUESTED_PERIOD para una pregunta sobre 2023–2025.
- A2 declara ese mismo periodo 2020-2022, pero lo etiqueta RETROSPECTIVE respecto a la pregunta. El esquema admite las etiquetas; no verifica la relación entre el texto del periodo y la pregunta.
- A3 etiqueta el periodo como 2023–2025, aunque el pasaje utilizado del informe OCDE remite a la congelación previa de 2020.
- El veredicto NO_SINGLE_VERDICT se justifica por desacuerdo entre estudios y falta de evidencia nacional homogénea. Las instrucciones prohíben usarlo por esos motivos aislados para una única proposición.
- publishedAt de OCDE figura como 2025-11-01, mientras la ficha oficial identifica 26 de noviembre de 2025.

Revisión de fuentes:
- [UCL Discovery](https://discovery.ucl.ac.uk/id/eprint/10181899/): el resumen describe la regulación catalana de 2020 y ausencia de evidencia de reducción de oferta. Se revisó la ficha y el resumen, no todo el manuscrito.
- [EsadeEcPol](https://www.esade.edu/ecpol/en/publications/the-effects-of-rent-caps-in-catalonia/): fecha 9 de febrero de 2023, análisis de la experiencia previa y del estudio de 2022. La fecha declarada por el agente coincide, pero no convierte el contenido en una evaluación de 2023–2025.
- [OCDE, informe](https://www.oecd.org/content/dam/oecd/en/publications/reports/2025/11/oecd-economic-surveys-spain-2025_cd5c7d04/abc5c435-en.pdf): pasaje sobre vivienda de la página impresa 39, evidencia de la congelación de 2020. [Ficha oficial](https://www.oecd.org/en/publications/oecd-economic-surveys-spain-2025_abc5c435-en/full-report.html): publicación el 26/11/2025.

Consumo informado: 694214 tokens de entrada, incluidos 647680 en caché; 4093 de salida, incluidos 1092 de razonamiento; total 698307. Se registran 18 llamadas web_search_call. Son métricas del turno, no un importe facturado; no se ha calculado el coste monetario.

Conclusión: fallo de entrega por latencia y fallo de calidad semántica. El esquema reforzado elimina ciertas contradicciones entre enums, pero esta salida demuestra que el modelo puede asignar etiquetas formalmente válidas a un alcance incorrecto. No se debe considerar resuelto el problema temporal ni de veredicto. No se ha modificado el resultado para presentarlo como válido.

Artefactos ignorados en bootstrap/target: publications-v8-live-results.json, publications-v8-live-error.png, publications-v8-live.log, publications-v8-report.json, publications-v8-turn.json y publications-v8-usage.json. Sin segunda inferencia ni cambios de código, esquema o instrucciones en esta fase.

Siguiente propuesta: revisar por separado la entrega asíncrona con recuperación del mismo trabajo y la comprobación de pertinencia temporal/veredicto, con presupuesto de búsqueda explícito y evaluación local antes de más consultas reales. No basta con aumentar el timeout o añadir otra regla de texto.


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


### Prueba real de political-v9 / contrato v3 — 29/09/2026

Una única consulta desde el frontend, sin reintentos, con la misma pregunta sobre límites
al alquiler en España durante 2023–2025. Request ID:
`0fa2d4b5-30f4-4f7f-8e82-85c1c5857bb8`. La API devolvió HTTP 504 / ANALYSIS_TIMEOUT a los
90,298 segundos. La UI mostró el error y advirtió de que reintentar puede duplicar el
trabajo. No entregó análisis ni token de continuación. Los dos servidores temporales
se detuvieron tras la prueba.

El mismo turno se recuperó por GET, sin iniciar otra inferencia. Finalizó en 106 segundos
(completed_at menos created_at), frente a los 172 segundos de la prueba v8. Es una sola
observación y no demuestra una mejora estable de latencia. El diagnóstico offline con
OpenAiJson.claimTurn acepta la respuesta original: contrato v3, una afirmación SINGLE,
veredicto documental INSUFFICIENT_EVIDENCE, cuatro unidades contadas y cero exclusiones.
El agente no selecciona NO_SINGLE_VERDICT ni inventa varias afirmaciones para representar
el desacuerdo. Esto mejora el caso concreto, pero no valida todos los casos ni el contexto
conversacional de seguimiento, que no se probó con inferencia real en esta fase.

La calidad de la clasificación de publicaciones NO supera la revisión:

- A1 / S2: cuenta el estudio de Jofre-Monseny, Martínez-Mazza y Segú sobre la regulación
  catalana de 2020, con datos hasta 2022, como RETROSPECTIVE y MATCH para 2023–2025.
  Su propia explicación reconoce alcance parcial. Es un antecedente para esta consulta,
  no una evaluación retrospectiva del periodo pedido.
  [Manuscrito UCL, portada e introducción](https://discovery.ucl.ac.uk/10181899/1/Martinez_final_manuscript_postprint_UB.pdf).
- A2 / S3: atribuye el estudio de Michael Abel, Luisa Carrer y Jaime Luque a Fernando
  Pinto, lo sitúa en 2024–2025 y lo vincula a la Ley 12/2023. El documento estudia la
  regulación catalana de 2020 y datos de 2020–2022. La ficha S3 del propio informe sí
  reconoce ese periodo anterior: hay contradicción entre fuente y evaluación.
  La paráfrasis de A2 y su localizador coinciden con el contenido de S4, por lo que la
  revisión identifica una mezcla de dos publicaciones, no dos apoyos independientes
  correctamente trazados. [Estudio, portada y sección 2](https://www.anderson.ucla.edu/sites/default/files/document/2024-04/CARRER_Abel%20Carrer%20Luque%202024.pdf).
- A3 / S5: presenta una advertencia de riesgos de política como apoyo a la proposición
  histórica delimitada. El pasaje es una valoración preliminar, no una estimación del
  efecto observado durante todo 2023–2025; no debe equipararse automáticamente a una
  evaluación de ese periodo. La fecha y autor corresponden a la intervención. El enlace
  original con /webbde/ falló en esta revisión; se pudo consultar la versión oficial con
  /webbe/. [Banco de España, portada y diapositiva 21](https://www.bde.es/f/webbe/GAP/Secciones/SalaPrensa/IntervencionesPublicas/Gobernador/Arc/Fic/IIPP-2024-04-29-hdc-en-tr.pdf).
- A4 / S4: la autoría Fernando Pinto, junio de 2026 y el análisis retrospectivo de
  2024–2025 coinciden con el texto. Su enfoque es expresamente descriptivo, no causal.
  La ausencia de día exacto justifica publishedAt null; publicar después del periodo
  no invalida por sí solo su pertinencia.
  [FEDEA, resumen y limitaciones](https://documentos.fedea.net/documento/ap2026-15/texto).
- S1: la publicación BOE de 15/03/2024 coincide con publishedAt. No se contó como posición.
  [Ficha oficial](https://www.boe.es/diario_boe/txt.php?id=BOE-A-2024-5213).

Se revisaron los pasajes de identificación, periodo y conclusiones necesarios para estos
hallazgos, no una reproducción independiente de los análisis econométricos. No se
modificó la salida para convertirla en válida ni se recalculó una muestra supuestamente
correcta. Las etiquetas pasan el contrato, pero el contenido no sostiene su asignación.
Con las cuatro unidades recibidas (3 SUPPORTS y 1 QUESTIONS), la regla actual de Java
produciría SUPPORTED_BY_PUBLICATIONS; esa valoración no llegó a la UI por el timeout y
no sería fiable con esta muestra. La validez estructural no garantiza validez semántica.

Consumo informado por el turno: 447143 tokens de entrada (399360 en caché), 4209 de salida
(683 de razonamiento), 451352 en total y 13 llamadas web_search_call. Son métricas de
consumo, no un importe facturado; no se ha estimado coste monetario.

Resultado de fase: fallo de entrega por timeout y fallo de calidad de las publicaciones;
mejora puntual de descomposición y veredicto documental. Sin cambios de código, reglas,
instrucciones o configuración remota durante esta prueba. Artefactos originales ignorados
bajo bootstrap/target con prefijo publications-v9: live-results.json, live-error.png,
live.log, report.json, turn.json, items.json y usage.json.

Siguiente fase propuesta, pendiente de revisión: diseñar la entrega asíncrona que permita
recuperar el mismo trabajo sin repetir inferencias. En paralelo conceptual, concretar la
validación de la identidad y el periodo de cada fuente antes de clasificar posiciones;
no basta con nuevas etiquetas o otra frase en las instrucciones. No iniciar otra prueba
real hasta acordar el siguiente cambio.
