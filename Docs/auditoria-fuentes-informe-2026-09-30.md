# Auditoría de las cinco fuentes del informe real — 30/09/2026

Estado: auditoría terminada; ajustes del agente propuestos, todavía sin implementar ni publicar.

Informe revisado: [resultado conservado](../POC/webapp/review/integration/async-live-20260930.json), trabajo `81a2eac9-9776-42e6-be25-a0984fd76982`, revisión `political-v9`.

Pregunta: «¿Los límites al precio del alquiler reducen la oferta de vivienda en alquiler en España? Analiza el periodo 2023–2025 y contrasta publicaciones con distintas posiciones.»

## Conclusión de la auditoría

La etiqueta INSUFFICIENT_EVIDENCE sigue siendo una salida prudente para esta muestra, pero no valida el razonamiento que la acompaña. El informe mezcla evidencia de una regulación anterior con el periodo solicitado, no distingue suficientemente indicadores de oferta y no explica un resultado que matiza su síntesis. Hay además dos fechas incorrectas y otras dos fechas completas sin respaldo suficiente en los documentos consultados.

No procede sustituir automáticamente la valoración por SUPPORTED o REFUTED, ni calcular ahora porcentajes de posicionamiento. Primero hay que corregir la justificación y evaluar las publicaciones individualmente. Esta revisión no es una investigación exhaustiva del mercado español ni una comprobación de los microdatos de los estudios.

## Revisión por fuente

