# Contenedores con Podman: fase 1

## Estructura

Desde la carpeta POC, compose.yaml construye dos imagenes Linux:

- api: Java 26, compilacion Maven por etapas, ejecucion como usuario sin privilegios.
- web: compilacion Node/Vite y servicio Nginx como usuario sin privilegios en puerto 8080.
- analysis-data: volumen con nombre para H2. Replay usa /app/data/replay; dev y openai usan /app/data/cross-check.

El navegador accede a http://localhost:8088. Nginx envia /api/ a api:8080 por la red interna. Java escucha en 0.0.0.0 dentro del contenedor y no publica un puerto en el host. La ruta de la API es relativa; no hay secretos ni direcciones privadas en el JavaScript.

## Requisitos y arranque en Windows

Podman usa una maquina Linux sobre WSL2. En este equipo, podman compose delega en Docker Compose instalado como herramienta independiente, sin Docker Desktop. No establecer PODMAN_COMPOSE_PROVIDER=podman-compose: ese proveedor no esta instalado.

Comprobar:

```powershell
podman --version
podman machine list
podman compose version
```

Si no existe una maquina, ejecutar podman machine init; despues podman machine start. No crear otra si ya existe una adecuada.

La primera preparacion ya ha creado .env local con un secreto aleatorio de 32 bytes codificado en Base64 y perfil replay. No contiene una clave de OpenAI. En otra maquina, copiar .env.example a .env y generar ese secreto una sola vez; conservarlo entre reinicios. Ambos contextos de build excluyen los ficheros .env y la configuracion local del servicio.

Desde la carpeta POC (ejecutar `cd POC` si estas en la raiz del repositorio):

```powershell
podman compose up --build -d
podman compose ps
podman compose logs --tail=100 api web
```

Abrir http://localhost:8088. Crear una conversacion nueva y escribir exactamente:

> Zapatero ganó una comisión por su asesoramiento en caso de Plus Ultra

El escenario inicial es PLUS_ULTRA_V20. No se efectuan llamadas a OpenAI en replay.

## Cambiar de proveedor

En .env usar ANALYSIS_PROFILE=replay, dev u openai (uno solo). Para openai aportar OPENAI_API_KEY y OPENAI_POLITICAL_ANALYSIS_AGENT_ID, con POLITICAL_AGENT_REVISION=political-v21. No activar local: su fichero privado no esta en la imagen. Tras cambiar configuracion, ejecutar podman compose up -d --force-recreate y abrir otra conversacion.

.env contiene secretos; no ejecutar ni compartir salidas de compose config o inspecciones que los expongan. Las capturas replay incluidas son respuestas historicas.

## Persistencia y parada

```powershell
podman compose down
podman compose up -d
```

Esto conserva el volumen H2. No usar down -v si se quieren conservar los datos. El volumen vive en la maquina Linux de Podman: eliminar esa maquina tambien puede eliminarlo. Los ficheros H2 existentes en el checkout no se copian ni migran automaticamente.

Para revisar persistencia, completar una consulta, recrear los contenedores y recargar la misma conversacion antes de que expire su acceso. Usar el mismo secreto y no borrar el almacenamiento del navegador. Los datos persistidos siguen sujetos a la limpieza y caducidad del servicio.

## Memoria y alcance

Limites iniciales: api 640 MiB (heap Java maximo 384 MiB), web 96 MiB. No incluyen el sistema operativo ni el motor de contenedores. No constituyen una prueba de capacidad para una maquina de 1 GB. La compilacion Maven/Node requiere mas memoria y debe hacerse fuera del servidor pequeno de despliegue.

Las imagenes usan tags de version que pueden recibir actualizaciones. Una publicacion reproducible debera registrar o fijar sus digests tras validar las imagenes.

Esta fase publica el puerto solo en 127.0.0.1, para validacion local. El despliegue publico, HTTPS, copias y pruebas de carga son posteriores. No se han incorporado MySQL ni Kafka.

## Estado de validacion

