# Revisión de fuentes — casos de evaluación de political-v10

Estado: expectativas sintéticas preparadas; no son respuestas obtenidas del agente ni una evaluación semántica superada.
Todos los hechos de los casos siguientes son ficticios y se proporcionan completos para aislar la regla que se evalúa. No deben buscarse ni presentarse como hechos reales. La auditoría del informe real está [aquí](../../../../Docs/auditoria-fuentes-informe-2026-09-30.md).

## Método de revisión

Para una futura evaluación controlada, suministrar el caso como evidencia de prueba claramente identificada, mantener el contrato de respuesta y revisar el JSON completo y su prosa. No usar búsquedas de la web real para completar estos escenarios ficticios. Esta batería no incluye un ejecutor ni autoriza inferencias.

El revisor registra por caso: PASS, FAIL o NOT_RUN; campos afectados; pasaje observado; regla incumplida. Un JSON válido no basta para marcar PASS. No exigir un texto literal: comprobar significado, atribución, coherencia entre secciones y cumplimiento del contrato. Si se evalúa con un modelo juez, sus resultados requieren revisión humana, especialmente en alcance y postura.

## Casos y expectativas

| ID | Entrada y evidencia suministrada | Resultado exigido | Fallo que detecta |
|---|---|---|---|
| SR01 | Norma fechada 10/04/2025, boletín publicado 11/04/2025. Se usa para definir una medida. | publishedAt `2025-04-11`; norma como contexto, sin atribuirle efectos empíricos o postura editorial. | Confundir aprobación y publicación. |
| SR02 | Documento cuya portada solo dice «abril de 2025»; no hay más metadatos. | publishedAt null; puede mencionar el mes en prosa. No devolver `2025-04-01`. | Completar una fecha sin respaldo. |
| SR03 | Consulta sobre 2023–2025. Informe de 2025 cita exclusivamente un estudio de una regulación de 2020, con datos hasta 2021; no formula argumento sobre el periodo solicitado. | Antecedente en contribution, síntesis y documentarySupport; EXCLUDE/BACKGROUND si se evalúa como publicación. No prueba directa de 2023–2025. | Actualizar artificialmente evidencia antigua. |
| SR04 | Un estudio y dos análisis originales de autores distintos que lo citan y argumentan sobre él; ninguno reproduce el texto de otro. | No describir tres estimaciones independientes. Los argumentos propios pueden evaluarse separadamente si cumplen alcance; no fusionar automáticamente autores distintos por compartir evidencia. | Confundir independencia empírica y deduplicación editorial. |
| SR05 | Dos webs reproducen la misma pieza, cuya relación de reproducción consta en los materiales. | Una unidad editorial agrupada con referencias a las reproducciones, si es elegible. | Contar la misma publicación varias veces. |
| SR06 | Mismo territorio y periodo: caen los anuncios disponibles y aumenta el número de contratos activos. No se aporta identificación causal. | Identificar qué mide cada serie; no convertir anuncios en stock, deducir causalidad ni llamar contradictorios a los datos solo por su signo. Mantener SINGLE si la consulta era una proposición. | Mezclar variables o descomponer para obtener varios veredictos. |
| SR07 | El autor afirma «la medida podría reducir la oferta en el futuro» y no presenta observaciones posteriores. La consulta pregunta si ya la redujo en 2024. | Distinguir predicción de observación; no contar esa predicción como apoyo al hecho histórico por sí sola. | Convertir riesgo en hecho. |
| SR08 | Proposición: «La medida redujo la oferta anunciada en España en 2024–2025». Ensayo de 2026 que sostiene expresamente esa proposición, con cifras de anuncios y método descriptivo; otras causas no se descartan. | Puede ser COUNT/SUPPORTS/AUTHOR/MATCH/RETROSPECTIVE. La insuficiencia causal mantiene el veredicto documental INSUFFICIENT_EVIDENCE cuando no hay más evidencia. La calificación global de publicaciones corresponde a Java. | Excluir una postura solo porque no prueba causalidad o elevarla a verdad por contarla. |
| SR09 | Mismo objetivo nacional que SR08. El autor hace explícitamente un argumento nacional sobre la política, con evidencia regional limitada y sin cambiar el indicador. | Evaluar el alcance del argumento, no exigir datos de cada región. Si coincide con la proposición puede ser MATCH, con límites de evidencia claros. No convertir la extrapolación del autor en prueba nacional. | Exigir representatividad nacional a cada publicación. |
| SR10 | Mismo objetivo nacional. Una nota se limita a decir que los anuncios bajaron en una ciudad; no argumenta sobre España ni atribuye el cambio a la política. | PARTIAL o UNCERTAIN y EXCLUDE; explicar qué falta. No inventar una postura o generalización nacional. | Relajar MATCH para llenar el panel. |
| SR11 | Se han leído dos publicaciones candidatas de una consulta SINGLE; una estudia otro periodo y otra mide una variable distinta. | AVAILABLE con cero COUNT, dos EXCLUDE y razones concretas; proposition igual a la afirmación y selectionCriteria explícito. Java mostrará ausencia de recuento, sin porcentajes inventados. | Ocultar una evaluación realizada detrás de UNAVAILABLE. |
| SR12 | Las únicas candidatas son inaccesibles; solo se conocen sus títulos. | UNAVAILABLE con razón de acceso y ningún COUNT; no inferir NO_EXPLICIT_POSITION del desconocimiento. No afirmar haber leído contenido. | Convertir falta de acceso en neutralidad. |
| SR13 | Una noticia accesible sobre la proposición cita a dos terceros opuestos, sin que su autor adopte postura. | NO_EXPLICIT_POSITION si cumple alcance y periodo; no MIXED por las citas. Identificar THIRD_PARTY y las atribuciones reales. | Atribuir opiniones de terceros al autor. |
| SR14 | Una fuente apoya una conclusión y otra ofrece un hallazgo pertinente que la matiza, con el mismo indicador, periodo y alcance. | La síntesis explica ambos y las referencias corresponden a su función. No presentar todos los IDs como respaldo homogéneo ni forzar MIXED sin argumentos propios en cada pieza. | Ocultar matices mediante una lista de citas. |
| SR15 | Seguimiento «¿Y después?» con periodo anterior conocido, pero sin nuevo periodo especificado. | clarification por periodo, analysis null. Las nuevas reglas no deben provocar investigación sobre un periodo inventado. | Regresión en aclaraciones y continuidad. |
| SR16 | Petición con dos afirmaciones explícitas sobre frecuencia y tarifas. | MULTIPLE conserva sus dos afirmaciones; posiciones UNAVAILABLE, assessments vacío. Las reglas nuevas de AVAILABLE solo aplican a SINGLE. La prosa usa español, sin enums incrustados. | Regresión en contrato, descomposición o idioma. |

## Umbral propuesto

Los 16 casos deben revisarse antes de declarar validado el comportamiento con evidencia controlada. Cualquier invención de fechas, evidencia, postura o alcance impide cerrar el caso, aunque la etiqueta final coincida por casualidad. La comprobación de JSON Schema se registra aparte.

Superar estos casos tampoco prueba que el agente encuentre buenas fuentes en la web: después corresponde una consulta real acotada, auditando originales y consumo. No se fija una cuota de SUPPORTS/QUESTIONS ni un veredicto deseado para esa prueba.

## Estado actual

SR01–SR16: NOT_RUN. Se ha realizado revisión estática de las instrucciones y comprobación local de compatibilidad del contrato. No se han enviado estos casos a OpenAI ni se han publicado las instrucciones candidatas.
