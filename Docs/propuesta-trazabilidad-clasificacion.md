# Propuesta de trazabilidad de la clasificación

Estado: contrato de entrada, validaciones, conservación en dominio y exposición en API implementados el 27/09/2026. El frontend ya presenta y valida la estructura de trazabilidad; el lector remoto v2 continúa sin activar. Diseño original: 26/09/2026. Los apartados por fase conservan el historial de avances.

## Objetivo

Evitar que una publicación se contabilice por opiniones que atribuye a terceros o por argumentos sobre otra variable, política o periodo.

## Información propuesta por unidad

- Proposición exacta y alcance al que responde: variable, territorio, periodo y política.
- Argumento identificable en el contenido consultado, con fuente y localizador cuando exista. Los extractos deben ser breves y respetar los límites de reproducción.
- Autor de la postura: publicación o autor del análisis, tercero citado, o ausencia de postura propia.
- Relación con el periodo: evidencia del periodo, evaluación retrospectiva o antecedente.
- Justificación de SUPPORTS, QUESTIONS, MIXED o NO_EXPLICIT_POSITION sobre esa proposición.

Un tercero citado no convierte automáticamente al medio en partidario de su tesis. Un antecedente puede aportar contexto sin entrar en la muestra del periodo solicitado. MIXED requiere argumentos propios en ambas direcciones, no simplemente recopilar voces opuestas.

## Decisión pendiente

Revisar si estos datos deben añadirse al contrato estructurado o conservarse inicialmente en la explicación existente. Un contrato estructurado facilitaría comprobaciones de presencia y referencias, pero no probaría por sí mismo que el argumento coincide con la fuente. Para verificar esa fidelidad se necesita acceso trazable al contenido y una evaluación semántica separada.

No se propone asignar una postura automáticamente a partir de palabras clave ni corregir resultados a mano. Tampoco se ha añadido una segunda llamada al modelo ni otro coste de ejecución.

## Ejemplos de aceptación

| Caso | Tratamiento esperado |
| --- | --- |
| Noticia que atribuye a una asociación una caída de oferta | Identificar a la asociación como titular de la opinión; no inferir SUPPORTS del medio sin argumento propio. |
| Artículo que presenta voces opuestas sin conclusión propia | No convertir esa pluralidad de voces automáticamente en MIXED. |
| Estudio sobre política de 2020 ante pregunta 2023–2025 | Antecedente, salvo vínculo explícito de la publicación con la proposición temporal solicitada. |
| Análisis con argumentos propios contrapuestos sobre la misma proposición | MIXED con ambos argumentos localizables. |

Esta propuesta queda fuera de la implementación de metadatos temporales. Requiere revisión antes de cambiar esquema, instrucciones o interfaz.

## Esquema y ejemplos preparados

Se propone un registro intermedio de evaluación de publicaciones, previo al recuento. El borrador completo de ese registro está en [publication-assessment.schema.json](proposals/publication-assessment.schema.json), y cinco ejemplos completos de registros en [publication-assessment.examples.json](proposals/publication-assessment.examples.json). No son informes completos ni sustituyen el esquema JSON activo del agente. Son datos ficticios, no resultados obtenidos del modelo; cada ejemplo usa su propia fuente ficticia S1.

| Campo | Finalidad |
| --- | --- |
| `id`, `sourceIds`, `explanation` | Identificar la unidad, agrupar reproducciones y explicar la decisión. |
| `decision` | `COUNT` para entrar en la muestra o `EXCLUDE` para quedar fuera. |
| `position` | Los cuatro códigos actuales cuando se cuenta; `null` cuando se excluye. No se añade un quinto posicionamiento. |
| `trace.stanceOwner` | `AUTHOR`, `THIRD_PARTY`, `NONE` o `UNDETERMINED`. AUTHOR es el autor de la pieza o informe, no necesariamente la línea editorial del medio. |
| `trace.stanceHolder` | Nombre del titular de la postura cuando sea conocido; `null` si no consta. |
| `trace.scope` | Variable, territorio, periodo estudiado, política, relación con la pregunta y explicación. El periodo estudiado es texto, no se confunde con las fechas de publicación. |
| `trace.temporalRelation` | `REQUESTED_PERIOD`, `RETROSPECTIVE`, `BACKGROUND` o `UNKNOWN`. Describe la relación con el periodo analizado, no la fecha de publicación. |
| `trace.arguments` | Paráfrasis breve, fuente, localizador cuando exista, atribución y relación con la proposición. No son citas literales ni texto que Java haya verificado. |

Se prefieren paráfrasis a extractos literales. Un localizador desconocido se expresa como `null`; no se inventan páginas o párrafos. En reproducciones agrupadas, los argumentos pueden remitir al original accesible, pero debe constar la relación de reproducción antes de agrupar. Las fuentes mantienen sus URL en el catálogo común.

