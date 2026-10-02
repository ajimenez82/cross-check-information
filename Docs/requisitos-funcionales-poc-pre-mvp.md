# Requisitos y funcionalidad — PoC inicial (pre-MVP)

Actualización political-v15: una publicación sin postura explícita puede contar si tiene hallazgos aceptados y cumple alcance, periodo y demás criterios editoriales. No se exige causalidad demostrada ni postura propia como condición general. La aplicación suministra esta explicación en los criterios de selección, sin códigos internos. [Pruebas](../POC/cross-check-service/docs/openai/political-v15-review.md).

Actualización political-v14: explicaciones en español sin códigos internos; documentos inaccesibles identificados como localizados, no verificados. La ausencia de causalidad no basta para excluir una postura editorial. Se mantienen los criterios de alcance, periodo, atribución y tipo de fuente. [Implementación y límites](../POC/cross-check-service/docs/openai/political-v14-review.md).

Actualización political-v13: la síntesis debe explicar siempre el resultado de la muestra de publicaciones, incluido por qué ninguna cuenta si todas quedan excluidas. No se sustituye esa explicación por texto generado automáticamente en Java. La síntesis real sobre evidencia guardada pasó con INSUFFICIENT_EVIDENCE y cero publicaciones contables; quedan aspectos editoriales y semánticos por revisar. [Contrato y pruebas](../POC/cross-check-service/docs/openai/political-v13-review.md).

Actualización political-v12 (01/10/2026): las publicaciones sin hallazgos aceptados no reciben valoraciones de postura. Las fuentes de contexto fuera del periodo pueden citarse en el análisis, pero quedan excluidas del porcentaje; NO_EXPLICIT_POSITION no elimina ese requisito. Las referencias de síntesis son IDs de hallazgos, no de fuentes. [Validación offline y límites](../POC/cross-check-service/docs/openai/political-v12-review.md).

## Actualización implementada — 01/10/2026

El análisis político realiza investigación, captura independiente y síntesis por etapas. Solo los fragmentos encontrados en los documentos capturados se convierten en hallazgos; documentos inaccesibles quedan como limitación. Una URL exacta repetida no aporta votos duplicados. La síntesis no puede crear fuentes ni fechas y debe referenciar los IDs aceptados. Se mantienen las valoraciones acordadas y la ubicación del posicionamiento de publicaciones en la interfaz.

Una fuente de contexto fuera del periodo elegible no se cuenta como postura, aunque no tenga posición explícita. Sin hallazgos aceptados se devuelve un fallo técnico antes de sintetizar; una salida inválida tampoco se presenta como «evidencia insuficiente». **La prueba real v11 no está aprobada:** el proveedor incumplió esas restricciones y el servicio rechazó su respuesta. [Resultado y límites](../POC/cross-check-service/docs/openai/political-v11-live-review.md). Las comprobaciones textuales no sustituyen la evaluación semántica.

- **Versión:** 1.2
- **Fecha:** 16 de septiembre de 2026
- **Estado:** alcance acordado para la primera fase.
- **Nombre definitivo:** pendiente. Los mockups utilizan «Contrasta», con «CrossCheck» como denominación secundaria y el lema «Información contrastada. Evidencias a la vista.». No implica disponibilidad de marca o dominio.
- **Referencia visual:** [desktop v4](../Mockups/desktop-v4.png) y [móvil v4](../Mockups/mobile-v4.png).

## 1. Descripción del proyecto

Aplicación web responsive con interfaz de conversación que permite plantear preguntas, aportar afirmaciones o introducir enlaces para contrastar información mediante un agente especializado de IA conectado a OpenAI.

El objetivo de esta PoC es validar el recorrido completo: entrada del usuario, ejecución del agente, recuperación y contraste de fuentes, respuesta estructurada y presentación de conclusiones e indicadores visuales.

El elemento diferencial es la **recuperación y trazabilidad de evidencias**. Cada valoración debe explicar qué la sustenta, qué fuentes se han utilizado y cuáles son sus límites. Una respuesta convincente o una mayoría de publicaciones no bastan para demostrar una afirmación.

La aplicación debe distinguir hechos documentados, declaraciones atribuidas a terceros, hipótesis, interpretaciones y opiniones. Una fuente que acredita una declaración no demuestra necesariamente la veracidad de lo declarado.

## 2. Alcance de la primera fase

### Incluido

- Interfaz compatible con escritorio y móvil.
- Selector de categoría visible, con una sola opción: **Análisis político**.
- Entrada de texto y enlaces.
- Agente especializado guardado en OpenAI y sesiones independientes por conversación.
- Preguntas de seguimiento que conservan el contexto de su conversación.
- Resultado con contexto, síntesis, valoración final, fuentes y posiciones en publicaciones cuando exista una clasificación suficiente para calcularlas.
- Nueva conversación e historial local.
- Usuario simulado **Ale Jiménez**, perfil de demostración y preferencias visuales.
- Páginas informativas, explicación de metodología, privacidad y políticas de uso.
- Estados de carga y error.
- Sin base de datos propia ni almacenamiento de archivos en el servidor.

### Fuera de alcance

- Autenticación real, registro, recuperación de contraseña y cuentas.
- Sincronización de historial entre dispositivos.
- Categorías adicionales operativas.
- Subida de documentos, imágenes, audio o vídeo.
- Base vectorial y corpus documental propio.
- Compartir conversaciones, exportar informes, favoritos, suscripciones y administración.
- Recuperación por un endpoint propio `GET /api/analysis/{id}`.
- Investigación o almacenamiento documental independientes del agente en el backend.

El PDF utilizado para elaborar los mockups es material de diseño. No implica que el producto permita adjuntar PDFs en esta fase.

## 3. Categorías

Cada categoría se asociará a un agente con instrucciones, criterios de contraste y forma de presentar los resultados adaptados a su ámbito.

| Categoría | Descripción | Disponibilidad |
|---|---|---|
| **Análisis político** | Contrasta afirmaciones y declaraciones políticas, aporta contexto y distingue hechos, posiciones e interpretaciones. Incluye el análisis de afirmaciones sobre causas judiciales vinculadas a la política, respetando estados procesales y evitando equiparar investigación con culpabilidad. | Única categoría activa |
| **Noticias falsas y bulos** | Examina noticias virales, cadenas y contenidos potencialmente inventados, manipulados o fuera de contexto; busca su origen y evidencias que los respalden o contradigan. | Futura |
| **Mitos y conspiraciones** | Examina mitos populares, leyendas urbanas y teorías conspirativas. Investiga su origen y contrasta las evidencias disponibles, distinguiendo hechos documentados, especulaciones y afirmaciones no verificables. | Futura |
| **Sociedad, medios y entretenimiento** | Analiza audiencias de televisión, fenómenos sociales, celebridades y afirmaciones sobre contenidos culturales. Distingue datos verificables, declaraciones e interpretaciones. | Futura |
| **Consumo, ocio y viajes** | Contrasta comparaciones de productos, restaurantes, campings, alojamientos y servicios. Diferencia características comprobables, reseñas y preferencias; explicita los criterios de cada recomendación. | Futura |
| **Estafas, timos y publicidad engañosa** | Analiza ofertas sospechosas, suplantaciones, mensajes fraudulentos y promesas comerciales. Incluye consultas sobre posibles llamadas fraudulentas, sin una categoría telefónica propia. Distingue señales de riesgo de engaños documentados y evita convertir la ausencia de alertas en garantía de seguridad. | Futura |
| **Salud y bienestar** | Examina afirmaciones sobre alimentación, suplementos, tratamientos y hábitos, atendiendo a la calidad de la evidencia y sus límites. No sustituye una valoración médica individual. | Futura |
| **Ciencia, tecnología y medioambiente** | Contrasta afirmaciones sobre descubrimientos, capacidades de la IA, privacidad, energía y cuestiones ambientales, diferenciando evidencia, hipótesis y expectativas. | Futura |
| **Datos y hechos históricos** | Examina fechas, acontecimientos, citas atribuidas y relatos históricos, diferenciando hechos documentados, interpretaciones historiográficas y controversias. | Futura |
| **Deportes** | Contrasta resultados, estadísticas, récords y declaraciones deportivas, precisando competición, temporada, categoría y fecha. | Futura |
| **General** | Permite analizar consultas que no encajen en ninguna categoría especializada. Delimita la afirmación, aporta contexto y contrasta las fuentes disponibles; solicita criterios o aclaraciones cuando sean necesarios y expresa los límites de la conclusión. | Futura |

