# Political-v10 — publicación y prueba real (30/09/2026)

Estado: publicada y verificada; prueba funcional real fallida por respuesta inválida. No se considera resuelto el problema de calidad de fuentes.

## Publicación y configuración

Se actualizó únicamente `instructions` en el agente existente. La lectura posterior coincide exactamente con el fichero local y mantiene nombre, modelo gpt-5.4-mini, razonamiento low, service tier default, herramientas, metadata y contrato v3. La lectura de la sesión ejecutada confirma que también copió esas instrucciones.

SHA256 de instrucciones: `1c986fa1ec6fd76ce04fce914f2e6ffd0038249fd07e2853cf4cb5121c8d916c`.
[Copia publicada](versions/political-v10.instructions.md). `POLITICAL_AGENT_REVISION` local, valor por defecto del perfil y ejemplo de configuración actualizados a political-v10. El nombre visible del agente continúa siendo cross-check-political-v1: no identifica la revisión de comportamiento.

Referencia de la operación: [OpenAI, actualizar un agente](https://developers.openai.com/api/reference/python/resources/beta/subresources/agents/methods/update).

## Ejecución

Consulta: «¿Los límites al precio del alquiler reducen la oferta de vivienda en alquiler en España? Analiza el periodo 2023–2025 y contrasta publicaciones con distintas posiciones.»

- Trabajo: `5745aa06-0040-4923-8f9f-8c53f29b13c1`.
- Navegador: un POST, 16 GET y recarga durante RUNNING; ninguna nueva consulta ni reintento de inferencia.
- Creación: 17:30:54.351 UTC. Fin: 17:31:53.951 UTC; unos 59,60 s en el servicio y 73,10 s en la revisión de navegador.
- OpenAI: una sesión, un turno completed. Servicio: FAILED / INVALID_ANALYSIS_OUTPUT. No se presentó un informe como válido al usuario.
- Uso informado por el turno: 258.198 tokens de entrada (220.160 en caché), 6.451 de salida (3.371 de razonamiento); total 264.649. No se ha calculado importe monetario; incluye el trabajo de investigación del turno, no solo la respuesta final.

[Registro público de ejecución](../../../webapp/review/integration/political-v10-live-20260930.json).
[Respuesta original rechazada](../../../webapp/review/integration/political-v10-provider-output-20260930.json). Es evidencia del fallo, no un informe validado ni una respuesta corregida.

## Diagnóstico reproducido offline

La deserialización estricta funciona, pero `ClaimAnalysis.validateInput` rechaza el inputExcerpt con «Missing or ambiguous input anchor». El agente unió la pregunta y el periodo en una frase nueva: añadió «durante 2023–2025» dentro de la interrogación. El contrato exige un fragmento literal y único de la entrada; ese texto no aparece en ella. La ruta completa `OpenAiJson.claimTurn` reproduce INVALID_ANALYSIS_OUTPUT sin red.

No se relajó la validación ni se alteró la respuesta para aceptarla. El rechazo por el anclaje no demuestra que el backend detecte los fallos semánticos siguientes.

## Revisión de fuentes y posicionamiento

| Hallazgo | Evidencia y consecuencia |
|---|---|
| Referencia cruzada incorrecta | A1 describe el informe FEDEA, pero tanto sourceIds como sus argumentos apuntan a S1, que corresponde al BOE. FEDEA es S2. Los IDs existen, pero la atribución semántica es errónea. |
| Fecha BOE incorrecta | S1 devuelve 2024-03-14, fecha de la resolución. La [ficha del BOE](https://www.boe.es/buscar/doc.php?id=BOE-A-2024-5213) indica publicación el 15/03/2024. |
| Fecha Banco de España incorrecta | S4 devuelve 2024-01-01. La [ficha oficial del documento 2432](https://www.bde.es/wbe/es/publicaciones/analisis-economico-investigacion/documentos-ocasionales/el-mercado-del-alquiler-de-vivienda-residencial-en-espana-evolucion-reciente-determinantes-e-indicadores-de-esfuerzo.html) indica 16/10/2024. |
| Funcas clasificada como QUESTIONS sin justificación suficiente | El [artículo de marzo de 2024](https://www.funcas.es/articulos/cual-el-esfuerzo-por-vivir-de-alquiler-en-espana-evolucion-y-diferencias-por-comunidades-autonomas/) advierte sobre la falta de estimaciones fundamentadas y comenta medidas desde 2022. Esa cautela metodológica no basta para inferir una postura contraria sobre la proposición exacta de 2023–2025. Deben verificarse también política y periodo antes de decidir COUNT, NO_EXPLICIT_POSITION o EXCLUDE. |
| Límites descriptivos mejor explicitados, pero resultado no fiable | La síntesis diferencia anuncios y causalidad, y reconoce que FEDEA es retrospectiva. Sin embargo, A1 atribuye ese argumento a otra fuente y su justificación MATCH no resuelve de forma suficiente el salto entre indicador territorial y proposición nacional. El [documento FEDEA](https://documentos.fedea.net/documento/ap2026-15/texto) distingue expresamente el agregado nacional como contexto y el contraste territorial como evidencia. |

El agente mantuvo SINGLE, redactó la síntesis sin códigos de enum y devolvió AVAILABLE con dos entradas contables propuestas y una excluida. Estas mejoras formales no permiten validar los porcentajes: sus bases contienen los errores descritos. La valoración individual propuesta era INSUFFICIENT_EVIDENCE, pero no llegó a aceptarse un informe ni a derivarse un resultado final en el servicio.

La revisión no constituye una reproducción estadística de los estudios ni una auditoría exhaustiva de todo el informe. Los 16 escenarios sintéticos siguen NOT_RUN; esta consulta real es una prueba distinta.

## Verificación y cierre

- OpenAiApiTest: 5 casos registrados, 3 superados, 2 verificaciones opcionales de navegador omitidas. Se verifica political-v10 en el token de conversación.
- Se reutilizó el artefacto Java ya verificado con el perfil local actualizado; la lógica de producción no cambió. La prueba focalizada recompiló el cambio del valor por defecto.
- Las credenciales y los datos privados de sesión permanecen en configuración local y bootstrap/target ignorados. La base habitual no se utilizó; la prueba tiene base aislada.
- No hubo una segunda inferencia. Los procesos temporales se detienen al cerrar esta fase.

Siguiente fase propuesta: fijar esta respuesta como caso de regresión y diseñar una extracción de evidencia por fuente que conserve identidad, fecha, periodo y postura antes de sintetizar. Incluir la copia literal del anclaje y distinguir cautela metodológica de oposición. Revisar ese diseño y sus pruebas offline antes de publicar más instrucciones o gastar en otra consulta. No se ha implementado aún ese cambio.
