# Requisitos y funcionalidad — PoC inicial (pre-MVP)

- **Versión:** 1.1
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
| **Noticias falsas o bulos** | Examina contenidos potencialmente falsos, manipulados o descontextualizados; busca su origen y evidencias que los respalden o contradigan. | Futura |
| **Deportes** | Contrasta resultados, estadísticas, récords y afirmaciones deportivas, precisando competición, temporada, categoría y fecha. | Futura |
| **Datos y hechos históricos** | Examina fechas, acontecimientos y relatos históricos, diferenciando hechos documentados, interpretaciones historiográficas y controversias. | Futura |
| **Estafas publicitarias y timos** | Analiza ofertas, promociones y mensajes para identificar promesas engañosas e indicios de fraude. Distingue señales de riesgo de engaños documentados y evita convertir la ausencia de alertas en garantía de seguridad. | Futura |
| **Sociedad** | Analiza afirmaciones sobre actualidad social, televisión, entretenimiento, ocio, turismo, hostelería y servicios cotidianos. Incluye comparaciones de audiencias, establecimientos y recomendaciones, distinguiendo datos verificables, valoraciones de usuarios y preferencias personales. | Futura |
| **General** | Permite analizar consultas que no encajen en ninguna categoría especializada. Delimita la afirmación, aporta contexto y contrasta las fuentes disponibles; solicita criterios o aclaraciones cuando sean necesarios y expresa los límites de la conclusión. | Futura |

Las categorías futuras se documentan como evolución del producto; no se muestran como opciones seleccionables en esta PoC.

**Una conversación tiene una categoría inmutable. Cambiar de categoría requerirá una nueva conversación.**

### 3.1. Ejemplos y criterios de Sociedad

Los siguientes ejemplos ilustran consultas admitidas; no constituyen afirmaciones verificadas ni recomendaciones del producto:

- «El Hormiguero es el programa más visto en su franja horaria, por encima de La Revuelta». El análisis debe precisar fechas, franja coincidente y métrica de audiencia: espectadores, cuota u otra medida comparable.
- «El camping Taiga Conil es más recomendable que el Camping Roche en Conil». El análisis debe explicitar los criterios de comparación —ubicación, instalaciones, precio, servicios o tipo de viaje— y separar características verificables de reseñas y preferencias.
- «Casa Mané es uno de los mejores restaurantes de Cádiz». El análisis debe delimitar el ámbito geográfico y qué significa «mejor», diferenciando reconocimientos documentados, valoraciones de clientes y opinión subjetiva.

En comparaciones y recomendaciones, el agente no debe convertir una preferencia en una verdad universal. Si faltan criterios, los solicitará o hará explícitos los utilizados. Las reseñas no se considerarán automáticamente representativas ni equivalentes a evidencias de calidad objetiva. Si no procede un veredicto factual único, se utilizará una valoración matizada, interpretación u opinión, o sin veredicto único.

### 3.2. Uso de General

General será la opción de cobertura para asuntos no contemplados en las categorías especializadas. No activará una reclasificación automática de una conversación existente: se conserva la categoría seleccionada al iniciarla. Si el asunto requiere otra especialidad, podrá sugerirse abrir una nueva conversación en la categoría correspondiente.

El catálogo previsto queda compuesto por siete categorías. Sociedad y General se incorporan al catálogo futuro; esta revisión no amplía las categorías operativas de la PoC, que sigue incluyendo únicamente Análisis político.

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

| Estado técnico | Etiqueta orientativa | Indicador | Criterio |
|---|---|---|---|
| `SUPPORTED` | Respaldada | Verde / comprobación | Las evidencias examinadas apoyan la afirmación dentro del alcance indicado. |
| `REFUTED` | Refutada | Rojo / cruz | Las evidencias examinadas contradicen la afirmación. |
| `MISLEADING` | Engañosa o fuera de contexto | Ámbar / advertencia | Hay elementos ciertos, pero se omite o altera contexto de forma que cambia su significado. |
| `INSUFFICIENT_EVIDENCE` | Evidencia insuficiente / No acreditada con las evidencias examinadas | Gris / bandera neutral | No hay base suficiente para sostener una conclusión sobre la afirmación. |
| `OPINION` | Interpretación u opinión | Azul / información | El contenido no puede valorarse directamente como verdadero o falso. |
| `NO_SINGLE_VERDICT` | Sin veredicto único | Neutral / balanza | La pregunta abierta o la combinación de afirmaciones requiere una conclusión diferenciada. |

«Pendiente de contraste» es un estado de ejecución, no un veredicto final. `OPINION` tampoco sustituye al contraste de las afirmaciones factuales que pueda contener una opinión.

#### Cómo se determina

La valoración es un juicio cualitativo del agente sobre las evidencias, **no una media de opiniones ni un porcentaje de falsedad**.

El agente debe:

1. Delimitar la afirmación y su alcance.
2. Identificar evidencias pertinentes a favor y en contra.
3. Considerar procedencia, relación directa con la afirmación, independencia, fecha y contradicciones.
4. Diferenciar evidencias de inferencias.
5. Asignar un estado y justificarlo con referencias.
6. Expresar lo que no puede concluirse.

No se ha acordado una fórmula numérica ni umbrales automáticos para el veredicto. No se inventará una puntuación de certeza.

El objeto de valoración incluirá estado, explicación y referencias a las fuentes relevantes. React traducirá el estado a un indicador visual mediante reglas estables.

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
12. La medición de publicaciones no altera automáticamente el veredicto documental.
13. El historial sobrevive a una recarga en el mismo navegador y permite abrir, renombrar y eliminar conversaciones.
14. El perfil identifica a Ale Jiménez como usuario de demostración y cerrar sesión está deshabilitado.
15. Las páginas informativas son accesibles.
16. No se requieren base de datos, almacenamiento de archivos ni autenticación real.
17. La clave de OpenAI no se expone al navegador.

## 10. Validación de la PoC y evolución

La prueba evaluará utilidad del análisis, calidad y pertinencia de fuentes, consistencia de los indicadores, capacidad de reconocer incertidumbre, clasificación de publicaciones, tiempo de respuesta y consumo.

Los casos de prueba incluirán afirmaciones respaldadas, refutadas, descontextualizadas, opiniones, preguntas abiertas y falta de evidencias. La calidad factual deberá revisarse con casos de referencia; que el JSON sea válido no garantiza que el análisis sea correcto.

Las siguientes fases podrán incorporar autenticación, persistencia sincronizada, nuevas categorías y formatos multimedia. Esas ampliaciones no forman parte de esta entrega.