Las categorías futuras se documentan como evolución del producto; no se muestran como opciones seleccionables en esta PoC.

**Una conversación tiene una categoría inmutable. Cambiar de categoría requerirá una nueva conversación.**

### 3.1. Ejemplos de Sociedad y de Consumo, ocio y viajes

Los siguientes ejemplos ilustran consultas admitidas; no constituyen afirmaciones verificadas ni recomendaciones del producto:

- **Sociedad, medios y entretenimiento:** «El Hormiguero es el programa más visto en su franja horaria, por encima de La Revuelta». El análisis debe precisar fechas, franja coincidente y métrica de audiencia: espectadores, cuota u otra medida comparable.
- **Consumo, ocio y viajes:** «El camping Taiga Conil es más recomendable que el Camping Roche en Conil». El análisis debe explicitar los criterios de comparación —ubicación, instalaciones, precio, servicios o tipo de viaje— y separar características verificables de reseñas y preferencias.
- **Consumo, ocio y viajes:** «Casa Mané es uno de los mejores restaurantes de Cádiz». El análisis debe delimitar el ámbito geográfico y qué significa «mejor», diferenciando reconocimientos documentados, valoraciones de clientes y opinión subjetiva.

En comparaciones y recomendaciones, el agente no debe convertir una preferencia en una verdad universal. Si faltan criterios, los solicitará o hará explícitos los utilizados. Las reseñas no se considerarán automáticamente representativas ni equivalentes a evidencias de calidad objetiva. Si no procede un veredicto factual único, se utilizará una valoración matizada, interpretación u opinión, o sin veredicto único.

### 3.2. Uso de General

General será la opción de cobertura para asuntos no contemplados en las categorías especializadas. No activará una reclasificación automática de una conversación existente: se conserva la categoría seleccionada al iniciarla. Si el asunto requiere otra especialidad, podrá sugerirse abrir una nueva conversación en la categoría correspondiente.

### 3.3. Selección y solapamientos

Se elegirá la categoría temática cuando el asunto esté claro. «Noticias falsas y bulos» se orienta a verificar contenido viral de origen dudoso; «Mitos y conspiraciones», a relatos persistentes, leyendas urbanas y teorías conspirativas; «Estafas, timos y publicidad engañosa», a consultas centradas en un posible fraude o engaño comercial. «General» cubre asuntos sin encaje claro o cuya clasificación el usuario desconozca.

La separación entre Sociedad y Consumo permite especializar el análisis: comparar audiencias requiere métricas y periodos comparables; recomendar un establecimiento exige criterios y preferencias explícitos. La categoría elegida no predetermina el veredicto.

El catálogo previsto queda compuesto por **once categorías**. Esta revisión no amplía las categorías operativas de la PoC, que sigue incluyendo únicamente **Análisis político**; las otras diez quedan para fases posteriores.

## 4. Análisis desde el buscador o editor de consulta

El «buscador» es el editor de mensajes que inicia o continúa un análisis; no es un buscador web convencional con una lista de resultados.

### 4.1. Entrada

- Campo de texto que admite una afirmación, una pregunta abierta o uno o varios enlaces, acompañados o no de texto.
- Selector visible con «Análisis político».
- Acción «Analizar» o botón de envío.
- Ejemplos seleccionables en la pantalla inicial, sin ejecución automática hasta que el usuario envíe la consulta.
- Validación de entrada vacía y de límites de longitud configurables.
- Sin controles de adjuntos multimedia.

### 4.2. Flujo

1. El usuario inicia una conversación y envía su consulta.
2. React muestra el mensaje y un estado de carga.
3. Spring Boot valida la solicitud y la envía al agente configurado.
4. El agente analiza la consulta, busca fuentes, contrasta evidencias y genera el resultado.
5. Spring Boot comprueba el formato del resultado.
6. React representa la respuesta y la guarda en el historial local.
7. Los mensajes posteriores continúan la sesión de esa conversación.

Admitir un enlace no implica haber accedido a él. Si el agente no puede leer su contenido, debe indicarlo y no presentarlo como evidencia consultada.

### 4.3. Estados de interfaz

- Conversación vacía: instrucciones breves y ejemplos.
- Analizando: mensaje como «Analizando y consultando fuentes…».
- Resultado disponible.
- Error de conexión o del proveedor.
- Tiempo máximo superado.
- Sesión no disponible o referencia caducada.

Los fallos técnicos no se representarán como veredictos sobre la consulta. El usuario podrá reintentar de forma explícita; se evitarán reintentos automáticos que puedan duplicar ejecuciones y consumo.

En esta fase se espera el resultado completo mediante HTTP. La transmisión progresiva por SSE queda como evolución si las pruebas de latencia lo justifican.

## 5. Resultado del análisis

La respuesta tendrá cuatro campos principales acordados: `context`, `summary`, `sources` y `verdict`. Se añade el bloque de posiciones entre publicaciones, denominado en este documento `publicationPositions`.

Las etiquetas de la interfaz serán naturales y en español; no se mostrarán nombres técnicos de campos.

### 5.1. Título y fecha

- Título descriptivo y neutral del análisis.
- Fecha del análisis y fecha de corte cuando el contenido se limite a un periodo.
- Identificación del alcance de las evidencias cuando sea relevante.

El título no debe adoptar como hecho una acusación contenida en la pregunta.

### 5.2. Contexto — `context`

Antecedentes, fechas, circunstancias y conceptos necesarios para entender el asunto. Se podrá plegar si resulta extenso.

En asuntos judiciales se distinguirán investigación, acusación, resolución, recurso y firmeza cuando estén documentados.

### 5.3. Síntesis — `summary`

Resumen de lo contrastado, con referencias a las fuentes correspondientes. Debe separar:

- Hechos respaldados por documentación.
- Declaraciones y quién las formula.
- Hipótesis o alegaciones.
- Interpretaciones.
- Contradicciones y límites de lo que se puede concluir.

Cuando existan varias afirmaciones, el análisis debe tratarlas por separado y evitar que una conclusión global oculte sus diferencias.

### 5.4. Valoración final — `verdict`

Tarjeta destacada con **icono o bandera, color, etiqueta y explicación**. El significado no dependerá exclusivamente del color.

| Estado técnico | Etiqueta definitiva | Indicador | Criterio |
|---|---|---|---|
| `SUPPORTED` | Respaldada | Verde / círculo con comprobación | Las evidencias examinadas apoyan la afirmación dentro del alcance indicado. |
| `REFUTED` | Refutada | Rojo / círculo con cruz | Las evidencias examinadas contradicen la afirmación. |
| `MISLEADING` | Engañosa o fuera de contexto | Ámbar / advertencia | Hay elementos ciertos, pero se omite o altera contexto de forma que cambia su significado. |
| `INSUFFICIENT_EVIDENCE` | Evidencia insuficiente | Gris / círculo con interrogación | No hay base suficiente para respaldar o refutar; predomina mixta o sin posición explícita, hay empate en el primer puesto, o no hay unidades o clasificación disponible. |
| `OPINION` | Interpretación u opinión | Azul / bocadillo de diálogo | El contenido no puede valorarse directamente como verdadero o falso. |
| `NO_SINGLE_VERDICT` | Sin veredicto único | Gris azulado / caminos que se bifurcan | La pregunta abierta o la combinación de afirmaciones requiere una conclusión diferenciada. |

