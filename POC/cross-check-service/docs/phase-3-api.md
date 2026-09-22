# Fase 3 · API con proveedor simulado

**Documento de la fase 3.** Desde la fase 4 se requiere una clave y los tokens usan cifrado real. El registro en memoria, el prefijo dev_ y DEV_REFERENCE_CAPACITY descritos aquí son históricos. Consultar [la configuración vigente](phase-4-conversation-tokens.md) antes de arrancar.

La API conecta HTTP con el caso de uso de la fase 2. Solo se activa con el perfil `dev`. Devuelve datos explícitamente ficticios, sin llamar a OpenAI ni investigar el texto o las URLs recibidas.

## Arrancar y revisar

Desde la carpeta del servicio:

```powershell
mvn clean verify
java -jar bootstrap/target/cross-check-bootstrap-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

El perfil dev escucha en `127.0.0.1:8080`. Si el puerto está ocupado, añadir `--server.port=8081`. En IntelliJ, establecer `dev` como perfil activo o pasar `--spring.profiles.active=dev` en los argumentos del programa.

Desde otra terminal, en la misma carpeta:

```powershell
./scripts/verify-api.ps1
```

Para otro puerto: `./scripts/verify-api.ps1 -BaseUrl http://127.0.0.1:8081`. El script comprueba salud, consulta nueva, fecha de servidor y seguimiento. Requiere un escenario de éxito.

Para inspeccionar la respuesta manualmente:

```powershell
$endpoint = 'http://127.0.0.1:8080/api/analysis/start'
$payload = @{ text = 'Consulta de ejemplo'; conversationToken = $null } | ConvertTo-Json
$result = Invoke-RestMethod -Uri $endpoint -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($payload))
$result | ConvertTo-Json -Depth 20

$followUpPayload = @{ text = 'Pregunta de seguimiento'; conversationToken = $result.conversationToken } | ConvertTo-Json
Invoke-RestMethod -Uri $endpoint -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($followUpPayload)) | ConvertTo-Json -Depth 20
```

En Postman: seleccionar POST, URL `http://127.0.0.1:8080/api/analysis/start`, Body → raw → JSON, y pegar [start-request.json](examples/start-request.json). Para continuar, copiar el token de la respuesta; el token ilustrativo de la fase 2 no es válido.

Detener con `Ctrl+C`. Sin dev, la salud sigue funcionando y el endpoint de análisis responde 404; los adaptadores simulados no se registran.

## Escenarios

El escenario se selecciona al arrancar mediante `DEV_ANALYSIS_SCENARIO` o el argumento `--crosscheck.development.scenario=...`. No se selecciona mediante campos, cabeceras o palabras especiales de la consulta.

Ejemplo para revisar fuentes y clasificación:

```powershell
java -jar bootstrap/target/cross-check-bootstrap-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --crosscheck.development.scenario=CLASSIFIED
```

| Escenario | HTTP | Resultado |
|---|---|---|
| `INSUFFICIENT_EVIDENCE` (predeterminado) | 200 | Informe ilustrativo sin fuentes; clasificación `UNAVAILABLE`. |
| `CLASSIFIED` | 200 | Cuatro fuentes ficticias, dos unidades y una exclusión. Una unidad agrupa dos reproducciones. |
| `ZERO_UNITS` | 200 | Clasificación `AVAILABLE`, ninguna unidad y una exclusión; el frontend deberá mostrar «No calculable». |
| `TIMEOUT` | 504 | `ANALYSIS_TIMEOUT`, estado `UNKNOWN`. |
| `UNAVAILABLE` | 503 | `ANALYSIS_PROVIDER_UNAVAILABLE`, estado `NOT_STARTED`. |
| `INVALID_OUTPUT` | 502 | `INVALID_ANALYSIS_OUTPUT`, estado `UNKNOWN`. |
| `CONFLICT` | 409 | `ANALYSIS_CONFLICT`, estado `NOT_STARTED`. |
| `SESSION_UNAVAILABLE` | 410 | `CONVERSATION_UNAVAILABLE`, estado `NOT_STARTED`. |

El timeout es un error simulado inmediato, no una espera real. Ningún escenario consume IA ni realiza llamadas externas. La fecha `analyzedAt` la asigna el servidor; los metadatos de fuentes, incluidos los instantes de consulta, son ilustrativos.

## Validación y errores

Se conserva el [contrato de la fase 2](phase-2-contract.md). Además:

- Cuerpo JSON obligatorio: `text` debe ser string y `conversationToken`, string o null.
- Campos desconocidos, claves duplicadas, JSON adicional tras el objeto y conversiones implícitas desde números o booleanos: 400 `INVALID_REQUEST`.
- Texto ausente, vacío, formado solo por espacios o superior al límite: 400 `INVALID_ANALYSIS_INPUT`.
- Token vacío, desconocido, manipulado o superior al límite: 400 `INVALID_CONVERSATION_REFERENCE`.
- Referencia caducada que aún está en el registro: 410 `CONVERSATION_REFERENCE_EXPIRED`.
- Método incorrecto: 405; Content-Type incorrecto: 415; formato de respuesta no aceptable: 406. Se mantiene la cabecera `Allow` cuando corresponde.
- Fallo interno inesperado: 500 `INTERNAL_ERROR`, sin mensajes internos ni trazas en la respuesta.

Los errores contienen únicamente `code`, `message`, `requestId` y `executionState`. Las validaciones ocurren antes de llamar al proveedor. Los fallos no generan reintentos ni informes ficticios de éxito.

Todas las respuestas incluyen `X-Request-Id`, generado por el servidor; en errores coincide con `requestId` del cuerpo. Las respuestas bajo `/api/` llevan `Cache-Control: no-store`.

Los mensajes al usuario están en español. Identificadores, comentarios y Javadoc están en inglés.

## Configuración de desarrollo

| Variable de entorno | Propiedad | Valor inicial |
|---|---|---|
| `SERVER_PORT` | `server.port` | 8080 |
| `MAX_INPUT_LENGTH` | `crosscheck.analysis.max-input-length` | 10000 puntos de código Unicode |
| `MAX_TOKEN_LENGTH` | `crosscheck.analysis.max-token-length` | 4096 unidades UTF-16; mínimo 40 para el token simulado |
| `POLITICAL_AGENT_REVISION` | `crosscheck.analysis.agent-revision` | development-v1 |
| `CONVERSATION_TOKEN_TTL` | `crosscheck.analysis.conversation-ttl` | PT2H |
| `DEV_ANALYSIS_SCENARIO` | `crosscheck.development.scenario` | INSUFFICIENT_EVIDENCE |
| `DEV_REFERENCE_CAPACITY` | `crosscheck.development.reference-capacity` | 1000 referencias |

Son valores iniciales de desarrollo, no límites finales de despliegue. La configuración se valida al arrancar.

La configuración JSON utiliza Jackson 3: propiedades de deserialización, rechazo de conversiones y detección de claves duplicadas. Referencia: [configuración JSON de Spring Boot](https://docs.spring.io/spring-boot/how-to/spring-mvc.html).

## Límites de la simulación

Los tokens aleatorios con prefijo `dev_` se registran en memoria y no están cifrados. Solo se guardan los datos de referencia, sin textos ni informes. Al reiniciar se pierden y pasan a ser desconocidos.

Cada respuesta correcta emite un token nuevo. Los anteriores pueden seguir válidos mientras estén en el registro y no hayan caducado. Al emitir tokens se limpian referencias caducadas; al alcanzar la capacidad se elimina la más antigua. Una referencia eliminada responde 400 porque ya no se conoce su caducidad.

El seguimiento reutiliza la sesión simulada y devuelve un título específico. No conserva contexto semántico; la continuidad real del agente se comprobará al integrar OpenAI.

Los adaptadores se ensamblan desde `DevelopmentAnalysisConfiguration`, limitada a dev. El escaneo de componentes se restringe a bootstrap para evitar que el controller active el flujo simulado en otro perfil.

Quedan fuera: cifrado real (fase 4), OpenAI (fase 5), conexión del frontend (fase 6), límites de frecuencia y concurrencia (fase 7), CORS para el frontend y controles de despliegue.

## Verificación

- Dominio y aplicación: 54 pruebas conservadas.
- Registro de referencias: 4 pruebas.
- API sobre un servidor HTTP real en puerto aleatorio: 39 casos.
- Perfil sin simulación: 1 prueba de salud, 404 y ausencia de beans simulados.
- Total: 98 pruebas.

Se comprueban campos JSON, valores nulos, fechas, referencias, agrupación, errores, límites Unicode, cabeceras y ausencia de reintentos. Los dobles de prueba se implementan en Java, sin Mockito.

```powershell
mvn clean verify
```

Antes de iniciar la fase 4 se presentará su alcance y se esperará confirmación.