Estado al 14/10/2026: Podman, WSL y Compose instalados. Maquina Linux arrancada y contenedor oficial de prueba ejecutado. Conexion reparada y confirmada desde el PowerShell del usuario. Compose valida correctamente. El usuario confirmo el arranque y funcionamiento correcto de la aplicacion con Compose tras configurar DOCKER_HOST. No se ha documentado aun una prueba especifica de persistencia H2 al recrear los contenedores ni mediciones de memoria. No se ha desplegado nada en AWS.

Validacion local: npm run build correcto; Maven verify correcto (306 pruebas registradas, 2 omitidas, sin fallos ni errores). git diff --check correcto. El usuario confirmo tambien el funcionamiento de la aplicacion en contenedores.

## Instalacion del entorno Windows

Instalados Podman 5.8.3, WSL 2.7.13 y Docker Compose 5.6.0 como proveedor independiente (sin Docker Desktop). En este equipo no establecer PODMAN_COMPOSE_PROVIDER=podman-compose: se utiliza docker-compose. Abrir una terminal nueva para recoger el PATH instalado. Validado docker-compose -f compose.yaml config --quiet sin mostrar secretos. La maquina Linux se inicializa con podman machine init y se arranca con podman machine start.

Verificacion de instalacion completada: podman-machine-default arrancada en modo rootless (linux/amd64), podman compose version correcto y contenedor oficial quay.io/podman/hello ejecutado y eliminado con --rm. No ha sido necesario reiniciar Windows. La construccion y el arranque fueron confirmados posteriormente por el usuario; ver la incidencia DOCKER_HOST mas abajo.

Compose y sus ficheros .env.example, .env local y .gitignore residen en POC. Los contextos de construccion son ./cross-check-service y ./webapp. Los Containerfiles permanecen en sus respectivas aplicaciones. Todos los comandos compose de esta guia se ejecutan desde POC.

## Incidencia resuelta: maquina arrancada, pero conexion no encontrada

Durante la instalacion del 13/10/2026 aparecieron estos errores en el PowerShell del usuario:

```text
Cannot connect to Podman.
dial unix /run/podman/podman.sock: connect: A socket operation encountered a dead network.
Error: read cli flags: connection "podman-machine-default" not found
```

`podman machine list` mostraba la maquina arrancada. Sin embargo, `Test-Path` devolvia `False` para el archivo de conexiones:

```powershell
Test-Path "$env:APPDATA\containers\podman-connections.json"
podman system connection list
```

El entorno de automatizacion veia ese archivo, pero el PowerShell del usuario no lo veia, aun coincidiendo usuario y rutas. No se determino la causa de esa diferencia de visibilidad. Faltaba registrar la conexion a la maquina existente en el entorno del usuario. Reiniciar la terminal o indicar el nombre de conexion no resolvio el problema. Tampoco basto con apuntar PODMAN_CONNECTIONS_CONF a un archivo inexistente.

### Solucion aplicada

Ejecutar el diagnostico en la terminal que presenta el error. Obtener los datos actuales de la maquina sin mostrar el contenido de la clave privada:

```powershell
podman machine inspect podman-machine-default --format '{{.SSHConfig.Port}} {{.SSHConfig.RemoteUsername}} {{.SSHConfig.IdentityPath}}'
```

En este caso: puerto 63945, usuario user y la ruta de identidad siguiente. Se registro la conexion con:

```powershell
podman system connection add --default `
  --identity "C:\Users\jimen\.local\share\containers\podman\machine\machine" `
  podman-machine-default `
  "ssh://user@127.0.0.1:63945/run/user/1000/podman/podman.sock"