«Pendiente de contraste» es un estado de ejecución, no un veredicto final. `OPINION` tampoco sustituye al contraste de las afirmaciones factuales que pueda contener una opinión.

#### Cómo se determina

La valoración documental es un juicio cualitativo del agente sobre las evidencias, **no una media de opiniones ni un porcentaje de falsedad**.

El agente debe:

1. Delimitar la afirmación y su alcance.
2. Identificar evidencias pertinentes a favor y en contra.
3. Considerar procedencia, relación directa con la afirmación, independencia, fecha y contradicciones.
4. Diferenciar evidencias de inferencias.
5. Asignar un estado y justificarlo con referencias.
6. Expresar lo que no puede concluirse.

No se calcula una puntuación de certeza. La valoración documental se conserva; únicamente cuando es INSUFFICIENT_EVIDENCE se aplica la regla determinista de posicionamiento descrita a continuación para obtener la valoración final.

El objeto de valoración incluirá estado, explicación y referencias a las fuentes relevantes. React traducirá el estado a un indicador visual mediante reglas estables.


#### Valoraciones derivadas de publicaciones

| Estado técnico | Etiqueta definitiva | Indicador | Criterio |
|---|---|---|---|
| `SUPPORTED_BY_PUBLICATIONS` | Respaldada por las publicaciones | Verde suave / periódico | Evidencia insuficiente y SUPPORTS es la posición estrictamente mayor entre las cuatro. |
| `QUESTIONED_BY_PUBLICATIONS` | Cuestionada por las publicaciones | Coral suave / periódico | Evidencia insuficiente y QUESTIONS es la posición estrictamente mayor entre las cuatro. |

El catálogo definitivo contiene ocho estados: los seis documentales anteriores y estas dos valoraciones derivadas. Java compara recuentos de unidades deduplicadas, no fuentes individuales ni porcentajes redondeados. El mayor porcentaje no necesita superar el 50 %: 40/30/20/10 produce SUPPORTED_BY_PUBLICATIONS. Un empate en el primer puesto, predominio de MIXED o NO_EXPLICIT_POSITION, cero unidades o clasificación UNAVAILABLE mantiene INSUFFICIENT_EVIDENCE. Ningún otro estado documental se transforma.

En las dos valoraciones derivadas se muestra siempre: **«La evidencia disponible no permite respaldar ni refutar la afirmación»**, seguido del predominio en la muestra y la explicación documental original. Se conservan respaldo documental y referencias. El agente no emite los estados derivados: devuelve su valoración documental y las posiciones; Java aplica la regla. Una mayoría editorial no demuestra verdad ni falsedad.

Se conserva el panel de posiciones y su funcionamiento: a la derecha en escritorio y después del análisis y antes de las fuentes en móvil, como en v5. No se adopta la reorganización propuesta en v6.

#### Respaldo documental

Junto al veredicto se mostrará una descripción cualitativa del respaldo de la afirmación: por ejemplo, «Respaldo documental de la acusación general: insuficiente», seguida de una explicación.

El respaldo se refiere a **la afirmación concreta**, no a la credibilidad general de una persona, partido o institución.

#### Ejemplo de la v4

- **Veredicto:** bandera gris, «No acreditada con las evidencias examinadas».
- **Respaldo documental:** insuficiente para sostener la acusación general.
- **Límite:** no descarta irregularidades en casos concretos.

Este ejemplo resume el PDF aportado para el diseño, con corte 11/09/2026. No constituye una nueva comprobación del estado de las causas.

### 5.5. Fuentes — `sources`

Cada fuente incluirá:

- Identificador estable dentro del resultado.
- Título.
- URL.
- Publicación, organismo o autor, cuando conste.
- Fecha de publicación, cuando conste, y fecha de consulta.
- Breve explicación de lo que aporta.
- Tipo de fuente cuando sea útil: documento oficial, noticia, opinión, etc.

Las conclusiones enlazarán con sus referencias. El usuario podrá abrir las fuentes y comprender qué parte del análisis respaldan.

No se inventarán URLs, fechas ni citas. La falta de acceso se indicará. La repetición de una misma noticia no se tratará como confirmación independiente.

Una fuente puede aportar hechos sin adoptar ninguna posición sobre la tesis analizada.

## 6. Posiciones expresadas en las publicaciones analizadas

### 6.1. Qué mide

Este indicador mide **la posición expresada en las publicaciones examinadas respecto a una proposición concreta**.

No mide:

- Opinión pública.
- Posición de todos los medios.
- Probabilidad de verdad o falsedad.
- Culpabilidad.
- Grado de respaldo documental del veredicto.

Se hablará de **publicaciones**, no de «medios que creen». Un artículo no representa necesariamente la línea editorial del medio y citar una declaración no equivale a respaldarla.

### 6.2. Clasificación por el agente

Antes de clasificar, se explicitará la proposición evaluada. Por ejemplo: «Existe evidencia suficiente para sostener una acusación general de lawfare».

| Clase | Significado |
|---|---|
| **Respalda** | La publicación adopta argumentos a favor de la proposición. |
| **Cuestiona** | La publicación discute su fundamento o la suficiencia de las evidencias. |
| **Mixta** | La publicación presenta una posición argumentada en ambas direcciones sin una conclusión única. |
| **Sin posición explícita** | Informa o recoge declaraciones sin adoptar una posición propia. |

El agente leerá el contenido accesible y justificará la clasificación. No inferirá la posición únicamente del titular, del nombre del medio o de una declaración citada.

Si no hay acceso suficiente para clasificar una publicación, se excluirá del denominador y se explicará el motivo. No se confundirá inaccesibilidad con «sin posición explícita».

### 6.3. Muestra y duplicados

El bloque mostrará:

- Proposición evaluada.
- Periodo de las publicaciones y fecha de consulta.
- Criterio de selección o búsqueda.
- Número de unidades analizadas.
- Publicaciones excluidas y motivo, cuando las haya.
- Desglose por posición.

Las reproducciones de una misma pieza de agencia o contenido sustancialmente idéntico se agruparán como una unidad para no contarlas como voces independientes. Las piezas diferenciadas de una misma publicación podrán clasificarse por separado, haciendo visible la concentración de procedencia.

La muestra no se describirá como representativa de toda la prensa. No se seleccionarán fuentes para alcanzar un porcentaje deseado.

### 6.4. Cálculo

Sea:

- `N`: número total de publicaciones o grupos deduplicados leídos y clasificados.
- `n_respalda`, `n_cuestiona`, `n_mixta`, `n_sin_posicion`: recuentos de cada clase.

Se cumplirá:

```text
N = n_respalda + n_cuestiona + n_mixta + n_sin_posicion
Porcentaje de cada clase = 100 × recuento de la clase / N
```

Cada unidad contará una vez y tendrá el mismo peso en esta primera versión. Las publicaciones mixtas y sin posición permanecerán en el denominador.

El agente producirá la clasificación y su justificación. El porcentaje será una operación aritmética reproducible sobre esos recuentos; la interfaz podrá calcularlo para asegurar consistencia, sin realizar un segundo análisis editorial.

Se mostrarán siempre recuentos y denominador, además del porcentaje. Si se redondean las cifras, se evitará que el redondeo aparente una precisión inexistente.

