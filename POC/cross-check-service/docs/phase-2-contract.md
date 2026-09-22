# Fase 2 · Modelo, contrato y caso de uso

Esta fase implementa el dominio y el caso de uso en Java puro. La forma JSON descrita aquí es el contrato previsto para la fase 3: todavía no hay controller, serialización HTTP, beans del caso de uso, proveedor real ni cifrado. Los dobles del proveedor y del codificador solo existen en los tests.

## Recorrido para revisar

1. `domain/.../analysis/AnalysisReport.java`: informe y validación de referencias entre fuentes, veredicto, síntesis y publicaciones.
2. `application/.../features/analysis/start/StartAnalysisHandler.java`: coordinación de consulta nueva y seguimiento.
3. `application/.../contracts/`: puertos del proveedor de IA y del codificador de referencias.
4. `application/.../features/analysis/common/dto/`: resultado de aplicación, independiente del dominio y de HTTP.
5. Ejemplos de este documento y tests de ambos módulos.

## Entrada

Futuro endpoint: `POST /api/analysis/start`.

| Campo | Regla |
|---|---|
| `text` | Obligatorio, no vacío ni formado únicamente por espacios según `String.isBlank()`. Se conserva el texto original, incluidos enlaces. El máximo se configura y se cuenta en puntos de código Unicode. |
| `conversationToken` | Ausente o null inicia conversación. Si está presente, debe ser no vacío, respetar la longitud máxima y superar la validación del codificador. Un token vacío no equivale a una consulta nueva. |

La categoría y la revisión del agente se fijan en el servidor. La solicitud no acepta un identificador remoto de sesión, modelo o agente.

- [Consulta nueva](examples/start-request.json).
- [Seguimiento](examples/follow-up-request.json).

## Resultado

`StartAnalysisResult` contiene un `conversationToken` opaco y un `analysis`.

| Campo de analysis | Contenido |
|---|---|
| `title`, `context`, `summary` | Textos obligatorios. |
| `summarySourceIds` | Referencias de la síntesis a identificadores de `sources`. Puede estar vacío; no se inventan citas. |
| `verdict` | `status`, `explanation`, `documentarySupport` y `sourceIds`. |
| `sources` | Lista de fuentes; puede estar vacía. |
| `publicationPositions` | Disponibilidad, alcance, unidades clasificadas y exclusiones. |
| `limitations` | Lista de limitaciones, vacía si no se han indicado. |
| `analyzedAt` | Instante UTC asignado por el reloj del servidor al finalizar el caso de uso. El proveedor no lo controla. |
| `asOf` | Fecha de corte `YYYY-MM-DD`, o null si no se conoce. |

Se mantiene la estructura de la propuesta de arquitectura. Se concretan dos campos adicionales: `summarySourceIds` hace explícitas las referencias de la síntesis y `publicationPositions.consultedAt` recoge la fecha de consulta de la muestra.

Los estados de veredicto son `SUPPORTED`, `REFUTED`, `MISLEADING`, `INSUFFICIENT_EVIDENCE`, `OPINION` y `NO_SINGLE_VERDICT`. El backend conserva el veredicto del agente: no lo deduce de recuentos ni aplica criterios editoriales propios.

### Fuentes

Cada fuente contiene:

- `id`, `title`, `url` y `contribution`, obligatorios.
- `consultedAt`, instante de consulta obligatorio; en una exclusión puede representar el intento de acceso.
- `publisher`, `publishedAt` y `type`, opcionales y representados por null si se desconocen. `publishedAt` es una fecha; `type` es texto descriptivo, no un catálogo cerrado en esta fase.

Las URLs deben ser absolutas, HTTP o HTTPS, con host y sin credenciales. Esta validación no comprueba que existan ni descarga contenido. Los identificadores deben ser únicos; las referencias se resuelven por coincidencia exacta.

### Publicaciones

| Campo | Regla |
|---|---|
| `availability` | `AVAILABLE` si se realizó la clasificación; `UNAVAILABLE` si no está disponible. |
| `reason` | Obligatoria cuando no está disponible o no hay unidades clasificadas; opcional en los demás casos. |
| `proposition` | Proposición concreta; obligatoria cuando la clasificación está disponible. |
| `period` | `{from, to}` con fechas inclusivas y ordenadas, o null si se desconoce. |
| `consultedAt` | Instante de consulta de la muestra, obligatorio cuando la clasificación está disponible. |
| `selectionCriteria` | Criterio de selección, obligatorio cuando la clasificación está disponible. |
| `units` | Unidades deduplicadas: `id`, `sourceIds`, `position`, `explanation`. Cada unidad contiene al menos una fuente. |
| `excluded` | Fuentes excluidas: `sourceId` y `reason`. Se conservan sus metadatos en `sources`. |

Posiciones admitidas: `SUPPORTS`, `QUESTIONS`, `MIXED`, `NO_EXPLICIT_POSITION`.

Una fuente no puede pertenecer a dos unidades ni estar clasificada y excluida a la vez. Una unidad puede agrupar reproducciones de varias fuentes. La decisión editorial de agruparlas la toma el proveedor; Java solo comprueba la coherencia de sus identificadores.

`UNAVAILABLE` exige una lista de unidades vacía. `AVAILABLE` con cero unidades es válido y permite representar un intento de clasificación en el que todas las publicaciones quedaron excluidas.

