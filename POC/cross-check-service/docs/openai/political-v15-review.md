# Political-v15 — criterios editoriales controlados por la aplicación

01/10/2026. Java proporciona el texto público de `selectionCriteria` para muestras disponibles. El modelo ya no decide la descripción de las reglas de la aplicación. El texto admite respaldo, cuestionamiento, posición mixta y ausencia de posición explícita, conservando alcance, periodo y evidencia aceptada como condiciones. No contiene códigos internos ni exige postura propia o prueba causal como requisito general.

Las instrucciones también aclaran la admisión de NO_EXPLICIT_POSITION. Las explicaciones específicas y las decisiones siguen pasando por el validador; no se traduce ni reescribe arbitrariamente el resto de la respuesta. El campo del borrador se conserva por compatibilidad interna, pero su redacción no se publica. Para disponibilidad UNAVAILABLE, el criterio público queda null.

Validación: `mvn verify`, 342 pruebas registradas, 340 superadas, dos opcionales omitidas. Nuevas regresiones comprueban que el proveedor no puede sustituir el criterio con «postura propia ... COUNT» y que una publicación elegible sin postura propia cuenta como NO_EXPLICIT_POSITION, manteniendo INSUFFICIENT_EVIDENCE.

Se reensambló offline la respuesta real v14: supera la validación y recibe el criterio corregido. No se modificó su archivo histórico ni se ejecutó inferencia. Los informes ya guardados en navegador o trabajos completados conservan su contenido histórico; esta corrección aplica a nuevos ensamblados.

Revisión predeterminada, ejemplo y configuración local: political-v15. [Instrucciones](versions/political-v15.synthesis.md). Reiniciar el servicio y usar conversación nueva para aplicar la revisión. Investigación y agente guardado sin cambios. La calidad semántica de otras explicaciones libres no queda garantizada por este ajuste.

Prueba posterior desde el frontend con investigación nueva: completada en unos 81 segundos, con recarga durante la ejecución y revisión móvil/escritorio. [Consulta Plus Ultra, resultado y problemas de calidad encontrados](political-v15-plus-ultra-review.md).