Si `N = 0`, se mostrará «No calculable», nunca 0 %. Cuando no se haya realizado la clasificación, el bloque indicará «Medición no disponible». Una muestra pequeña se identificará con su tamaño; no se ha fijado todavía un mínimo de publicaciones.

### 6.5. Ejemplo ilustrativo aprobado

```text
Cuestiona: 8
Respalda: 1
Mixta: 0
Sin posición explícita: 1
Total: 10

Porcentaje que cuestiona = 8 / 10 × 100 = 80 %
```

Texto de presentación:

> 8 de las 10 publicaciones examinadas cuestionan que exista evidencia suficiente para sostener la acusación de lawfare.

No equivale a «el 80 % afirma que no existe lawfare».

En los mockups v4, estos números son ficticios y llevan la etiqueta **«Datos ilustrativos · no calculados del PDF»**. No se corresponden con las tres referencias documentales mostradas en las tarjetas.

### 6.6. Presentación

- Panel separado de la valoración final.
- Porcentaje destacado con una frase que nombre la posición medida.
- Barra con segmentos y leyenda de las cuatro clases.
- Acceso al desglose y a la justificación de cada clasificación.
- Nota de alcance sobre la muestra y su diferencia respecto a la opinión pública.

El modelo de resultado debe conservar identificadores de publicaciones y grupos, posición y explicación para permitir ese desglose. Las fuentes del análisis y las publicaciones clasificadas pueden solaparse, pero no se presuponen conjuntos idénticos.

## 7. Secciones y navegación

| Sección | Funcionalidad en esta fase |
|---|---|
| **Nueva conversación** | Crea una conversación vacía de Análisis político. Conserva las anteriores en el historial local. |
| **Historial** | Lista conversaciones con título y fecha. Permite abrir, renombrar, eliminar y borrar el historial local. |
| **Conversación** | Muestra mensajes, resultado estructurado, fuentes, valoración y preguntas de seguimiento. |
| **Mi perfil** | Muestra Ale Jiménez y «Usuario de demostración». No constituye una cuenta real. |
| **Preferencias** | Apariencia clara, oscura o según el sistema, conservada en el navegador. |
| **Acerca del proyecto** | Explica objetivo, alcance experimental y propósito de contraste con evidencias. |
| **Cómo funciona** | Explica agentes, fuentes, indicadores, limitaciones y cálculo de posiciones entre publicaciones. |
| **Políticas de uso** | Expone usos previstos, usos no admitidos y responsabilidades. Texto informativo pendiente de redacción final. |
| **Privacidad** | Explica datos locales, envío de consultas a OpenAI y alcance de las acciones de borrado. |
| **Ayuda y comentarios** | Preguntas frecuentes y acción para señalar problemas en respuestas. Sin servicio de recepción implementado, se identificará como demostración y no se simulará un envío exitoso. |

### 7.1. Menú de cuenta

Avatar y nombre en la esquina superior derecha. El menú agrupará:

- Mi perfil.
- Preferencias.
- Cerrar sesión, deshabilitado con «Disponible al incorporar autenticación».

En móvil podrá mostrarse solo el avatar en la cabecera, manteniendo accesible el nombre desde el menú.

### 7.2. Escritorio

- Cabecera con marca y menú de cuenta.
- Barra lateral con nueva conversación, historial y enlaces informativos al pie.
- Área central con conversación y editor.
- Panel de posiciones junto al análisis cuando haya ancho suficiente, como en la v4.

### 7.3. Móvil

- Menú lateral colapsado accesible mediante botón.
- Conversación en una columna.
- Tarjetas, fuentes y estadísticas adaptadas al ancho.
- Editor accesible al pie y lectura mediante desplazamiento.
- Indicadores con etiqueta textual, contraste y controles utilizables con teclado o tecnologías de asistencia.

### 7.4. Acciones sobre respuestas

- Copiar respuesta.
- Abrir fuentes.
- Consultar desglose de publicaciones.
- Señalar un problema, con el alcance de demostración indicado.
- Reintentar ante fallos técnicos.

No se incluyen exportación ni enlaces públicos para compartir.

## 8. Responsabilidades y restricciones técnicas acordadas

### Frontend: React y TypeScript

Gestiona interfaz, navegación, historial y preferencias locales. Presenta los indicadores mediante una correspondencia fija de estados. No contiene la clave de OpenAI.

### Backend: Java y Spring Boot

Clean Architecture, organización por vertical slices y separación conceptual de commands y queries. Únicamente se implementa el slice `start`.

Endpoint:

```http
POST /api/analysis/start
```

Cada petición inicia un análisis. Sin referencia de conversación, abre una sesión; con referencia, utiliza el contexto de la conversación existente.

Spring valida la solicitud, gestiona la comunicación con OpenAI y comprueba el formato de la respuesta. **No investiga ni decide si las afirmaciones son verdaderas.**

El contrato de aplicación para el proveedor de IA estará separado de su implementación OpenAI. No se crearán repositorios ni queries de persistencia sin necesidad.

### Agente en OpenAI

Se selecciona un agente guardado para Análisis político. Realiza la investigación, el contraste, la valoración y la clasificación de posiciones. Cada conversación mantiene su propia sesión.

Guardar la configuración y el historial en OpenAI simplifica las peticiones, pero no implica que el contexto deje de consumir tokens.

### Estado y persistencia

- Sin base de datos propia.
- Sin escritura de archivos de usuario o informes en el servidor.
- Historial y preferencias en `localStorage`, con límites configurables.
- Sesiones mantenidas por OpenAI.
- Referencia de continuación opaca, cifrada y autenticada por el backend, con caducidad.
- La referencia no constituye autenticación de usuario y debe tratarse como credencial de acceso a esa conversación.
- El historial no se sincroniza y puede perderse al borrar los datos del navegador.
- Borrar el historial local no equivale a borrar los datos del proveedor; la interfaz lo explicará.
- Los documentos de diseño y mockups del repositorio no son almacenamiento de archivos del producto.

### Configuración y límites

Clave y agente de OpenAI, secreto de referencias, origen del frontend y tiempo máximo mediante configuración del servidor. Límites configurables de entrada, concurrencia y frecuencia.

No se han fijado valores numéricos definitivos; se ajustarán mediante pruebas de la PoC. Una publicación de la demo sin autenticación deberá controlar el acceso y el consumo.

## 9. Criterios de aceptación funcional

1. Se puede usar la aplicación en escritorio y móvil.
2. El selector aparece y solo permite Análisis político.
3. Se envía texto o enlaces y se recibe una respuesta del agente mediante el backend.
4. Los seguimientos mantienen el contexto de la conversación y una nueva conversación no reutiliza su sesión.
5. Se muestran contexto, síntesis, valoración y fuentes cuando el análisis termina correctamente.
6. El estado visual coincide con el valor estructurado del veredicto y tiene etiqueta y explicación.
7. La ausencia de evidencias no se representa como falsedad demostrada.
8. Un error técnico no se presenta como conclusión del agente.
9. Las fuentes se identifican y vinculan a su aportación al análisis.
10. La medición de publicaciones muestra recuentos, denominador, criterio y desglose, o indica que no está disponible.
11. Los porcentajes coinciden con los recuentos; no se inventan cifras para completar la interfaz.
12. La medición de publicaciones no altera el respaldo documental; Java deriva las dos valoraciones editoriales únicamente desde INSUFFICIENT_EVIDENCE mediante predominio estricto, sin exigir superar el 50 %.
13. El historial sobrevive a una recarga en el mismo navegador y permite abrir, renombrar y eliminar conversaciones.
14. El perfil identifica a Ale Jiménez como usuario de demostración y cerrar sesión está deshabilitado.
15. Las páginas informativas son accesibles.
16. No se requieren base de datos, almacenamiento de archivos ni autenticación real.
17. La clave de OpenAI no se expone al navegador.

