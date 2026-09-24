# Fase 5 · Integración OpenAI preparada para validación real

El backend dispone de un adaptador para **Agents API**, con agente guardado y sesión independiente por conversación, conforme a la arquitectura del proyecto. Se mantiene el contrato `POST /api/analysis/start` y el cifrado del identificador de sesión. No se ha creado ningún agente remoto ni realizado llamadas reales en esta entrega.

## Modos de ejecución

| Perfiles | Comportamiento |
|---|---|
| Sin perfil | Salud; API de análisis desactivada. |
| `dev,local` | Proveedor simulado; no necesita credenciales OpenAI. |
| `openai,local` | Proveedor OpenAI; exige clave e identificador de agente. |

`dev` y `openai` son excluyentes. `local` carga el fichero externo `config/application-local.yml` desde el directorio de trabajo del servicio. No hay cambio automático al proveedor simulado si OpenAI falla.

En el fichero local, excluido de Git:

```yaml
CONVERSATION_TOKEN_SECRET: 'tu_clave_base64_actual'
OPENAI_API_KEY: 'tu_clave_de_api'
OPENAI_POLITICAL_ANALYSIS_AGENT_ID: 'identificador_del_agente_guardado'
```

Las variables de entorno tienen prioridad sobre el fichero. La clave no se incluye en los DTO, el frontend, el JAR, los logs ni los mensajes de error. Las pruebas utilizan credenciales ficticias y servidores locales.

## Preparación del agente pendiente

1. Activar facturación, comprobar acceso de la cuenta a Agents API y los permisos de la clave. La guía oficial indica `api.agents.read`, `api.agents.write` y `api.responses.write`.
2. Elegir un modelo disponible en la cuenta y revisar costes. No se ha fijado un modelo por defecto ni se ha probado su compatibilidad real.
3. Revisar [las instrucciones](openai/political-analysis-instructions.md) y [la definición del agente](openai/political-agent.template.json). Incluye búsqueda web `live` y un esquema JSON compatible con `AnalysisReport`. Las instrucciones están en inglés; el informe se solicita en español.
4. Desde la carpeta del servicio, ejecutar `./scripts/prepare-openai-agent.ps1 -Model 'MODELO_ELEGIDO'`. Genera `bootstrap/target/political-agent.request.json`, sustituyendo modelo e instrucciones. **Solo prepara un fichero: no lee la clave ni hace llamadas de red.**
5. En la sesión de configuración posterior, crear el agente con ese JSON mediante `POST https://api.openai.com/v1/agents`, autenticación Bearer y cabecera `OpenAI-Beta: agents=v1`. Guardar el identificador devuelto en `OPENAI_POLITICAL_ANALYSIS_AGENT_ID`. El servicio no crea ni modifica agentes durante las consultas.
6. Activar `openai,local` en IntelliJ. Mantener Working directory en `POC/cross-check-service` y usar Program arguments `--spring.profiles.active=openai,local`.

Conservar versiones nuevas del agente cuando cambien sus instrucciones o esquema. Cambiar también `POLITICAL_AGENT_REVISION`; el valor inicial del perfil OpenAI es `political-v1`, distinto de `development-v1`, para evitar reutilizar tokens simulados. Si modificas el agente remoto sin cambiar su revisión local, las sesiones antiguas pueden conservar su configuración anterior.

## Flujo implementado

La primera petición crea una sesión con `agent_id`, `environment.type=none` y la consulta como entrada de usuario. No requiere sandbox ni ejecución de código. En los seguimientos, el adaptador comprueba que la sesión está inactiva antes de enviar otro mensaje. Mantiene su identificador estable, protegido por el token cifrado existente.

El servicio consulta el estado guardado del turno hasta que termina. Un estado de sesión `idle` no se interpreta como éxito. En un seguimiento espera un identificador de turno nuevo, para no devolver accidentalmente el resultado anterior. Solo acepta mensajes finales completos del turno correspondiente; omite comentarios y resultados de herramientas. Recorre las páginas de mensajes con un límite de 20 páginas de 100 elementos.

La salida JSON se convierte al dominio existente, validando campos, tipos, enumeraciones, URLs, identificadores, duplicados y referencias internas. Una salida inválida devuelve un error técnico; no se transforma en un veredicto de evidencia insuficiente. La validación de estructura no demuestra la calidad factual del análisis: esa comprobación forma parte de las pruebas reales pendientes.