No se devuelve ningún porcentaje ni recuento redundante. El frontend calculará N a partir de `units.size()`, manteniendo las unidades mixtas y sin posición en el denominador. Con N = 0 mostrará «No calculable»; con `UNAVAILABLE`, «Medición no disponible».

## Ejemplos para revisar

Todos son ficticios. Los tokens no son utilizables y los enlaces de example.org no representan fuentes investigadas.

- [Evidencia insuficiente y medición no disponible](examples/insufficient-evidence-response.json).
- [Clasificación con reproducciones agrupadas y una exclusión](examples/classified-response.json): dos unidades, una cuestiona y otra no adopta posición; el frontend derivaría 50 % para cada una.
- [Clasificación realizada sin unidades válidas](examples/zero-units-response.json): N = 0, nunca 0 %.

Los ejemplos se validan como JSON, pero la prueba de serialización exacta contra el endpoint pertenece a la fase 3.

## Coordinación y referencias de conversación

El handler valida entrada y token antes de invocar al proveedor. Decodifica la referencia, comprueba categoría, revisión y caducidad, y proporciona al proveedor la sesión remota únicamente desde esa referencia validada.

El contrato de `ConversationReferenceCodec` exige verificar integridad y formato. El cifrado real queda para la fase 4; los tests usan un doble sin seguridad, que no se incorpora al servidor.

Una referencia que caduca exactamente en el instante actual se considera caducada. La caducidad se comprueba al admitir el seguimiento; si caduca durante un análisis ya admitido, el resultado puede completarse. Cada resultado correcto renueva el plazo desde su finalización. Si el proveedor cambia de sesión durante un seguimiento, se rechaza el resultado para evitar perder silenciosamente el contexto.

`AnalysisPolicy` recibe longitud máxima de consulta, longitud máxima de token, revisión del agente y duración de la referencia. La longitud del token se mide en unidades UTF-16 de Java; el codec futuro debe producir tokens ASCII. Los valores de las pruebas son ilustrativos, no configuración de producción.

No hay reintentos automáticos, persistencia ni control de concurrencia en esta fase. Si falla la emisión del token después del análisis, se propaga el fallo técnico sin volver a ejecutar al proveedor.

Los contenedores de consulta, sesión y token tienen un `toString()` redactado. Esto no sustituye la futura política de logs: no se deben registrar solicitudes, informes o credenciales.

## Errores y traducción HTTP prevista

Las excepciones de aplicación no contienen códigos HTTP. La siguiente tabla orienta al controller de la fase 3:

| Situación | Tipo / razón | HTTP previsto | Código previsto |
|---|---|---|---|
| Consulta inválida | `InvalidAnalysisInputException` | 400 | `INVALID_ANALYSIS_INPUT` |
| Token inválido o revisión incompatible | `InvalidConversationReferenceException` | 400 | `INVALID_CONVERSATION_REFERENCE` |
| Token caducado | `ExpiredConversationReferenceException` | 410 | `CONVERSATION_REFERENCE_EXPIRED` |
| Resultado inconsistente o incompleto | `InvalidAnalysisOutputException` | 502 | `INVALID_ANALYSIS_OUTPUT` |
| Proveedor no disponible | `AnalysisProviderException / UNAVAILABLE` | 503 | `ANALYSIS_PROVIDER_UNAVAILABLE` |
| Tiempo de espera agotado | `AnalysisProviderException / TIMEOUT` | 504 | `ANALYSIS_TIMEOUT` |
| Sesión confirmada como no disponible | `AnalysisProviderException / SESSION_UNAVAILABLE` | 410 | `CONVERSATION_UNAVAILABLE` |
| Conflicto remoto | `AnalysisProviderException / CONFLICT` | 409 | `ANALYSIS_CONFLICT` |
| Error interno al emitir referencia | Fallo interno | 500 | `INTERNAL_ERROR` |

Los errores del proveedor conservan `executionState`: `NOT_STARTED` solo cuando consta que no se ejecutó; `UNKNOWN` cuando no hay certeza. Un timeout no implica cancelación remota. No se devuelven detalles del proveedor. `requestId` y la traducción de mensajes son responsabilidad de la futura capa HTTP.

Ejemplos: [entrada inválida](examples/invalid-input-error.json), [referencia caducada](examples/expired-reference-error.json), [resultado inválido](examples/invalid-output-error.json) y [timeout](examples/timeout-error.json).

Una inconsistencia estructural nunca se convierte en `INSUFFICIENT_EVIDENCE`: es un error técnico sin resultado de análisis.

## Pruebas

Desde la carpeta del servicio:

```powershell
mvn -pl domain,application -am test
mvn clean verify
```

Hay 54 casos ejecutados: 28 de dominio y 26 de aplicación, contando las variantes parametrizadas. Se prueban duplicados, referencias inexistentes, inmutabilidad, URLs, periodos, disponibilidad, consulta nueva, seguimiento, caducidad, límites Unicode, errores sin reintentos y mapeo de resultados.

JUnit solo es dependencia de test. El código de producción de estos dos módulos utiliza exclusivamente Java y las dependencias internas permitidas.

La fase queda lista para revisión. La fase 3 no se inicia hasta presentar su trabajo y recibir confirmación.
