# Expediente de evidencia — political-v11

Actualización vigente: [political-v12](openai/political-v12-review.md) separa candidatos evaluables de fuentes sin hallazgos y explicita IDs permitidos y restricciones de COUNT. Revisada offline; aceptación real pendiente. Las referencias v11 inferiores documentan la implementación inicial.

Estado actual (01/10/2026): investigación, captura independiente y síntesis implementadas, con persistencia y recuperación por etapas. Contrato interno v1, informe público v3 y revisión political-v11. La primera fase offline descrita más abajo se conserva como antecedente, no como estado actual. Véase la [revisión real](openai/political-v11-live-review.md).

## Implementación vigente

1. **RESEARCH:** el modelo investiga con búsqueda web. Java proporciona opciones literales de anclaje; el modelo selecciona IDs y propone fuentes y citas.
2. **CAPTURE:** Java descarga HTML, texto o PDF y comprueba las citas contra el texto normalizado. Asigna IDs, URL final, hash y fecha de acceso. Una fuente inaccesible no aporta hallazgos.
3. **SYNTHESIS:** otra sesión, sin herramientas, recibe los hallazgos aceptados y las limitaciones. Devuelve valoraciones y referencias a IDs; Java ensambla las fuentes, fechas y anclajes del informe v3.

`EvidencePipeline`, `PublicSourceCapture` y `OpenAiEvidenceJobProvider` implementan estas etapas. H2 conserva el expediente y el historial de sesiones, turnos, huellas y uso. `NEXT_STAGE` protege el siguiente envío; una respuesta ambigua se reconcilia sin repetir la inferencia. La conversación lógica mantiene su identidad entre sesiones remotas.

Instrucciones y esquemas vigentes: `infrastructure/src/main/resources/openai/evidence/{research,synthesis}.{md,schema.json}`. `./scripts/prepare-openai-evidence.ps1` genera ambas configuraciones en target sin publicar ni consumir la API. Las plantillas anteriores son históricas. Activación: `openai,local`, `POLITICAL_AGENT_REVISION: political-v11`, `OPENAI_EVIDENCE_PIPELINE_ENABLED: true` y trabajos asíncronos activos. `dev,local` sigue simulado.

Una URL exacta repetida se captura y cuenta una sola vez si coinciden sus metadatos de identidad y postura. Se combinan sus hallazgos; autores discrepantes quedan sin confirmar y se registra la limitación. Posturas incompatibles se rechazan.

La adquisición limita tamaño, duración y redirecciones, valida DNS y destinos y no envía credenciales ni cookies. No sortea bloqueos ni aplica OCR a PDFs escaneados. Solo confirma días completos de metadatos de publicación admitidos y consistentes; no usa fechas de creación de PDF ni convierte una actualización en publicación. En los demás casos `publishedAt` queda null.

Continúan las reglas COUNT/EXCLUDE y SINGLE/MULTIPLE. Documentos legales y datos brutos no cuentan como posicionamiento editorial. Sin hallazgos el trabajo falla con `EVIDENCE_CAPTURE_FAILED` antes de sintetizar. No hay reparación automática de salidas inválidas. El expediente se purga con la retención del trabajo y no se expone en la API pública.

**La coincidencia textual no verifica por sí sola la interpretación, causalidad o postura.** EV05 sigue requiriendo evaluación semántica. La captura tampoco garantiza acceso a todas las fuentes ni un lector universal de fechas.

## Antecedente: contrato offline

## Componentes

- `EvidenceDossier`: entrada/contexto, afirmaciones con anclajes, fuentes y hallazgos. Comprueba coherencia al construirse y copia defensivamente todas las listas.
- `Capture`: contenido proporcionado por un adaptador de adquisición de confianza, URL resuelta, versión y fecha de acceso. Calcula SHA256 del texto UTF-8 conservado. Es el hash del texto capturado, no de los bytes originales de un PDF o de una página.
- `Fragment`: texto literal con offsets UTF-16 y localizador de la versión capturada. Se comprueba que coincida exactamente con el contenido de su fuente.
- `PublicationDate`: valor, precisión, tipo de fecha, evidencia y estado de revisión. Solo publica un día completo cuando el tipo es PUBLICATION y la revisión es CONFIRMED.
- `Finding`: fuente y afirmación a las que pertenece, pasaje, paráfrasis, atribución, método, alcance, periodo y relación con la afirmación.
- `EvidenceAssembler`: proyecta las fuentes al tipo `Evidence` ya existente y resuelve referencias de hallazgos a IDs de fuente sin permitir cambiar su identidad o su afirmación asociada.

El código está en `domain/src/main/java/com/crosscheck/domain/analysis/evidence`. Las pruebas están en `domain/src/test/java/com/crosscheck/domain/analysis/evidence/EvidenceDossierTest.java`.

