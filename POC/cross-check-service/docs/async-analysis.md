# Trabajos de análisis asíncronos

## Actualización political-v11 — 01/10/2026

El proveedor de evidencia ejecuta RESEARCH, CAPTURE y SYNTHESIS dentro del mismo trabajo. Persiste el expediente, la etapa, la huella de entrada y el historial de sesiones, turnos y uso. NEXT_STAGE conserva el punto previo al siguiente envío; la reconciliación evita repetir un envío ambiguo. CAPTURE procesa una fuente por paso y SYNTHESIS usa otra sesión remota. La conversación lógica conserva su identidad y sus reservas entre etapas.

El plazo total sigue siendo diez minutos: no se reinicia por etapa. Una interrupción prolongada puede caducar el trabajo aunque OpenAI haya terminado. La retención purga también el expediente; el estado HTTP no expone sus textos ni IDs remotos. `OPENAI_EVIDENCE_PIPELINE_ENABLED` requiere async activo. La publicación v11 sí actualizó el agente remoto y la configuración local; las notas inferiores sobre no modificarlos pertenecen a la entrega anterior.

La prueba real v11 falló; no debe confundirse con la prueba asíncrona anterior satisfactoria. [Diagnóstico completo](openai/political-v11-live-review.md).

Fases 2 y 3 implementadas, 29/09/2026. API asíncrona activada por defecto y frontend integrado. Validación offline completada y primera prueba real correcta el 30/09/2026; véase la revisión enlazada al final.
El diseño y los criterios de aceptación están en `Docs/propuesta-entrega-asincrona.md`.

## Activación para revisión local

Desde la carpeta del servicio, después de `mvn verify`:

```powershell
java -jar bootstrap/target/cross-check-bootstrap-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev,local --crosscheck.analysis.jobs.enabled=true
```

Este ejemplo utiliza el proveedor simulado. No usar `openai` para una revisión offline.
El frontend utiliza `/api/analysis/jobs`. La ruta antigua `/api/analysis/start` devuelve
409 / ASYNC_ANALYSIS_REQUIRED con async activo para impedir eludir las reservas de conversación.
`ASYNC_ANALYSIS_ENABLED` vale true por defecto. Si tu configuración local contiene false,
cámbialo a true o elimina esa sobrescritura; reinicia el servicio y Vite para revisar esta fase.
En IntelliJ basta con el perfil `dev,local` y el escenario `CLASSIFIED`; no activar `openai`
para esta comprobación. La variable no selecciona proveedor: eso lo determina el perfil.

Configuración bajo `crosscheck.analysis.jobs`:

| Propiedad | Valor predeterminado |
|---|---|
| enabled | true; variable `ASYNC_ANALYSIS_ENABLED` |
| database-path | data/cross-check; variable `ANALYSIS_DATABASE_PATH` |
| concurrency / queue-capacity | 2 / 20 |
| queue-timeout / execution-timeout | PT2M / PT10M |
| retention / tombstone-retention | PT24H / P7D |
| lease | PT60S |
| worker-enabled | true; false solo para pruebas que avanzan el worker explícitamente |

La ruta de base relativa se resuelve desde el directorio de ejecución. Para IntelliJ,
configurar una ruta absoluta estable si el working directory cambia. Los datos no están
en target y `/data/` está excluido de Git. La base H2 pertenece a una única instancia;
no ejecutar dos procesos sobre el mismo fichero ni borrar la base para reiniciar el servicio.
No se han cambiado `application-local.yml`, el agente remoto o sus instrucciones.

## Contrato HTTP implementado

- `POST /api/analysis/jobs`: cuerpo `{ "text": "...", "conversationToken": null }`,
  cabeceras `Idempotency-Key` UUID v4 y `Authorization: Bearer <jobAccessToken>`.
- `jobAccessToken`: 32 bytes aleatorios, Base64URL sin padding, generado y guardado por
  el cliente antes de enviar. La base almacena su SHA-256. No es una API key de OpenAI.
- Creación: HTTP 202 con Location y cuerpo de estado. Un replay devuelve el mismo ID:
  202 mientras está activo, 200 cuando terminó. Clave reutilizada con otro cuerpo: 409.