## Reglas de decisión propuestas

Estas reglas irían en Java; no están implementadas en esta fase. El JSON Schema adjunto valida estructura, no todas las relaciones entre campos. No se ha verificado su compatibilidad con Structured Outputs: es un borrador de revisión, no una petición lista para publicar.

1. Toda referencia de argumento debe pertenecer a `sourceIds` de la unidad y existir en el catálogo del informe. No se permiten IDs duplicados, doble recuento ni fuentes simultáneamente contadas y excluidas.
2. `COUNT` exige posición no nula, `scope.match=MATCH` y relación temporal `REQUESTED_PERIOD` o `RETROSPECTIVE`. Un ámbito parcial o incierto se excluye con motivo; no se fuerza la coincidencia ni se amplía la pregunta para incluirlo.
3. SUPPORTS y QUESTIONS exigen `stanceOwner=AUTHOR` y al menos un argumento propio en la dirección correspondiente. MIXED exige argumentos propios en ambas direcciones. Esto comprueba la declaración estructurada, no que sea fiel al texto.
4. THIRD_PARTY o NONE pueden entrar como NO_EXPLICIT_POSITION si la pieza trata expresamente la proposición y se ha leído contenido suficiente para justificar la ausencia de postura propia. UNDETERMINED se excluye: desconocer la postura por falta de acceso no equivale a ausencia de postura.
5. `EXCLUDE` exige `position=null` y explicación. BACKGROUND u OUT_OF_SCOPE se excluyen de esta muestra. La fuente puede seguir aportando contexto al análisis.
6. Una noticia con voces opuestas no es MIXED automáticamente. Si el autor no hace suyos argumentos en ambas direcciones, corresponde examinar NO_EXPLICIT_POSITION.
7. No se reparan contradicciones cambiando posturas silenciosamente. Un registro incompatible se rechaza con el tratamiento técnico acordado; las exclusiones legítimas son decisiones explícitas con motivo.

Los ejemplos muestran: respaldo propio, cuestionamiento retrospectivo, postura propia mixta, atribución a tercero sin postura propia y exclusión de un antecedente. No cubren todos los casos. Por ejemplo, una pieza con postura propia y citas ajenas conservaría AUTHOR, distinguiendo la atribución en cada argumento.

## Impacto propuesto

| Componente | Cambio necesario antes de activarlo |
| --- | --- |
| Agente | Generar evaluaciones estructuradas para las piezas examinadas. Preservar la diferencia entre evidencia documental y muestra editorial. No añadir una segunda inferencia por defecto. |
| Contrato de entrada del proveedor | Incorporar una colección `assessments` que sustituya la doble declaración independiente de `units` y `excluded`. Esquema completo y migración pendientes; el borrador adjunto solo define cada registro. |
| Adaptador Java | Deserializar y validar evaluaciones; producir `units` desde COUNT y exclusiones desde EXCLUDE. Expandir fuentes excluidas agrupadas sin duplicarlas. Calcular el periodo únicamente con las unidades contadas, como ya hace el adaptador. |
| Dominio y aplicación | Conservar trazabilidad en unidades y exclusiones. Mantener los cuatro posicionamientos y la política actual de ocho valoraciones finales. Los porcentajes podrían variar al cambiar qué publicaciones son elegibles. |
| API hacia el frontend | Mantener `units` y `excluded` para presentar el resultado, ampliándolos con trazabilidad. No devolver simultáneamente otra colección contradictoria de evaluaciones. |
| Frontend | Mostrar la justificación en «Ver desglose de publicaciones» y las razones ampliadas en exclusiones. Mantener la posición del panel: derecha en escritorio y después del análisis en móvil. No mostrar enums técnicos como etiquetas al usuario. |
| Historial local | Versionar o migrar el formato almacenado. Marcar resultados antiguos como «Trazabilidad no disponible» sin inventar datos ni invalidarlos solo por falta del nuevo campo. |
| Despliegue | Coordinar esquema del proveedor, backend y frontend; elevar la revisión de compatibilidad del token. No basta con cambiar el Markdown del agente. |

La transformación de evaluaciones a unidades es una propuesta arquitectónica, no un cambio ya realizado. Antes de implementarla se debe acordar el esquema completo del informe y el tratamiento de errores parciales. Se propone conservar inicialmente el rechazo del informe completo ante incoherencias estructurales; no eliminar unidades automáticamente porque eso alteraría el reparto sin explicación.

## Revisión solicitada para la siguiente fase

Confirmar el registro intermedio, las reglas de elegibilidad y la presentación dentro del desglose existente. Después se prepararía el contrato completo y su implementación por fases. La revisión local de estos archivos comprueba sintaxis y coherencia de los cinco ejemplos; no valida su ejecución por el modelo, el contenido de fuentes reales ni la compatibilidad remota del esquema.