## 10. Validación de la PoC y evolución

La prueba evaluará utilidad del análisis, calidad y pertinencia de fuentes, consistencia de los indicadores, capacidad de reconocer incertidumbre, clasificación de publicaciones, tiempo de respuesta y consumo.

Los casos de prueba incluirán afirmaciones respaldadas, refutadas, descontextualizadas, opiniones, preguntas abiertas y falta de evidencias. La calidad factual deberá revisarse con casos de referencia; que el JSON sea válido no garantiza que el análisis sea correcto.

Las siguientes fases podrán incorporar autenticación, persistencia sincronizada, nuevas categorías y formatos multimedia. Esas ampliaciones no forman parte de esta entrega.


### Precisión temporal y coherencia del análisis

La proposición conserva su sentido afirmativo o negativo en contexto, veredicto y posiciones. Las normas y la información administrativa son evidencia documental, pero no se cuentan automáticamente como publicaciones con postura editorial. Si no se evalúa una muestra editorial, su clasificación es UNAVAILABLE.

Los campos consultedAt de fuentes y clasificación aceptan null cuando no se dispone de una hora real verificable, incluso con clasificación AVAILABLE. El frontend muestra «No disponible» en la fuente y omite la hora de la muestra; no se inventa medianoche. analyzedAt sigue siendo la hora de finalización calculada por Java, distinta de la consulta de fuentes.

El periodo estudiado, las fechas de publicación y el corte de evidencias son conceptos distintos. Una fuente posterior puede analizar retrospectivamente el periodo solicitado, salvo que se exija un corte histórico que la excluya; no debe presentarse como conocida antes de su publicación. publicationPositions.period representa las fechas de las publicaciones clasificadas, no el periodo estudiado. Las fechas conocidas deben ser completas y reales (YYYY-MM-DD); las desconocidas se expresan como null, sin completar por suposición. Si se desconoce un límite de la muestra, su periodo se expresa como null. Estas reglas guían al agente; el formato y la validez de las fechas también se verifican al deserializar la respuesta.

### Criterios de precisión del agente (political-v5)

La ausencia de evidencia causal, el desacuerdo entre publicaciones o la cobertura regional limitada no justifican por sí solos MISLEADING. Esta valoración requiere identificar una distorsión material demostrada por fuentes; si una proposición delimitada no puede respaldarse ni refutarse, corresponde INSUFFICIENT_EVIDENCE. Una pregunta sobre España no implica automáticamente una afirmación sobre cada región.

Las posiciones deben referirse a la misma variable, dirección, territorio y periodo que la proposición: precios, oferta, contratos y rotación no son intercambiables. MIXED requiere argumentos en ambas direcciones sobre esa misma proposición; un matiz o efecto secundario no basta. Las fechas visibles se extraen cuando constan y las fuentes retrospectivas se identifican como tales, también en la descripción de la muestra. Las referencias se etiquetan S1, S2, etc., conservando sus relaciones, y la prosa explicativa se redacta en español.

Son reglas del agente, no garantías semánticas impuestas por Java. Los casos de revisión están en POC/cross-check-service/docs/openai/political-regression-cases.md; verificar su cumplimiento real requiere evaluar respuestas y fuentes.
### Validación temporal en Java

El dominio rechaza un informe si una fecha conocida de una fuente clasificada queda fuera de publicationPositions.period, incluidos todos los miembros de una unidad agrupada. Los límites son inclusivos. Las fuentes solo documentales o excluidas de la muestra no están sujetas a su periodo.

Si asOf está definido, no puede anteceder a la publicación de las fuentes utilizadas. Una fuente que figure únicamente como excluida puede ser posterior al corte; si se cita en la síntesis o el veredicto, vuelve a considerarse evidencia utilizada. Las fuentes no excluidas se consideran utilizadas también como contexto. Las fechas y periodos desconocidos siguen admitiendo null, sin inventar valores.

La incoherencia produce INVALID_ANALYSIS_OUTPUT mediante el tratamiento existente del adaptador; no se cambian fechas ni se repite automáticamente una consulta. Esta validación comprueba coherencia entre campos, no la autenticidad de las fechas ni la calidad semántica frente a las páginas originales.
### Preparación local de la revisión de fuentes y fechas

Se ha preparado un procedimiento para identificar la versión de cada fuente, verificar publicación frente a actualización y distinguir evidencia del periodo, antecedentes y retrospectivas. Cuando la fecha completa no es verificable se conserva null; la precisión parcial se explica en la prosa. El periodo de la muestra se obtiene de las fechas verificadas de sus fuentes clasificadas; si alguna no tiene fecha completa, se deja null. El corte de evidencias no se deduce automáticamente de la fuente más reciente.

Estado: instrucciones publicadas y verificadas por lectura remota el 26/09/2026; revisión local political-v6. Evaluación real pendiente, sin nuevas inferencias en esta fase. Los ejemplos, límites y siguiente paso se documentan en Docs/revision-fuentes-fechas-agente.md. No cambia el contrato, el modelo ni el frontend.
### Metadatos temporales controlados por Java (26/09/2026)

El adaptador OpenAI deserializa estrictamente la respuesta y construye el informe con metadatos controlados por el servicio. Sustituye publicationPositions.period por el mínimo y máximo de publishedAt de las fuentes clasificadas, incluyendo reproducciones agrupadas y excluyendo fuentes ajenas a las unidades. Si la muestra está vacía o alguna de sus fechas es desconocida, devuelve null. Las fechas de publicación siguen procediendo del proveedor: este cálculo no certifica su autenticidad.

Los consultedAt de fuentes y muestra se devuelven como null porque el adaptador actual no dispone de registros verificables de acceso a cada fuente. No se usa como sustituto la hora de ejecución; analyzedAt sigue asignándose en Java. Los campos del proveedor deben seguir cumpliendo el contrato: no se admiten campos ausentes o metadatos mal formados. Un periodo válido en forma pero distinto del calculado se sustituye antes de construir el informe de dominio. La validación del dominio sigue protegiendo referencias, unicidad y corte de evidencias; asOf no se recalcula.

Las reglas nuevas se aplican al adaptador OpenAI, no a los datos ilustrativos del perfil dev. No se reescriben automáticamente posibles contradicciones en la prosa del modelo. No se han cambiado el esquema remoto ni las instrucciones political-v6. La propuesta de trazabilidad semántica permanece separada, pendiente de revisión, en Docs/propuesta-trazabilidad-clasificacion.md.
### Contrato de trazabilidad preparado en backend — 27/09/2026

Se añadió un lector estricto separado para el contrato de proveedor schemaVersion "2", con assessments y validación de autoría declarada, alcance, argumentos, referencias e inclusión/exclusión. COUNT se convierte en unidades y EXCLUDE en exclusiones individuales; se mantiene el cálculo Java del periodo. La revisión del contrato no es POLITICAL_AGENT_REVISION.

Esta fase no activa el nuevo lector en el adaptador ni modifica el agente remoto political-v6. La trazabilidad se valida en el DTO de entrada, pero su conservación en dominio, exposición en API y presentación en frontend quedan pendientes antes de la activación. El contrato actual sigue funcionando.

Esquema e informe completo de ejemplo: Docs/proposals/political-report-v2.schema.json y political-report-v2.example.json. Estado detallado: Docs/propuesta-trazabilidad-clasificacion.md. Validación local: 206 pruebas ejecutadas correctamente y una prueba opcional de navegador omitida; sin llamadas de pago. El cumplimiento estructural no verifica fidelidad a las fuentes.
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

### Separación de relevancia temporal y fechas de publicación — 28/09/2026