| Fuente del informe | Comprobación en el original y localizador | Problema e impacto | Tratamiento propuesto |
|---|---|---|---|
| **S1 · BOE, Ley 12/2023** | La cabecera del BOE indica publicación el **25/05/2023**; el 24 es la fecha de la ley. | `publishedAt=2023-05-24` confunde ambas fechas. Su uso como marco normativo, sin atribuirle medición de resultados, es correcto. | Corregir a `2023-05-25`; conservar como contexto, no como voto editorial. [Original](https://www.boe.es/buscar/doc.php?id=BOE-A-2023-12203). |
| **S2 · OCDE, Spain 2025** | P. impresa 39, página 41 del PDF: la afirmación sobre retirada de viviendas remite a la regulación de **2020**. La referencia [20], p. 58, es Monràs y García-Montalvo (2022). La ficha editorial fecha el informe el **26/11/2025**. | La contribución omite el periodo de la evidencia citada y favorece su lectura como evaluación de 2023–2025. `2025-11-01` es incorrecto. No aporta una segunda estimación independiente por citar el mismo trabajo de base. | Corregir fecha; identificar el pasaje como antecedente. Evaluar por separado cualquier argumento propio de la OCDE sobre la política actual. [PDF](https://www.oecd.org/content/dam/oecd/en/publications/reports/2025/11/oecd-economic-surveys-spain-2025_cd5c7d04/abc5c435-en.pdf) · [Ficha](https://www.oecd.org/en/publications/oecd-economic-surveys-spain-2025_abc5c435-en.html). |
| **S3 · Monras y Montalvo, FRBSF** | Portada: versión septiembre de 2023, sin día. Sección 3, p. impresa 9/página 11 del PDF: datos de **2016 a junio de 2021**. Analiza la regulación catalana introducida en septiembre de 2020. | La contribución resume su resultado, pero omite que no observa 2023–2025. El informe lo usa en el respaldo documental del periodo posterior. El día `01` no queda acreditado por la portada. | Mantener como antecedente explícito, no prueba directa del periodo solicitado. `publishedAt=null` mientras no se verifique el día. FRBSF es el editor; no convertir el argumento de sus autores en postura institucional. [PDF, portada y sección 3](https://www.frbsf.org/wp-content/uploads/wp2023-28.pdf). |
| **S4 · O-HB, seguimiento de Barcelona** | Noticia de **30/10/2025**, sobre el primer trimestre de 2025: nuevos contratos prácticamente estables, aumento del alquiler de temporada y saldo positivo de contratos activos (+423). | La contribución individual es razonable. Sin embargo, se cita S4 junto a la síntesis negativa sin explicar qué dato aporta, cuál la matiza y qué indicador mide. No demuestra por sí sola un efecto causal favorable ni desfavorable. | Incorporar el matiz y distinguir flujo de contratos, contratos activos y alquiler temporal. Mantener su alcance local. No asignar QUESTIONS o MIXED automáticamente por la coexistencia de indicadores. [Noticia original](https://www.ohb.cat/en/presenting-the-results-of-the-latest-monitoring-report-on-barcelonas-zmrt/). |
| **S5 · Fernando Pinto, FEDEA, ap2026-15** | Texto fechado **junio de 2026**. Secciones 5, 6.3 y 7: comparación descriptiva, con resultados de 2024 T1–2025 T4; no una estimación causal. Sección 6.2 distingue anuncios de stock arrendado. | Es una retrospectiva potencialmente pertinente. `2026-09-29` no se acredita en el texto. Reducir anuncios no equivale automáticamente a reducir viviendas arrendadas; el propio documento contempla esa diferencia. | Conservar con sus límites, atribuido al autor. Usar `publishedAt=null` sin día verificable. La postura argumentada puede examinarse para posicionamiento aunque la evidencia no pruebe causalidad. [Texto, §§6.2 y 7](https://documentos.fedea.net/documento/ap2026-15/texto). |

## Qué debe cambiar en el razonamiento

**La fecha del documento no actualiza sus datos.** Una referencia moderna que reproduce un estudio antiguo hereda el periodo de ese estudio para ese argumento. Tampoco equivale a corroboración empírica independiente. El problema debe detectarse antes de escribir la síntesis, no limitarse a anotar una advertencia al final.

**“Oferta” necesita una definición operativa.** Anuncios disponibles, nuevas firmas, contratos activos y vivienda destinada al alquiler son medidas diferentes. Deben conservar su nombre, periodo y ámbito en cada afirmación. Una diferencia entre indicadores no es necesariamente una contradicción entre estudios, ni demuestra por sí sola causalidad.

**Las citas deben explicar su función.** Un conjunto de IDs al final de una conclusión no permite saber qué fuente la apoya, cuál la matiza o cuál solo contextualiza. La síntesis debe expresar esas relaciones, evitando presentar toda la lista como respaldo uniforme.

**Evidencia y postura son decisiones diferentes.** Una publicación puede sostener un argumento claro sin probarlo causalmente. A la inversa, un indicador administrativo no es automáticamente una postura editorial. La etiqueta final prudente no exime de clasificar correctamente las publicaciones que sí sean elegibles.

## Posicionamiento: qué podemos concluir y qué no

El resultado entrega UNAVAILABLE, sin unidades ni exclusiones. Su razón exige una muestra homogénea para toda España. Esa exigencia es más fuerte que describir una muestra no representativa y puede vaciar innecesariamente el panel.

Propuesta de criterio: una pregunta sobre España no significa que cada publicación deba estudiar todas las regiones. Importan la proposición que sostiene la publicación y la medida realmente aplicada. Sin embargo, un resultado local que no aborda la proposición general tampoco debe marcarse MATCH para poder contarlo. La cobertura territorial y la coincidencia argumental se deben justificar, no asumir.

Para este informe:

- El material normativo queda como contexto documental.
- Los antecedentes no se convierten en posiciones sobre el periodo posterior por su fecha de edición.
- El seguimiento estadístico requiere identificar una postura propia antes de asignar apoyo o cuestionamiento.
- El ensayo descriptivo requiere resolver si su argumento coincide con el significado de oferta y el alcance de la proposición; no se excluye solo por no ser causal.

No asigno porcentajes ni una valoración derivada de publicaciones en esta auditoría: falta esa evaluación trazable. Para SINGLE, cuando se hayan evaluado candidatos, conviene conservar los EXCLUDE con su razón y distinguir muestra evaluada sin unidades contables de imposibilidad real de evaluación. Esto utiliza campos ya existentes; no obliga a cambiar Java ni la ubicación del panel.

## Ajustes concretos propuestos para las instrucciones

Las instrucciones actuales ya prohíben inventar fechas, contar antecedentes y confundir citas de terceros. No basta con repetir esas prohibiciones: propongo convertirlas en una secuencia de trabajo verificable.

1. Antes de sintetizar, registrar para cada argumento: autor, localizador leído, versión, periodo de datos, territorio, política, indicador, método y conclusión atribuida. Son pasos de investigación; no nuevos campos obligatorios del JSON en esta fase.
2. Resolver por separado precisión de fecha y pertinencia temporal. Si solo se conoce mes/año, conservar ese dato en la explicación y devolver null donde se exige una fecha completa.
3. Seguir las referencias de segunda mano relevantes y conservar su periodo. Distinguir versiones de un estudio y dependencia de evidencia de la deduplicación editorial de reproducciones.
4. Etiquetar el uso como evidencia del periodo, retrospectiva, antecedente o contexto. Un antecedente no puede reaparecer como prueba actual en summary o documentarySupport.
5. Conservar el indicador medido y el límite del método: descripción, asociación, estimación causal o argumento. No convertir anuncios en stock ni posibilidad en hecho observado.
6. Buscar resultados que matizan la conclusión y explicarlos con simetría. No forzar un equilibrio artificial ni una categoría MIXED.
7. Evaluar postura propia independientemente de suficiencia causal; justificar coincidencia de alcance sin exigir representatividad nacional a cada pieza. No relajar MATCH ni fabricar posturas para llenar la muestra.
8. Revisar cada frase relevante contra sus fuentes. Las referencias deben respaldar esa frase o quedar identificadas expresamente como matiz/contexto.
9. Usar etiquetas españolas en la prosa; reservar enums para los campos de estado. La síntesis original expone INSUFFICIENT_EVIDENCE como texto al usuario.

## Casos de aceptación para la siguiente fase

| Caso | Comportamiento esperado |
|---|---|
| Ley fechada el día anterior a su publicación | Diferenciar fecha de la norma y publishedAt. |
| Edición con solo mes/año | No completar artificialmente el día. |
| Informe nuevo que cita evidencia de una política antigua | Conservar el periodo del argumento original y señalar dependencia. |
| Menos anuncios y más contratos activos | Distinguir indicadores; no declarar contradicción o causalidad automáticamente. |
| Ensayo con postura propia y método descriptivo | Separar elegibilidad de la postura de fuerza probatoria. |
| Publicación local ante pregunta nacional | Justificar el alcance; ni exclusión por geografía solamente ni generalización automática. |
| Muestra evaluada sin candidatos elegibles | Explicar exclusiones y ausencia de recuento, sin inventar neutralidad. |
| Fuente que matiza una síntesis | Incorporar el matiz o retirar la cita que aparenta apoyo. |

Estas son expectativas de evaluación semántica, no pruebas automáticas ya ejecutadas.

## Límites y cierre de fase

Se leyeron las cinco URLs citadas, los pasajes relevantes de los dos PDF y las fichas de apoyo indicadas. El PDF completo enlazado desde el proyecto O-HB no pudo recuperarse por timeout: la auditoría de S4 se apoya en la noticia oficial que el agente citó y en la descripción del proyecto. La ficha de FEDEA enlazada desde el texto devolvió página no encontrada, aunque el texto citado sí fue accesible. Por ello no se certifica una fecha exacta de publicación de S5.

Los enlaces se comprobaron el 30/09/2026; no se infiere que su contenido sea inmutable. No se reprodujeron los análisis estadísticos ni se amplió la búsqueda a toda la evidencia disponible. La auditoría comprueba fidelidad y alcance de estas referencias, no dicta una conclusión definitiva sobre la política de alquileres.

No se han modificado el agente, sus instrucciones activas, la revisión de conversación, el contrato, Java o el frontend. No se ha ejecutado otra inferencia de pago. Siguiente fase propuesta: preparar un cambio acotado de instrucciones en inglés y ejemplos de aceptación para revisión antes de publicarlo.
