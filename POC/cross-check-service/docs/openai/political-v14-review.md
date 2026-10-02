# Political-v14 — claridad editorial y evidencia verificada

01/10/2026. Ajuste del paquete y las instrucciones de síntesis, sin nuevas inferencias.

- Las observaciones originales de investigación pasan a `unverifiedResearchNotes`. Ya no se añaden automáticamente a las limitaciones del informe como hechos. El modelo solo debe utilizarlas si los hallazgos aceptados las sostienen.
- Los documentos omitidos incluyen título y URL como etiquetas de descubrimiento, con `metadataVerified: false`. No se convierten en fuentes verificadas ni candidatos editoriales.
- Las incidencias de captura presentadas por Java identifican documento y dominio, en lugar de IDs sueltos como S2. También funciona con expedientes guardados de revisiones anteriores.
- Las instrucciones exigen prosa en español sin códigos internos y separan ausencia de evidencia causal de inelegibilidad editorial. Los enums estructurados y el contrato público v3 permanecen intactos.

La revisión local, el ejemplo y el valor predeterminado pasan a political-v14. La investigación y el agente guardado no cambian. [Copia de instrucciones de síntesis](versions/political-v14.synthesis.md). Reiniciar y usar una conversación nueva para aplicar la revisión.

Validación: `mvn verify`, 340 pruebas registradas, 338 superadas y dos opcionales omitidas. Se comprueba que las impresiones no verificadas no se publican automáticamente y que las fuentes inaccesibles quedan identificadas sin adquirir categoría de evidencia.

Límites: las instrucciones editoriales no garantizan por sí solas cumplimiento del modelo. Un título localizado no confirma la autoría ni el contenido; la interfaz no convierte esas etiquetas en referencias probatorias. No se reescribieron los resultados reales v13 ni se ocultaron sus problemas.

## Prueba real posterior

Una única síntesis v14 sobre el expediente guardado terminó COMPLETED, con INSUFFICIENT_EVIDENCE, una fuente documental y cero publicaciones contables. No se repitieron investigación ni capturas. Worker real y base diagnóstica aislada; no es una consulta nueva completa desde el frontend. La auditoría confirmó una sesión, un turno y las instrucciones esperadas. El servidor diagnóstico se cerró al terminar.

Mejoras observadas: las fuentes omitidas se identifican por título y dominio; no se incorporaron las antiguas impresiones sobre fuerza de evidencia catalana o métodos de documentos inaccesibles. La exclusión remite a antecedentes y periodo no elegible.

Cumplimiento editorial parcial: `selectionCriteria` todavía contiene COUNT. También menciona posición propia como requisito general, aunque NO_EXPLICIT_POSITION es una categoría válida cuando se cumplen los demás criterios. No se considera resuelta toda la redacción ni se atribuye a la validación estructural una comprobación semántica completa. La respuesta original se conserva sin corregirla artificialmente.

Duración aproximada: 30,6 segundos. Uso: 10.503 tokens de entrada, cero en caché; 802 de salida; total 11.305. Una sola inferencia. Artefactos privados: `bootstrap/target/v14-resume-*` y base `bootstrap/target/political-v14-resume`.
