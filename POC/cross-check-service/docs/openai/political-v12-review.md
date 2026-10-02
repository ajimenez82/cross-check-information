# Political-v12 — candidatos y referencias de síntesis

01/10/2026. Ajuste implementado y comprobado offline. Posteriormente se ejecutó una única síntesis real con el expediente guardado, sin repetir investigación ni capturas. La aceptación real sigue fallida por `publicationPositions.reason` ausente. El resultado fallido v11 se conserva sin modificaciones.

## Cambio

El paquete separa `publicationCandidates` de `omittedSources`. Las fuentes sin hallazgos aceptados solo sirven para explicar limitaciones; no reciben evaluaciones, ni siquiera EXCLUDE. Los documentos con hallazgos siguen disponibles para la narrativa.

Cada candidato declara `allowedFindingIds`, `countEligibleFindingIds`, `requiredDirectionalFindingIds`, `allowedDecisions` y restricciones. BACKGROUND, periodos no admitidos, alcance no coincidente y tipos no editoriales no habilitan COUNT. NO_EXPLICIT_POSITION tampoco permite eludir esos criterios. La elegibilidad temporal usa la misma función en el paquete y en el validador; no se debilitan las comprobaciones existentes.

El modelo debe referenciar hallazgos, nunca IDs de fuente, también al excluir una publicación. Los candidatos que solo admiten EXCLUDE mantienen sus fragmentos citables para el análisis factual. Los permisos para COUNT son condiciones necesarias, no una confirmación de postura o causalidad.

## Revisión y activación

Instrucciones activas: `infrastructure/src/main/resources/openai/evidence/synthesis.md`. Copias: [v11](versions/political-v11.synthesis.md), [v12](versions/political-v12.synthesis.md). El esquema de salida y el informe público v3 no cambian. Investigación y agente guardado no se han modificado; la síntesis se configura por sesión. Véase [configuración oficial](https://developers.openai.com/api/docs/guides/agents-api/configuration).

`POLITICAL_AGENT_REVISION` pasa a political-v12 en configuración local, ejemplo y valor predeterminado. Reiniciar el servicio aplica los recursos y la revisión; usar una conversación nueva para no reutilizar tokens de la revisión anterior. No se ha arrancado un servicio de larga duración durante esta entrega.

## Comprobaciones

- `mvn verify`: 338 pruebas registradas, 336 superadas, dos opcionales omitidas; cero fallos.
- Nuevas regresiones: fuente inaccesible sin candidato y rechazo de S2 como hallazgo; evidencia BACKGROUND excluida pero citable; publicación elegible con sus IDs aceptados. La exclusión válida produce un informe con fuentes y sin votos, manteniendo evidencia insuficiente.
- Revisión offline del expediente real guardado: una candidata S1, solo EXCLUDE; tres fuentes omitidas; dos hallazgos. La respuesta original inválida sigue siendo rechazada. No se corrigió ni se presentó como respuesta real satisfactoria.
- Los esquemas y ejemplos se validan offline. No se repite el navegador porque el contrato público y el frontend no cambian; los 16 escenarios previos no demuestran la calidad de esta nueva síntesis.

## Prueba real posterior

Se ejecutó una única sesión y un único turno de síntesis mediante el worker real, en una base diagnóstica aislada. La auditoría de lectura confirmó las instrucciones v12. El proceso de prueba terminó y cerró el servidor al finalizar.

La respuesta corrigió los dos problemas anteriores: solo evaluó S1 con EXCLUDE y position null, utilizando F1_1 y F1_2. No creó evaluaciones para S2, S3 ni S4. Propuso INSUFFICIENT_EVIDENCE y explicó que los fragmentos disponibles no establecen causalidad.

El resultado local fue FAILED / INVALID_ANALYSIS_OUTPUT. La reproducción offline de la respuesta exacta identifica `InvalidAnalysisReportException: positions.reason es obligatorio`: `publicationPositions.reason` vino null aunque todas las candidatas estaban excluidas. El contrato exige una explicación cuando no hay unidades contables. No se alteró la respuesta ni se mostró un informe satisfactorio.

Uso de esta síntesis: 9.892 tokens de entrada, cero en caché, 840 de salida, total 10.732. No se ejecutó una segunda inferencia. Artefactos privados y base aislada: `bootstrap/target/v12-resume-*` y `bootstrap/target/political-v12-resume`.

Siguiente ajuste propuesto: exigir siempre una explicación no vacía en el contrato interno de síntesis para `publicationPositions.reason`, evitando que el modelo tenga que resolver esa condición. Debe mantenerse la validación pública y probarse antes de otra inferencia. El éxito en referencias y exclusión no acredita por sí solo la calidad semántica del informe completo.