podman system connection list
podman compose version
```

El usuario confirmo que Compose devolvia su version tras este registro. No fue necesario reinstalar Podman, recrear la maquina ni borrar datos. La conexion queda registrada en el archivo de configuracion.

El puerto y la ruta documentan el caso resuelto: comprobar machine inspect antes de reutilizarlos en otro entorno. El socket corresponde a esta maquina rootless (UID Linux 1000). Si la conexion ya existe, no repetir connection add sin comprobar su URI; para seleccionar una existente, usar podman system connection default <nombre>.

Durante el diagnostico se habia definido en esa sesion:

```powershell
$env:PODMAN_CONNECTIONS_CONF = "$env:APPDATA\containers\podman-connections.json"
```

Esta variable selecciona el archivo; no crea conexiones. No se cambio permanentemente. En una terminal nueva, comprobar system connection list y compose version. Si se usa una ruta personalizada, mantener la misma seleccion de archivo.

### Instalacion realizada y permisos

Se instalaron RedHat.Podman, Microsoft.WSL y Docker.DockerCompose mediante winget. Usar --source winget evito la solicitud interactiva de terminos del origen Microsoft Store. WSL fallo inicialmente con 0x80073d28 por requerir permisos de administrador; se completo mediante elevacion de Windows. No fue necesario reiniciar este equipo. Abrir una terminal nueva permite recoger los cambios del PATH.

Comprobaciones realizadas:

```powershell
podman --version
wsl --version
podman machine list
podman compose version
podman run --rm quay.io/podman/hello
```

El contenedor oficial de prueba funciono y se elimino al terminar mediante --rm. El usuario confirmo posteriormente el funcionamiento de CrossCheck desde POC. La prueba especifica de persistencia H2 al recrear los contenedores sigue sin documentarse.

## Incidencia resuelta: Compose pide una clave SSH

Al ejecutar podman compose up --build -d, el proveedor Compose solicito:

```text
user@127.0.0.1's password:
```

Compose intentaba conectarse mediante SSH. El canal local de Windows expuesto por la maquina Podman permitio conectar sin esa solicitud. Cancelar el prompt con Ctrl+C y ejecutar desde POC:

```powershell
$env:DOCKER_HOST = "npipe:////./pipe/docker_engine"
podman compose ps
podman compose up --build -d
```

El nombre docker_engine identifica el canal compatible con la API Docker que proporciona Podman en este equipo; Docker Desktop no esta instalado. La maquina Podman debe estar arrancada para que ese canal este disponible. El usuario confirmo que todo funcionaba correctamente con este ajuste el 14/10/2026.

### Guardar el ajuste para el usuario de Windows

Para conservarlo en futuras sesiones, ejecutar una vez en PowerShell:

```powershell
[Environment]::SetEnvironmentVariable(
    "DOCKER_HOST",
    "npipe:////./pipe/docker_engine",
    "User"
)
```

No requiere permisos de administrador. Abrir una terminal nueva; si Windows Terminal o IntelliJ siguen heredando el entorno anterior, cerrar y volver a abrir la aplicacion completa. Para aplicarlo tambien a la terminal actual:

```powershell
$env:DOCKER_HOST = "npipe:////./pipe/docker_engine"
```

Este ajuste afecta a las herramientas compatibles con Docker ejecutadas por ese usuario, no solo a este proyecto. Es adecuado mientras se utilice este motor Podman local. Si se cambia a otro motor o servidor remoto, revisar la variable. No colocarla en POC/.env: ese fichero configura los servicios del proyecto; aqui necesitamos configurar el cliente Compose antes de que conecte con el motor.

Para comprobarlo en una terminal nueva:

```powershell
$env:DOCKER_HOST
podman compose ps
```

Para quitar la configuracion persistente y la de la terminal actual:

```powershell
[Environment]::SetEnvironmentVariable("DOCKER_HOST", $null, "User")
Remove-Item Env:DOCKER_HOST -ErrorAction SilentlyContinue
```

La opcion persistente queda documentada; no se ha aplicado automaticamente al equipo.

## Swagger

Documentacion disponible tras reconstruir las imagenes: http://localhost:8088/api/swagger-ui.html; JSON en /api/docs. API_DOCS_ENABLED controla ambos recursos. Compose local la habilita por defecto; configurar false para desactivarla. Instrucciones de uso y validacion en [swagger-api.md](swagger-api.md).