El transporte utiliza `java.net.http.HttpClient` del JDK y Jackson administrado por Spring Boot. Se ha optado por un cliente HTTP acotado a las operaciones necesarias, sin introducir un SDK adicional. La integración beta queda aislada en `infrastructure/openai` y sus peticiones se comprueban contra un servidor HTTP local. No se sigue ninguna redirección ni se permite configurar otra URL de proveedor en la aplicación.

## Límites y errores

- Plazo total por consulta: 90 segundos, incluyendo envío, espera y lectura de resultados. Configurable con `crosscheck.openai.timeout`; mantenerlo por debajo del plazo del frontend/proxy (120 segundos por defecto).
- Consulta de estado cada segundo: `crosscheck.openai.poll-interval`.
- Hasta dos consultas simultáneas por instancia: `crosscheck.openai.max-concurrent-requests`. Un seguimiento simultáneo de la misma sesión se rechaza en esa instancia.
- Cuerpos HTTP limitados a 2 MB por operación e informe JSON a 250.000 caracteres.
- Sin reenvío automático de mensajes ni creación automática de otra sesión tras un fallo. Las consultas de estado solo leen el progreso.
- Un timeout o desconexión puede dejar trabajo en ejecución en OpenAI. No se promete cancelación remota ni recuperación automática; reintentar puede repetir trabajo. Una primera consulta fallida puede dejar una sesión remota sin referencia en el navegador.
- Credenciales rechazadas, límites del proveedor y otros fallos se traducen al contrato de errores existente, sin propagar el cuerpo del proveedor. Los fallos de sesión y los conflictos se distinguen de la indisponibilidad.
- Se registran únicamente los recuentos de tokens de entrada y salida cuando el proveedor los ofrece; son métricas orientativas y no una factura ni un límite de gasto.

El control de concurrencia no es un límite monetario ni una exclusión entre múltiples instancias. Antes de publicar la demo quedan por revisar acceso de usuarios, límites de frecuencia y presupuesto. El servicio continúa escuchando en `127.0.0.1` por defecto.

## Validación

`mvn verify` ejecuta pruebas sin llamar a OpenAI: creación y seguimiento, espera de turno nuevo, paginación, errores HTTP, timeout, límites de tamaño/concurrencia, salida malformada, referencias inválidas y configuración de perfiles. Se mantienen las pruebas anteriores del contrato HTTP y del cifrado.

Resultado de esta entrega: **151 pruebas Java correctas**, ninguna omitida; `npm run build` correcto. El recorrido HTTP del perfil `openai` se comprobó con un servidor proveedor local, incluyendo consulta, seguimiento y cifrado de la sesión. También se corrigió una aserción antigua que buscaba el texto corto `v1` en un cifrado aleatorio y podía fallar por coincidencia; ahora comprueba el encabezado público del JWE.

La interfaz mantiene el mismo contrato. Sus textos ya no afirman que todas las respuestas son simuladas; explican ambos modos y que borrar el historial local no elimina sesiones remotas.

**Pendiente con credenciales:** confirmar acceso y permisos, crear el agente, validar modelo/esquema/búsqueda, medir tiempos y costes, revisar fuentes y calidad factual, ejecutar una consulta y un seguimiento desde el frontend, y comprobar errores reales de cuota y autenticación de forma controlada. La implementación no equivale a una validación real del proveedor.

## Referencias oficiales consultadas

- [Configuración de agentes guardados](https://developers.openai.com/api/docs/guides/agents-api/configuration).
- [Preparación y permisos](https://developers.openai.com/api/docs/guides/agents-api/quickstart).
- [Sesiones y seguimientos](https://developers.openai.com/api/docs/guides/agents-api/sessions).
- [Turnos, mensajes y paginación](https://developers.openai.com/api/docs/guides/agents-api/sessions/events).
- [Búsqueda web del agente](https://developers.openai.com/api/docs/guides/agents-api/tools/web-search).
- [Referencia Agents API, incluido formato JSON Schema](https://developers.openai.com/api/reference/resources/beta/subresources/agents).

Agents API requiere actualmente la cabecera beta `OpenAI-Beta: agents=v1`. El acceso y comportamiento reales de esta cuenta no se han comprobado.