## Garantías comprobables

1. Los anclajes son fragmentos literales, únicos y no solapados de la entrada o del contexto suministrado. La proposición resuelta se conserva por separado; no se inserta un periodo en el fragmento literal.
2. Los IDs de fuente, hallazgo y afirmación son únicos dentro del expediente. Cada hallazgo pertenece a una fuente existente y a una afirmación existente.
3. Un hallazgo requiere contenido CAPTURED. MODEL_EXCERPT_ONLY e INACCESSIBLE pueden registrarse con su limitación, pero no aportan hallazgos verificados por coincidencia textual.
4. Un fragmento atribuido al BOE que solo aparece en el contenido de FEDEA se rechaza. Una referencia posterior a un hallazgo de FEDEA tampoco puede reasignarlo al BOE.
5. Fechas parciales conservan su precisión y su descripción, con publishedAt null. Una fecha declarada sin confirmar, una resolución o una actualización tampoco se publican como fecha exacta de publicación.
6. El ensamblado toma fechas y metadatos del expediente; no los vuelve a generar. Se proporciona una comprobación de compatibilidad para rechazar fechas propuestas distintas de la aceptada.
7. La fecha de acceso procede de Capture. Para fuentes sin captura queda null y la contribución explica la limitación.

## Frontera de confianza y límites

Estos tipos son un contrato Java interno, no un DTO que deba deserializarse directamente desde el JSON del modelo. El adaptador futuro debe asignar los IDs, suministrar las capturas y conceder CONFIRMED solo tras una comprobación independiente. En esta fase no hay parser de fechas ni adquisición HTTP, y una marca CONFIRMED suministrada por el modelo no es admisible como verificación.

La clase comprueba que la evidencia textual existe, pero no interpreta si «15 de marzo» realmente es la fecha editorial ni si una cita respalda la paráfrasis. El lector de metadatos o el revisor de confianza tendrá que establecer ese significado. Una captura que el modelo fabrica no se convierte en fiable por calcularle un hash.

Los hallazgos conservan las interpretaciones de alcance, método y postura; no se ha implementado un clasificador semántico. En particular, no se da por resuelto EV05: falta de estimaciones no equivale automáticamente a oposición. El ensamblador no crea votos, porcentajes ni veredictos.

La proyección actual prepara fuentes y referencias. Todavía no ensambla el informe completo ni aplica nuevos criterios de selección: la integración posterior deberá conservar las reglas existentes COUNT/EXCLUDE, SINGLE/MULTIPLE y validación final. El expediente permite no tener fuentes/hallazgos; ese estado no autoriza por sí solo un veredicto de evidencia insuficiente.

Los registros en memoria conservan texto para comprobar los fragmentos. Persistencia, límites de tamaño, retención, serialización y adquisición segura corresponden a las siguientes fases; no se ha creado una ruta pública que acepte ese contenido.

## Pruebas y relación con los fallos reales

Ejecutado: `mvn -pl domain test`. Resultado: **56 pruebas superadas, 0 omitidas**, incluidas 15 nuevas del expediente.

| Caso | Cobertura offline actual | Pendiente |
|---|---|---|
| EV01 | Rechaza el anclaje reformulado observado en v10; acepta un anclaje literal y contexto previo explícito. | Conectar esta validación antes de iniciar la síntesis. |
| EV02 | Rechaza referencias fuente–hallazgo cruzadas y fragmentos ausentes en la captura asignada. | Verificar que la extracción inicial interpreta y atribuye correctamente un pasaje auténtico. |
| EV03/EV04 | Con metadatos mínimos revisados en fixtures, ensambla 15/03/2024 y 16/10/2024; rechaza las fechas erróneas de v10. | Extraer y revisar estos metadatos desde documentos reales mediante un adaptador. Las pruebas no consultan esas webs. |
| EV05 | Sin evaluación semántica automatizada. | Revisar la postura, política y periodo sin deducir oposición de la incertidumbre. |

También se comprueban anclajes ambiguos o solapados, fechas parciales, evidencia de fecha ausente o ajena, fuentes inaccesibles, alteración de capturas, IDs duplicados/desconocidos, colecciones inmutables y reutilización de un hallazgo para otra afirmación existente.

Los textos breves de prueba son fixtures controlados. No reproducen una investigación real ni demuestran una mejora del modelo. La [manifestación de regresiones](../../../Docs/proposals/source-evidence-regression-v10.json) mantiene la referencia y hash de la respuesta original; los 16 escenarios semánticos anteriores siguen sin ejecutarse.

## Siguiente fase

Diseñar e implementar la persistencia y recuperación por etapas con proveedor simulado: guardar el expediente antes de sintetizar, reanudar sin duplicados y mantener el plazo y las reservas de capacidad. Publicar nuevos contratos o ejecutar otra consulta real requerirá una fase posterior.