## Primera fase de backend realizada — 27/09/2026

Se prepararon [el esquema completo del informe de entrada v2](proposals/political-report-v2.schema.json) y [un informe ficticio completo](proposals/political-report-v2.example.json). `schemaVersion: "2"` versiona el contrato JSON; no es la revisión del agente ni la revisión incluida en el token.

`OpenAiTracedReport` representa las evaluaciones estructuradas y aplica las reglas. `OpenAiJson.tracedReport` es el punto de lectura estricto del nuevo formato, deliberadamente separado de `report`, que sigue atendiendo las respuestas del agente actual. No hay detección automática ni degradación silenciosa al formato antiguo.

Se rechazan referencias ajenas a la unidad, fuentes inexistentes, duplicados, inclusión de ámbitos parciales o antecedentes, posturas de terceros contabilizadas como propias y MIXED sin argumentos propios en ambas direcciones. SUPPORTS y QUESTIONS no admiten argumentos propios en la dirección contraria; NO_EXPLICIT_POSITION no admite argumentos propios direccionales. Las contradicciones producen el error técnico existente, sin corregir posturas ni eliminar unidades silenciosamente.

La conversión produce las unidades actuales desde COUNT y expande las fuentes agrupadas de EXCLUDE a exclusiones individuales. Reutiliza el cálculo del periodo y descarte de horas no verificables del adaptador. Las validaciones globales del dominio siguen activas.

Limitación deliberada de esta fase: la trazabilidad existe en el DTO de entrada y se valida, pero todavía no se conserva en el informe de dominio ni se envía al frontend. Su transporte y presentación requieren la siguiente fase antes de activar este lector contra OpenAI. No se ha actualizado el agente remoto, el contrato activo de la API ni el historial del navegador. Las secciones anteriores describen el diseño objetivo; este apartado especifica lo implementado hasta ahora.

La semántica sigue dependiendo del contenido: declarar AUTHOR o MATCH en un JSON no demuestra autoría ni coincidencia real. No se han ejecutado consultas de pago ni comprobado compatibilidad del borrador con el servicio remoto.

### Trazabilidad conservada en dominio y API — 27/09/2026

PublicationTrace conserva titular de la postura, alcance, relación temporal y argumentos con referencias. PublicationAssessment y ExcludedPublication incorporan trace nullable; los constructores anteriores mantienen compatibilidad y representan ausencia de trazabilidad con null. Las listas de argumentos se copian defensivamente. Se comprueban referencias de argumentos dentro de las unidades y existencia de las referencias de exclusiones en el informe.

El lector preparado para proveedor v2 conserva los datos al convertir COUNT/EXCLUDE al dominio; las exclusiones de reproducciones agrupadas comparten la justificación original, que puede referenciar otra fuente de ese grupo. La API añade trace en unidades y exclusiones mediante DTOs propios, sin modificar el cálculo de posiciones ni los ocho veredictos. El lector activo del formato anterior admite trace ausente o null y rechaza trazabilidad no nula por esa vía: no acepta justificaciones nuevas sin migrar el contrato.

El frontend actual tolera los campos adicionales pero todavía no presenta ni valida en detalle su contenido. El lector v2 continúa sin activarse y el agente remoto sigue en political-v6. La próxima fase debe añadir tipos, validación y visualización de la trazabilidad, contemplando informes del historial sin ese campo. No hay inferencias de pago en esta fase.
### Trazabilidad en el frontend — 27/09/2026

El contrato TypeScript admite trace en unidades y exclusiones, con validación de campos, enumerados, argumentos y referencias. Para las unidades, los argumentos deben referenciar fuentes de esa misma unidad; las exclusiones agrupadas pueden referenciar otras fuentes conocidas del informe. Esto valida estructura y referencias, no fidelidad a las fuentes ni todas las reglas semánticas del backend.

El desglose existente muestra titular de la postura, alcance, periodo estudiado, medida, relación temporal, argumentos parafraseados y localizadores cuando constan. Cada argumento permite abrir la fuente mediante el diálogo existente. Las etiquetas son españolas y se indica que es una justificación declarada, no una comprobación independiente.

Trace ausente o null se muestra como «Trazabilidad no disponible». El historial antiguo sigue aceptándose sin migración destructiva ni datos inventados; la recarga no vuelve a enviar consultas. No cambia el lugar del panel, el cálculo de porcentajes ni los ocho veredictos. El nuevo lector de proveedor sigue sin activarse en OpenAI.

Verificación local: npm run build y scripts/verify-trace.cjs, con respuestas HTTP simuladas. Se comprueban escritorio y móvil, referencias, exclusiones, persistencia tras recarga, ausencia/null y cuatro trazas inválidas. Capturas en POC/webapp/review/integration/trace-1536.png y trace-390.png. Sin inferencias de pago.
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
