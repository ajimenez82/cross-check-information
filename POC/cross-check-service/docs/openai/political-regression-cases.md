# Casos de revisión del agente político

Estos casos sintéticos permiten revisar las reglas sin inferencias de pago. Son expectativas de evaluación, no resultados obtenidos del modelo. Las pruebas Java verifican contratos y lógica determinista; no demuestran que el agente aplique correctamente estas reglas. La comprobación semántica real queda pendiente.

| Caso | Datos del caso | Resultado esperado |
| --- | --- | --- |
| Causalidad no identificada | Se pregunta si una medida reduce la oferta en España. Solo hay correlaciones regionales y explicaciones alternativas. | INSUFFICIENT_EVIDENCE; no MISLEADING por falta de prueba causal ni por cobertura territorial limitada. |
| Distorsión demostrada | Se afirma que el total cayó a la mitad, pero el documento citado dice que pasó de 100 a 110; lo que se redujo a la mitad fue el crecimiento anual, de 20 a 10. | REFUTED si se evalúa literalmente la caída del total; MISLEADING solo si la proposición parcialmente cierta confunde expresamente crecimiento y nivel. Explicar magnitudes y evidencia, sin elegir por palabras clave. |
| Variable distinta | La proposición habla de oferta; el artículo únicamente cuestiona que los topes reduzcan picos de precios. | Excluir de esa muestra por alcance distinto; no QUESTIONS sobre oferta. |
| Sin postura | Un artículo describe el debate sobre oferta, sin adoptar una posición propia. | NO_EXPLICIT_POSITION, sin atribuirle las opiniones citadas. |
| Matiz no mixto | Un artículo niega caída de oferta total y menciona crecimiento del alquiler temporal. | QUESTIONS respecto a oferta total; el matiz por sí solo no justifica MIXED. Aclarar si la pregunta se refiere a oferta habitual. |
| Posición realmente mixta | Un análisis presenta argumentos propios a favor y en contra del efecto sobre la misma variable, periodo y territorio, sin resolverlos. | MIXED, explicando ambos argumentos sobre la misma proposición. |
| Fuente retrospectiva | Página fechada 16/03/2026 que analiza datos de 2024–2025, sin corte histórico exigido por el usuario. | publishedAt 2026-03-16 y alcance retrospectivo explícito. No describir la publicación como de 2024–2025. |
| Corte histórico | La misma página, pero el usuario pide solo información disponible al terminar 2025. | Excluir del análisis con ese corte; no anticipar conocimiento. |
| Fecha desconocida | Una publicación pertinente no muestra fecha verificable. | publishedAt null y advertencia sobre ventana temporal incompleta; no inventar una fecha ni un periodo en la prosa. |
| Referencias e idioma | La herramienta devuelve turn5view0; el informe incluye una conclusión de alcance nacional. | Relabelar como S1 en todas sus referencias y escribir «para toda España». Preservar títulos originales, URL y códigos técnicos. |

Revisión local de political-v5: los diez casos tienen reglas explícitas en las instrucciones. Esto valida la cobertura del documento, no predice las respuestas del modelo. Antes de considerar superada la evaluación real, contrastar también la clasificación con el texto completo de las fuentes y no solo con la explicación generada.
