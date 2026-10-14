# OpenAPI y Swagger UI

Integracion mediante springdoc-openapi-starter-webmvc-ui 3.1.1, compatible con Spring Boot 4. La documentacion se genera desde los controladores y los DTO del servicio; no sustituye las reglas de validacion de Java.

## Acceso

Con Java desde IntelliJ (puerto 8080 por defecto):

- Interfaz: http://localhost:8080/api/swagger-ui.html
- Contrato JSON: http://localhost:8080/api/docs

Con Podman y Nginx:

- Interfaz: http://localhost:8088/api/swagger-ui.html
- Contrato JSON: http://localhost:8088/api/docs

Las rutas comienzan por /api y atraviesan el proxy existente de Nginx. El esquema usa un servidor relativo / para ejecutar las llamadas en el mismo origen, tanto con Java directo como a traves de Nginx.

Para incorporar el cambio a los contenedores existentes, ejecutar desde POC:

```powershell
podman compose up --build -d
```

Si el cliente Compose vuelve a pedir una clave SSH, usar la configuracion DOCKER_HOST documentada en contenedores-podman.md.

## Activar o desactivar

Variable de entorno: API_DOCS_ENABLED=true o false. Controla tanto la interfaz como el contrato JSON; cambiarla requiere reiniciar Java o recrear el contenedor.

- dev y replay: activados por defecto.
- openai y configuracion general: desactivados por defecto.
- Compose local: activados por defecto mediante API_DOCS_ENABLED. Para desactivarlos, poner API_DOCS_ENABLED=false en POC/.env y ejecutar podman compose up -d --force-recreate.

El despliegue publico debera establecer API_DOCS_ENABLED=false o proteger el acceso. La interfaz documenta la API, no incorpora autenticacion de usuarios. El bearer de los trabajos es una credencial de acceso al trabajo.

## Probar trabajos asincronos

1. Preferir replay o dev para pruebas sin consumo del proveedor. En openai, un POST real puede generar consumo OpenAI.
2. Generar una credencial aleatoria de 32 bytes codificada como Base64url sin padding. Esta credencial NO es CONVERSATION_TOKEN_SECRET, una API key ni una cuenta de usuario.
3. Pulsar Authorize e introducir la credencial, sin el prefijo Bearer. Swagger lo incorpora al header Authorization. No se persiste al recargar la interfaz.
4. En POST /api/analysis/jobs indicar Idempotency-Key como UUID v4 y un body con text; conversationToken puede ser null en el primer turno.
5. La respuesta 202 incluye analysisId, Location y Retry-After. Repetir GET /api/analysis/jobs/{id} con la misma credencial hasta COMPLETED o FAILED. Un GET no inicia otra consulta al proveedor.
6. COMPLETED puede contener un informe o una aclaracion. FAILED contiene un error. Para reintentar el POST original, conservar clave, body y bearer; cambiar el body con la misma clave produce conflicto. Para otra consulta usar otra clave.

En el replay inicial usar exactamente: Zapatero ganó una comisión por su asesoramiento en caso de Plus Ultra.

Ejemplo de generacion local en PowerShell:

```powershell
$credentialBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($credentialBytes)
$jobCredential = [Convert]::ToBase64String($credentialBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
$jobCredential
[guid]::NewGuid().ToString()
```

La credencial generada se usa solo en tu interfaz local y no debe compartirse. Si se pierde, no permite recuperar el trabajo asociado mediante otra credencial.

## Endpoint antiguo

POST /api/analysis/start se muestra como deprecated y explica que devuelve 409 ASYNC_ANALYSIS_REQUIRED cuando la cola asincrona esta habilitada, como ocurre por defecto. El frontend utiliza /api/analysis/jobs.

## Validacion

Maven verify correcto. Comprobacion HTTP sobre el JAR en dev: JSON OpenAPI con rutas de trabajos, bearer, Idempotency-Key requerido y esquemas View; endpoint antiguo marcado deprecated; actuador excluido del contrato. Swagger UI, su configuracion y recursos JavaScript cargan bajo /api. Con API_DOCS_ENABLED=false, contrato e interfaz devuelven 404. Sin llamadas a OpenAI y procesos de prueba cerrados al finalizar.

La comprobacion HTTP se hizo con Java directo; la imagen desplegada debe reconstruirse para incluir la dependencia nueva. El proxy conserva /api/ y no requiere rutas adicionales de Nginx.
