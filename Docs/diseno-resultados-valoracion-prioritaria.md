# Resultados con valoración prioritaria

Implementación frontend revisada el 09/10/2026. Primera fase del rediseño; contrato y agente sin cambios.

## Organización
1. Valoración final con icono, etiqueta destacada, título, explicación íntegra, referencias y número de fuentes.
2. Claves del análisis con la síntesis existente. Solo se omite si su texto es exactamente igual al de la explicación; no se deduplican frases por semejanza.
3. Contexto, explicación documental y limitaciones en desplegables.
4. Desglose por afirmaciones solo cuando hay varias, conservando los veredictos y sus referencias.
5. Posiciones de las publicaciones a la derecha en escritorio y después del análisis, antes de las fuentes, en móvil.
6. Cuatro fuentes visibles inicialmente y un desplegable para el resto. Las tarjetas muestran publicación, título y fecha; la contribución completa sigue en la ficha modal.

La explicación del veredicto no se recorta, por lo que sus salvedades siguen visibles. Los detalles permanecen accesibles con ratón y teclado. Las referencias pueden abrir también fuentes cuyas tarjetas estén plegadas.

## Validación
- Compilación TypeScript y Vite correcta.
- Cuatro escenarios replay completados en Java y navegador sin consumo OpenAI, con recarga y presentación móvil.
- Verificadas jerarquía del veredicto, cuatro tarjetas iniciales, acceso a las fuentes adicionales, recuperación del contexto desplegable y orden móvil.
- Ejemplo sintético de varias afirmaciones: desglose conservado, sin desbordamiento horizontal y desplegable operable con teclado.
- Capturas y resultados: POC/webapp/review/integration/verdict-first.
- Scripts: POC/webapp/scripts/verify-replay.cjs y verify-result-layout.cjs. PLAYWRIGHT_MODULE permite indicar la ubicación de Playwright.

## Revisión pendiente
El usuario debe revisar esta presentación antes de la segunda fase. Las capturas originales de OpenAI no se han modificado. Sus textos aún pueden repetir ideas y ser extensos: la concisión del mockup se realizó manualmente y no se imita recortando respuestas en el frontend.

La segunda fase, pendiente, ajustará la redacción del agente para que explicación, síntesis y soporte documental sean breves y complementarios. Requerirá una prueba real nueva guardada como otro escenario.

## Fase 2: contrato editorial implementado localmente

Java antepone solo la frase de predominio de respaldo o cuestionamiento a la explicacion integra del agente. No recorta, reescribe ni elimina frases por similitud. Las reglas de clasificacion y el contrato JSON v3 permanecen iguales.

Las instrucciones activas se encuentran en POC/cross-check-service/infrastructure/src/main/resources/openai/unified/instructions.md. Cada campo tiene un proposito:

| Campo | Contenido | Objetivo de longitud |
|---|---|---|
| title | Tema sin repetir la conclusion | 8-14 palabras |
| context | Proposicion, periodo y alcance | 1-2 frases |
| claim.verdict.explanation | Conclusion documental, motivo y salvedad esencial | 1-2 frases |
| summary | Hechos o argumentos decisivos complementarios | 2-3 frases |
| claim.verdict.documentarySupport | Desarrollo de las evidencias y lagunas | Un parrafo breve |
| limitations | Limitaciones concretas | Una frase por entrada |
| sources[].contribution | Aportacion especifica de la fuente | 1-2 frases |

Son objetivos editoriales, no limites de validacion o recortes. Una salvedad esencial puede repetirse cuando sea necesaria para evitar una conclusion enganosa. El volumen de investigacion y las reglas de atribucion no se reducen.

La explicacion documental insuficiente debe decir que no puede establecerse. El agente no anuncia en ese campo el predominio de publicaciones: Java aporta esa frase al derivar el veredicto final. Para varias afirmaciones se conservan sus conclusiones individuales.

Estado al 13/10/2026: instrucciones publicadas y verificadas como political-v20; nueva captura PLUS_ULTRA_V20 guardada sin modificaciones. Los replay historicos permanecen intactos. La validacion editorial detecta repeticion residual en la sintesis y enfasis en cobro personal pese al alcance mediante terceros; consultar POC/cross-check-service/docs/openai/political-v20-replay-review.md.

Validacion local del 09/10/2026: Maven test correcto, 305 pruebas registradas, 303 superadas y 2 omitidas; cero fallos y errores. Incluye composicion de ambos veredictos editoriales, conservacion de matices y referencias, distribuciones de posiciones y contrato HTTP. git diff --check correcto.

Validacion final 13/10/2026: Maven verify correcto (306 pruebas, 304 superadas y 2 omitidas). PLUS_ULTRA_V20 completa Java y navegador con SUPPORTED_BY_PUBLICATIONS; recarga, referencias y vistas web/movil verificadas. Capturas en POC/webapp/review/integration/verdict-first.

## Publicacion editorial political-v21 (13/10/2026)

Publicadas y verificadas por lectura las instrucciones sin referencias a Java ni a desplegables. Se actualizaron solo las instrucciones remotas; nombre, modelo y contrato se conservaron. Revision local actualizada a political-v21 para identificar las nuevas conversaciones. No se ejecuto inferencia ni se modifico el replay PLUS_ULTRA_V20, que corresponde a la revision anterior.