- `GET /api/analysis/jobs/{analysisId}` con el mismo Bearer devuelve el estado local y,
  al completar, result con analysis/clarification/conversationToken. No llama a OpenAI.
- GET de FAILED sigue siendo HTTP 200 con error. Un error HTTP de acceso o almacenamiento
  no implica que se haya repetido o cancelado el análisis. Todas las respuestas API llevan no-store.

La representación no expone consultas de entrada, hashes, claves de acceso, leases,
sessionId ni turnId. El POST busca un replay autenticado antes de revalidar el token de
conversación: puede recuperarse un trabajo aceptado aunque el token de entrada haya caducado.
La retención no se renueva al consultar. No hay cancelación remota ni reintento de inferencia.

## Ejecución y recuperación

El scheduler ejecuta pasos acotados cada segundo; mantiene hasta dos trabajos remotos
reservados. Un mismo proceso puede esperar varias ejecuciones remotas sin mantener una
petición HTTP abierta. Las escrituras SQL son transacciones breves; no contienen llamadas
HTTP. H2 usa una conexión serializada para este alcance de una sola instancia.

Se conserva la entrada/contexto aceptados, identidad del proveedor/revisión, fase,
referencias remotas, plazos y versión del lease. Los contextos y el resultado final se
confirman en la misma transacción. Una caída antes del commit no genera un resultado
parcial visible. La revisión del token final es la fijada en la entrada aceptada.

El adaptador OpenAI crea las sesiones nuevas CON input (requerido para environment.type=none),
usa events para seguimientos y GET para consultar estado. La creación inicial puede iniciar
inferencia: su huella se guarda antes y el worker pasa a polling sin otro envío por events. El envío incluye una clave remota estable y un identificador de correlación dentro
del JSON de entrada. Antes del envío se guarda la huella del input remoto exacto. El
worker no reenvía automáticamente un POST incierto: identifica la sesión por metadata
exacta y el turno por input correlacionado, o mantiene RECOVERING. Nunca adopta el último
turno por su fecha solamente. La huella permite reconciliar después de eliminar el texto.

Los fallos transitorios de lectura usan backoff y jitter, respetando Retry-After cuando
el proveedor lo aporta, hasta el plazo total. Cada operación remota está acotada a 15 s
(o al tiempo restante). No se extiende el plazo al reiniciar Java. Una respuesta inválida
falla; no se modifica ni provoca otra inferencia.

Un FAILED con remoto aún incierto mantiene la reserva y bloquea seguimientos sobre esa
sesión. Se sigue consultando en segundo plano para liberar la reserva al confirmar un
estado terminal, sin sustituir el resultado FAILED. Puede requerir intervención si no
se puede reconciliar. El vencimiento local no garantiza detener consumo remoto.

Al caducar, se borran entrada, contexto copiado y resultado; un marcador conserva clave,
hashes y datos mínimos de recuperación. La implementación guarda el marcador en la fila
`analysis_job` (`tombstone=true`), no en una tabla separada. Los snapshots tienen su propia
caducidad. Las referencias mínimas de conversación permiten detectar contexto obsoleto.
No es un servicio con múltiples instancias ni una garantía universal de exactamente una vez.

## Verificación de fase

- `AnalysisJobsTest`: base H2 en fichero, cierre/reapertura, replay concurrente, credenciales,
  contexto y token durables, resultado simulado después de 106 s, respuesta perdida,
  envío ambiguo, leases y worker atrasado, plazos, reserva remota, retención, límite de cola,
  seguimiento concurrente/obsoleto, cambio de revisión y fallo de commit con rollback.
- `OpenAiJobProviderTest`: servidor HTTP local; verifica input inicial al crear sesión,
  events en seguimientos, clave estable, correlación, respuesta perdida, ausencia de input
  en la recuperación y rechazo de salida inválida.
- `AnalysisJobsApiTest`: API HTTP real con perfil dev; 202, polling, resultado, replay,
  confidencialidad de campos, credenciales, conflictos y exclusión de la ruta síncrona.