Estado vigente: instrucciones y esquema reforzados publicados y verificados mediante lectura remota. Contrato JSON schemaVersion 2; revisión local de conversación political-v8; nombre remoto cross-check-political-v1. Se conservan el modelo, las herramientas y los parámetros de ejecución.

El periodo estudiado determina la pertinencia del contenido. publishedAt es un metadato independiente: puede ser null sin invalidar una publicación que estudia el contexto solicitado. Un estudio publicado en 2026 sobre 2023–2025 puede ser retrospectivo; una publicación de 2023 sobre 2020–2022 es un antecedente. La ventana calculada por Java describe fechas de publicación de la muestra, no el periodo de los hechos; queda null si alguna fecha de publicación es desconocida.

asOf solo limita la información disponible cuando se establece un corte histórico. Java rechaza evidencia con fecha conocida posterior al corte. Con fechas desconocidas, la comprobación de disponibilidad histórica continúa siendo responsabilidad del agente: las instrucciones exigen excluir material cuya disponibilidad anterior al corte no se pueda establecer. Java no verifica páginas ni deduce fechas ausentes.

La plantilla y los dos esquemas de Docs/proposals separan cada evaluación mediante anyOf en COUNT y EXCLUDE. COUNT exige MATCH, REQUESTED_PERIOD o RETROSPECTIVE, postura no nula y titular distinto de UNDETERMINED. EXCLUDE exige postura null y conserva la trazabilidad y explicación. Así, COUNT no admite simultáneamente PARTIAL o BACKGROUND. Las reglas de Java permanecen activas; no se eliminan unidades ni se cambian decisiones silenciosamente para aceptar un informe.

Las instrucciones exigen evaluar el alcance antes de decidir y prohíben cambiar etiquetas solo para encajar en COUNT. El esquema restringe combinaciones, pero no demuestra que MATCH, las fechas o las atribuciones sean verdaderas; todavía requiere evaluación con fuentes reales.

Verificación: 27 pruebas de OpenAiTracedReportTest correctas (cinco casos nuevos), más 33 comprobaciones locales de JSON Schema. La respuesta real rechazada de political-v7 se conserva como fixture de regresión; se comprueba que sigue fallando, sin convertirla en un resultado válido. También se prueban fecha desconocida, retrospectiva posterior al periodo, corte histórico y exclusiones explícitas. La API OpenAI aceptó el nuevo esquema y la lectura posterior coincidió con las instrucciones y text.format locales.

El script scripts/verify-agent-schema.py requiere Python y jsonschema 4.26.0. Para esta verificación se instaló el validador únicamente en bootstrap/target/schema-validation-deps y se añadió esa carpeta a PYTHONPATH. No es una dependencia del servicio Java.

No se ejecutaron inferencias de pago en esta fase. Pendiente: una nueva prueba real acordada y la revisión separada del consumo de búsquedas. Al arrancar el servicio, usar una conversación nueva por el cambio de revisión.

### Diseño pendiente: veredicto por afirmaciones — 28/09/2026

Se preparó Docs/propuesta-veredicto-por-afirmaciones.md, un fragmento JSON Schema, ejemplos y una política ejecutable de diseño con 18 casos locales correctos. Propone reservar NO_SINGLE_VERDICT a la agregación en Java de varias afirmaciones con estados documentales diferentes. Incluye decisiones pendientes sobre posicionamiento para consultas múltiples y anclaje al texto original; no acredita corrección semántica.

No está implementado en el servicio ni en el frontend. El contrato activo sigue siendo v2 y la revisión local political-v8. No se publicó el diseño ni se ejecutaron inferencias. Consultar la propuesta antes de la siguiente fase.

### Implementación local del contrato v3 — 28/09/2026

Estado vigente: implementado y probado con proveedor simulado; no publicado en OpenAI. El esquema completo está en Docs/proposals/political-report-v3.schema.json y su ejemplo ficticio en political-report-v3.example.json. No se envía verdict global: claimAnalysis contiene objetivo, descomposición y conclusiones por afirmación. El fragmento proposalVersion del diseño no forma parte del informe v3.

ClaimAnalysis valida cardinalidad, tipos de conclusión, duplicados y anclajes literales únicos y no solapados contra el mensaje recibido. Deriva el código y conserva la explicación, respaldo y referencias de cada afirmación. OpenAiClaimReport reutiliza las validaciones de fuentes, trazabilidad y fechas. El lector se selecciona explícitamente con report-schema-version=3; no hay fallback al formato anterior.

La API incorpora claimAnalysis nullable mediante DTOs propios. Los informes v1/v2 y el proveedor dev conservan claimAnalysis null. El frontend acepta ausencia/null en el historial y muestra los nuevos detalles junto a la valoración final cuando existen, con referencias navegables y etiquetas españolas. El lugar del panel de publicaciones no cambia.

SINGLE conserva la política de pluralidad editorial cuando su conclusión documental es insuficiente; además, una muestra disponible debe declarar exactamente la misma proposición que la afirmación. MULTIPLE exige publicación UNAVAILABLE sin unidades ni exclusiones, hasta disponer de clasificación por afirmación: no se mezclan posiciones de cuestiones diferentes. La conclusión documental individual permanece visible aunque el resultado final de SINGLE se derive de publicaciones.

Limitación activa del lector v3: los anclajes se comprueban contra el mensaje actual, incluso en seguimientos. Una respuesta que reutilice fragmentos de otro turno se rechaza; no se sintetiza contexto verificable a partir de la salida del modelo. El caso «¿y después?» está cubierto como rechazo del fixture, no como funcionalidad conversacional resuelta. Antes de activar v3 debe revisarse el tratamiento de seguimientos contextuales y la adaptación de las instrucciones del agente.

Verificación: Maven verify con pruebas de navegador y repeticiones dirigidas tras ampliar los casos; informes finales de 244 pruebas, cero fallos, errores u omisiones. npm run build correcto. 36 comprobaciones locales del esquema correctas. El recorrido v3 se prueba con API Java real, upstream local simulado y navegador en escritorio/móvil; también referencias, historial tras recarga, respuestas inválidas y compatibilidad con claimAnalysis ausente/null. Capturas en POC/webapp/review/integration/claims-1536.png y claims-390.png. Se mantiene la regresión del error temporal de v2.

La configuración activa sigue siendo OPENAI_REPORT_SCHEMA_VERSION=2 y POLITICAL_AGENT_REVISION=political-v8. No se cambiaron el agente remoto, las instrucciones activas ni las credenciales y no se ejecutaron inferencias de pago. La aceptación del esquema v3 por OpenAI y la calidad semántica del modelo siguen sin comprobarse.

### Contexto de seguimientos y aclaraciones v3 — 28/09/2026

Implementado y verificado localmente, pendiente de publicación del agente. La configuración activa sigue en esquema 2 y political-v8. El servicio no solicita al proveedor v2 el nuevo formato.

El servidor conserva instantáneas de contexto: último mensaje relevante, objetivo aceptado, proposiciones y pregunta de aclaración pendiente. Cada token incluye un contextId autenticado y cifrado; no transporta el texto de la conversación. La instantánea se vincula también a sessionId. Un token antiguo referencia su propia instantánea; no se sobrescribe con la última respuesta.

La POC usa memoria del proceso, máximo 1000 instantáneas y 32000 caracteres por contexto. La caducidad coincide con la del token (por defecto dos horas desde la respuesta); se purgan entradas vencidas al acceder al almacén y se expulsan las más antiguas cuando se alcanza el límite. No hay persistencia, distribución entre instancias ni garantía de conservación tras reinicios. Los tokens ya vencidos conservan el error de expiración existente.

Si el token válido referencia contexto perdido por reinicio o expulsión, el servicio devuelve una aclaración local CONTEXT_UNAVAILABLE sin llamar al proveedor. conversationToken queda null, la UI pide la consulta completa y el siguiente envío inicia una sesión nueva. El contexto no se reconstruye a partir del historial modificable del navegador.