Los 106 s se avanzan con reloj de prueba; no se espera ese tiempo ni se ejecuta inferencia.
Los reinicios de persistencia cierran y reabren una base en fichero; no son una prueba de
apagado físico del equipo. El fallo de guardado se inyecta dentro de una transacción real;
no se llena físicamente el disco. La compatibilidad del nuevo ciclo de envío con OpenAI
real queda pendiente de una fase posterior. La recuperación desde el navegador se verifica
en fase 3 con respuestas interceptadas y con Spring Boot dev (véase más abajo).

Resultado final: `mvn -q verify` correcto; 281 pruebas registradas, 279 superadas y
2 comprobaciones de navegador omitidas por no activar sus opciones. Los 28 casos nuevos
asíncronos se ejecutaron correctamente. Log local: `bootstrap/target/async-final-verification.log`.

## Frontend integrado — fase 3

La aplicación guarda en localStorage la clave UUID, credencial Base64URL de 32 bytes y
cuerpo exacto antes del primer POST. Recargar recupera el mismo trabajo; perder el 202
provoca un replay de la misma petición. Una vez guardado analysisId solo se hacen GET.
Las credenciales de trabajo se eliminan del historial al guardar un resultado completo;
el token de conversación permanece para seguimientos. El plazo de recuperación es 24 h,
sin renovarlo al recargar. Una solicitud no confirmada de más de 24 h no vuelve a enviarse.

HTTP tiene un límite de 15 s; polling sigue la indicación del servidor (3 s), con backoff
3–15 s y respeto de Retry-After. Las pestañas ocultas y la desconexión pausan el polling.
A los 90 s se informa de la demora sin terminar el seguimiento. Web Locks coordina
pestañas del mismo origen y las escrituras del historial; si no se puede persistir, no
se envían consultas nuevas. Borrar historial elimina acceso, no cancela trabajos remotos.
Los informes históricos, aclaraciones y posición del panel de publicaciones se conservan.

Revisión manual: enviar una consulta con dev/CLASSIFIED, recargar mientras aparece en
cola, esperar el informe, enviar un seguimiento y comprobar que permanece tras recargar.
No hay cambios en el agente ni nuevas inferencias de pago. No se modifica el fichero
local que contiene secretos; solo la configuración compartida y el ejemplo.

### Resultados de verificación de fase 3

- `npm run build`: TypeScript y Vite correctos.
- `mvn -q verify`: 279 pruebas superadas, 2 comprobaciones optativas de navegador omitidas.
- Las dos comprobaciones optativas se ejecutaron aparte: afirmaciones/aclaraciones y
  rechazo temporal. La segunda detectó un cambio de mensaje, se corrigió y su repetición
  terminó correctamente. No se usó un proveedor remoto real.
- `verify-async.cjs` con `LIVE_API=1`: 17 escenarios superados, incluida la conexión al
  worker Spring Boot dev y el seguimiento cifrado. Las pérdidas de red/202 se provocan
  por interceptación en Playwright; la espera superior a 90 s se comprueba envejeciendo
  el instante guardado. El timeout HTTP de 15 s se espera realmente.
- `verify-verdicts.cjs` y `verify-trace.cjs`: etiquetas/iconos, orden móvil/escritorio,
  referencias, historial antiguo y rechazo de trazas inválidas correctos.
- Capturas nuevas: `POC/webapp/review/integration/async-pending-{390,1536}.png` y
  `async-result-{390,1536}.png`. No se amplían las garantías a recuperación tras apagado
  físico, borrado de la base, limpieza automática del navegador ni exactitud del modelo.

Logs locales (ignorados por Git): `bootstrap/target/async-frontend-verification.log`,
`async-browser-verification.log`, `async-browser-contracts.log` y
`async-browser-temporal-final.log`. El log de contratos conserva el primer fallo de
mensaje; la repetición focalizada está en el último log.

## Prueba real — 30/09/2026

[Informe de la prueba y corrección previa](openai/async-live-review.md): una consulta,
un turno remoto, resultado mostrado tras recarga en unos 65 s. Se corrige la creación
vacía que permitía el mock: OpenAI exige input inicial para sesiones sin entorno.
La revisión semántica de las cinco fuentes queda pendiente. No se han ejecutado más
inferencias ni modificado las instrucciones del agente.