La entrada de v3 es un sobre de datos con currentInput y previousContext. Los anclajes se buscan en el mensaje actual y en los textos de la instantánea del servidor, no en una pregunta combinada inventada por el proveedor. Se permiten fragmentos de mensajes anteriores guardados. La pregunta de aclaración del agente no sirve como anclaje. No se verifica automáticamente la fidelidad semántica del objetivo anterior ni la conservación correcta de cada matiz; se verifica procedencia e integridad del contexto.

La respuesta del proveedor v3 usa Docs/proposals/political-response-v3.schema.json: schemaVersion, analysis y clarification. Debe existir exactamente un resultado no nulo; Java rechaza ambos o ninguno. analysis contiene el informe v3 existente. clarification contiene question y reason (MISSING_PERIOD, MISSING_SCOPE, AMBIGUOUS_REFERENCE u OTHER). CONTEXT_UNAVAILABLE es exclusivo del servidor y se rechaza si lo envía el modelo. El esquema individual admite los dos campos nullable; la exclusividad la aplica Java.

La API mantiene HTTP 200 para una aclaración válida y devuelve analysis null, clarification y un token de continuación (salvo contexto perdido). No hay veredicto ni fuentes de un supuesto análisis. Las respuestas con análisis incluyen clarification null. El frontend conserva las aclaraciones en el historial, las muestra sin alerta técnica y permite contestar desde el campo de seguimiento, también tras recargar. Los informes antiguos siguen siendo compatibles.

Se añadieron instrucciones en docs/openai/political-v3-context.draft.md, expresamente no publicadas. Indican preservar el periodo al cambiar solo de territorio, pedir periodo ante «¿y después?», usar la respuesta a la aclaración y reemplazar el objetivo en un cambio claro de tema. La decisión semántica de aclarar sigue correspondiendo al proveedor; no se ha probado todavía con el modelo real ni se garantiza por estas pruebas.

Verificación: 253 pruebas Java correctas, sin errores, fallos u omisiones, incluyendo navegador con API Java real y upstream simulado; npm run build correcto; 39 comprobaciones locales de esquemas correctas. Se prueban análisis → aclaración → respuesta, recarga sin reenvíos, referencias, pérdida de contexto, aislamiento por sesión, límite de capacidad, expiración, cambio explícito de territorio, cambio de tema y rechazo de respuestas ambiguas de contrato. Las pruebas de contenido utilizan decisiones prefijadas; acreditan el transporte y las reglas, no la comprensión del modelo.

Capturas: POC/webapp/review/integration/clarification-1536.png y clarification-390.png. No se ejecutó ninguna inferencia de pago ni se publicaron cambios remotos. La siguiente fase debe componer y revisar las instrucciones completas y el esquema v3 antes de activarlo; el timeout permanece como trabajo separado.


### Preparación local de las instrucciones completas v3

La definición completa está preparada en `POC/cross-check-service/docs/openai/political-analysis-instructions-v3.md`
y `political-agent-v3.template.json` en esa misma carpeta. Mantiene el nombre del agente
`cross-check-political-v1`, el modelo `gpt-5.4-mini` y sus ajustes actuales. El nombre del
agente no identifica la versión del contrato.

Las instrucciones separan aclaración y análisis, preservan el contexto explícito del servidor,
exigen anclajes literales y veredictos por afirmación, y dejan la agregación global a Java.
Para varias afirmaciones no se combina el posicionamiento de publicaciones en una distribución.
Se conservan las reglas de fechas, fuentes, atribución y exclusión de publicaciones.

Preparación local desde la carpeta del servicio:

```powershell
./scripts/prepare-openai-agent.ps1 -Model gpt-5.4-mini -SchemaVersion 3
```

Genera `bootstrap/target/political-agent-v3.request.json`; no lee credenciales ni publica.
Omitir `-SchemaVersion` conserva la preparación v2. El borrador de contexto anterior se
mantiene como historial y no debe publicarse por separado.

Esta fase no activa v3 ni modifica el agente remoto. Antes de publicar, revisar la descomposición,
las preguntas de aclaración y las restricciones de posicionamiento. La validación local del
esquema no demuestra aceptación por OpenAI ni precisión semántica del modelo; la exclusividad
entre análisis y aclaración se comprueba también en Java. Publicación, cambio coordinado de
configuración/revisión y prueba real de pago quedan para fases posteriores.


### Publicación y activación de v3 — 29/09/2026

Estado vigente: la definición v3 se ha publicado en el agente existente y se ha
verificado mediante una lectura posterior de OpenAI. Las instrucciones y `text.format`
coinciden con `political-analysis-instructions-v3.md` y `political-agent-v3.template.json`.
El nombre continúa siendo `cross-check-political-v1`; se conservan `gpt-5.4-mini`,
razonamiento `low`, búsqueda web `live` y el nivel de servicio `default`.

La configuración local, el ejemplo y los valores predeterminados del perfil `openai`
usan ahora `OPENAI_REPORT_SCHEMA_VERSION: 3` y `POLITICAL_AGENT_REVISION: political-v9`.
La revisión identifica compatibilidad de conversaciones, no una versión seleccionable
remota. Las conversaciones anteriores deben sustituirse por una conversación nueva.
Si el servicio estaba arrancado, hay que reiniciarlo para cargar esta configuración;
si estaba parado, basta arrancarlo con los perfiles `openai,local`. Cualquier variable
de entorno o argumento explícito antiguo debe retirarse o actualizarse porque puede
prevalecer sobre el fichero local. El perfil `dev` sigue usando el proveedor simulado.

`prepare-openai-agent.ps1 -Model gpt-5.4-mini` prepara ahora v3 por defecto y genera
`bootstrap/target/political-agent-v3.request.json`. Para preparar expresamente la
versión histórica v2, usar `-SchemaVersion 2`; los archivos v2 se conservan para revisión
y compatibilidad. El script de preparación sigue sin publicar ni leer credenciales.

La publicación no ejecutó sesiones, turnos ni consultas de análisis. La aceptación del
esquema por OpenAI no demuestra la calidad de los resultados: queda pendiente una
prueba real controlada, autorizada por separado, para revisar descomposición, contexto,
fuentes, fechas, posicionamiento y tiempo de respuesta. No se da por resuelto el timeout.

Evidencia local de la operación, dentro de la carpeta ignorada `bootstrap/target`:
`agent-before-political-v9.json` conserva la definición anterior y `agent-v3-readback.json`
la lectura verificada. Una vuelta a v2 exige restaurar conjuntamente definición remota,
contrato local y revisión de conversación; cambiar solo un número no restaura el agente.


### Resultado de la prueba real v3 — 29/09/2026

La única consulta real autorizada devolvió HTTP 504 a los 90,298 s; el mismo turno acabó
en 106 s y se recuperó por GET para diagnóstico. Pasa el lector Java v3 con una afirmación
y veredicto documental INSUFFICIENT_EVIDENCE. La revisión de publicaciones detecta mezcla
de autoría/contenido entre fuentes y periodos incorrectamente clasificados: la fase no se
considera superada. No se entregó un análisis en la UI. Véase el detalle y las fuentes en
`POC/cross-check-service/docs/openai/publications-live-review.md`, apartado political-v9.
Pendientes: entrega asíncrona recuperable y control de coherencia semántica de fuentes.
No se han implementado estos cambios ni realizado una segunda inferencia en esta fase.


### Diseño de entrega asíncrona — fase 1, 29/09/2026

Se documenta la [propuesta de entrega asíncrona](propuesta-entrega-asincrona.md), pendiente
de implementación y revisión. Define POST /api/analysis/jobs, consulta autenticada por ID,
cola persistida, estados, idempotencia, recuperación de envíos inciertos y contexto durable.
Propone H2 en fichero para una instancia de la POC y activación coordinada del frontend.
Los [ejemplos del contrato](proposals/async-analysis.examples.json) son ilustrativos.

La fase no modifica Java, frontend, configuración ni el agente remoto. El comportamiento
activo continúa siendo síncrono. La matriz del documento establece pruebas futuras con
proveedor simulado, incluyendo reinicios reales, duplicados, caducidad y fallos de disco.
El timeout local no equivale a cancelación del trabajo remoto ni garantiza detener costes.


### Entrega asíncrona — fase 2 implementada, deshabilitada por defecto

Se implementan la API de trabajos, el worker con checkpoints, H2 en fichero y contexto
durable. Los envíos inciertos se reconcilian sin reenvío automático. La ruta síncrona
queda excluida cuando se activa async; la configuración habitual sigue desactivada para
conservar el frontend actual. No se ha realizado una consulta real de OpenAI en esta fase.

La [guía de operación](../POC/cross-check-service/docs/async-analysis.md) describe las
propiedades, los endpoints, los límites y las pruebas. La [propuesta](propuesta-entrega-asincrona.md)
conserva el diseño de referencia. Pendiente fase 3: polling y recuperación en frontend,
activación coordinada y, con autorización separada, prueba real.


### Entrega asíncrona — fase 3, frontend integrado (29/09/2026)

La web crea trabajos con POST /api/analysis/jobs y consulta GET /api/analysis/jobs/{id}.
Guarda antes del envío la clave de idempotencia, credencial de acceso y cuerpo exacto.
Una recarga, desconexión o pérdida del 202 recupera la misma solicitud; no crea otra
inferencia por iniciativa propia. Cada petición HTTP dura como máximo 15 s. El polling
sigue al servidor (3 s), con backoff ante fallos, pausa en pestañas ocultas y recuperación
al reconectar. A los 90 s se informa de la demora sin dar por terminado el análisis.

El historial conserva informes y aclaraciones; los seguimientos usan el último token.
Los trabajos fallidos/caducados no se reenvían automáticamente. Web Locks coordina las
pestañas y localStorage conserva el acceso. Si no se puede guardar la solicitud, no se
envía. Borrar el historial advierte que se pierde el acceso sin cancelar el trabajo remoto.
Las credenciales de jobs completados se retiran del historial. El panel de publicaciones
mantiene su ubicación. El límite de 50 conversaciones/100 turnos evita descartar trabajos
pendientes de manera automática.

ASYNC_ANALYSIS_ENABLED pasa a true por defecto; la ruta síncrona queda excluida con 409.
El perfil dev sigue siendo simulado y openai selecciona al proveedor real. Una
sobrescritura local false debe eliminarse o cambiarse a true. Se requiere reiniciar Java.
La revisión de esta fase es offline, incluida la conexión navegador/Vite/Spring Boot dev.
La compatibilidad del nuevo ciclo con OpenAI real y la calidad semántica de las fuentes
siguen pendientes; no se han ejecutado nuevas inferencias ni cambiado el agente.

Detalles y pruebas en la [guía del servicio](../POC/cross-check-service/docs/async-analysis.md)
y la [guía del frontend](../POC/webapp/README.md). Las secciones de fases anteriores
conservan su estado histórico; esta fase sustituye la desactivación inicial del transporte.


### Entrega asíncrona — primera prueba real, 30/09/2026

Una consulta real de OpenAI se completa y llega al frontend tras recargar durante RUNNING:
59,62 s en el servicio y unos 65 s para la revisión del navegador. Se observan un POST,
16 GET, una sesión remota y un único turno completed; la recarga final conserva el informe.
El resultado declarado es INSUFFICIENT_EVIDENCE, una afirmación, cinco fuentes y
posicionamiento no disponible. Su exactitud factual no se ha auditado en esta fase.

Antes de probar se corrige una suposición del mock: environment.type=none requiere input
al crear la sesión. El worker persiste el intento/huella antes de esa llamada y no vuelve
a enviarlo por events. Si la respuesta se pierde, correlaciona metadata e input; una
sesión sin turno visible no habilita un reenvío. Los seguimientos mantienen events.
No cambian el agente, el modelo ni las instrucciones. La prueba no superó 90 s ni provocó
una caída de Java; esos escenarios permanecen verificados localmente.

[Informe y captura](../POC/cross-check-service/docs/openai/async-live-review.md).


## Revisión local de fuentes — candidata political-v10 (30/09/2026)

La [auditoría de las cinco fuentes](auditoria-fuentes-informe-2026-09-30.md) detectó problemas de fechas, periodo estudiado, indicadores y uso de citas en la síntesis. Se han preparado instrucciones locales para comprobar esos aspectos y separar la postura de una publicación de la suficiencia de sus pruebas. Se conservan los requisitos de elegibilidad COUNT y el contrato v3.

La propuesta distingue una muestra SINGLE evaluada sin publicaciones contables (AVAILABLE con exclusiones) de una evaluación que no pudo realizarse (UNAVAILABLE). Mantiene la regla de MULTIPLE y no modifica la disposición del frontend ni los veredictos existentes.

Estado: candidata local, no publicada; la revisión activa continúa en political-v9. Véanse los [cambios y validación](../POC/cross-check-service/docs/openai/political-v10-review.md) y los [16 casos semánticos pendientes](../POC/cross-check-service/docs/openai/political-v10-source-review-cases.md). Las 39 comprobaciones estructurales superadas no demuestran todavía cumplimiento semántico del modelo.


## Publicación y prueba de political-v10 (30/09/2026)

Political-v10 ya está publicada; la configuración remota y las instrucciones de la sesión ejecutada coinciden con la versión local. El perfil, ejemplo y configuración local utilizan esa revisión. La única consulta real autorizada terminó en INVALID_ANALYSIS_OUTPUT: el agente reformuló el anclaje que debía copiar literalmente. El rechazo se reprodujo offline. También se detectaron referencias de fuentes cruzadas, dos fechas erróneas y una clasificación de postura no suficientemente justificada. No se considera resuelta la calidad de fuentes.

OpenAiApiTest: 3 pruebas superadas y 2 opcionales omitidas. Los 16 casos sintéticos siguen pendientes. No hubo segunda inferencia ni relajación de validadores. [Ejecución, evidencia y siguiente fase propuesta](../POC/cross-check-service/docs/openai/political-v10-live-review.md).


## Diseño pendiente: evidencia por fuente antes de síntesis

Se ha preparado una [propuesta de extracción y validación por fuente](propuesta-extraccion-evidencia-por-fuente.md), con expediente interno, referencias estables, límites de verificación y recuperación asíncrona por etapas. Es un diseño para revisión: no cambia todavía el contrato público v3, la UI ni el agente political-v10. La implementación y cualquier nueva prueba de pago pertenecen a fases posteriores.


## Expediente de evidencia: contrato offline implementado

Se incorpora el contrato interno v1 EvidenceDossier y la proyección de fuentes/referencias EvidenceAssembler. Valida anclajes literales, identidad de referencias, coincidencia de fragmentos con capturas, precisión y revisión de fechas. La suite de dominio supera 56 pruebas, incluidas 15 nuevas, sin omisiones. EV01 tiene regresión automática; EV02 tiene comprobaciones estructurales; EV03/EV04 utilizan metadatos de prueba revisados. EV05 continúa pendiente de revisión semántica.

No hay adquisición de documentos, persistencia del expediente ni integración con OpenAI todavía. El frontend, contrato público v3 y agente political-v10 no cambian. [Implementación, garantías y límites](../POC/cross-check-service/docs/evidence-dossier.md).
