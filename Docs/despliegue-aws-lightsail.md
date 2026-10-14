# Despliegue beta en AWS Lightsail

Preparacion del 14/10/2026. Dominio indicado: www.contrastaloquetecuenten.com. Instancia aun no creada. Los archivos estan en POC/deploy/lightsail; esta preparacion no crea recursos AWS ni modifica el DNS.

## Capacidad y plan

Lightsail Linux con IPv4 publica: 512 MB por 5 USD/mes, 1 GB por 7 USD/mes. Son precios base; copias, impuestos y otros servicios pueden sumar costes. Fuente: https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-bundles.html

El perfil preparado inicialmente contempla 1 GB: Java con heap maximo de 256 MiB y limite de contenedor de 480 MiB, Nginx 64 MiB y Caddy 96 MiB. El sistema operativo requiere memoria adicional. No se ha certificado una capacidad de 512 MB. Medir antes de contratar el minimo; ajustar limites sin una prueba puede causar OOM. Un ensayo local con limites de contenedor tampoco reproduce toda la memoria del host.

Compilar las imagenes en el ordenador local y transferirlas al servidor. No compilar Maven/Node en la instancia pequena. Una sola instancia ejecuta la API y H2. Configuracion inicial replay, concurrencia 1 y cola de 5; openai se activa despues de validar el despliegue.

## Arquitectura y acceso

Internet -> Caddy (80/443, HTTPS) -> Nginx (interno) -> Java (interno) -> volumen H2.

- API y web no publican puertos directamente en este despliegue.
- Caddy obtiene y renueva certificados; requiere DNS correcto y acceso exterior a 80/443.
- Toda la aplicacion, incluida la API, requiere usuario y password HTTP validado por Caddy sobre HTTPS. El frontend envia la credencial del trabajo en X-Job-Credential para dejar Authorization disponible para Basic. Caddy retira el header Basic antes de reenviar a Java. No hay restriccion por IP para los probadores.
- El limite de creacion de trabajos en Nginx es 2 solicitudes/minuto con burst de 3. Como Nginx recibe trafico de un unico Caddy, este limite es compartido por los probadores; polling GET no consume ese limite. Puede limitar tambien reintentos POST. No es un tope monetario del proveedor.
- Swagger desactivado en el servidor. /healthz comprueba Nginx y queda tras la proteccion de acceso; Compose comprueba Java internamente.
- .env del servidor se conserva privado, separado del .env local; nunca transferir secretos dentro de una imagen o del paquete de release.

## 1. Preparar un release local

Desde POC, con la maquina Podman arrancada:

```powershell
./deploy/lightsail/prepare-release.ps1 -ReleaseTag beta-20261014-1
```

Si la politica de scripts impide ejecutarlo, usar una excepcion solo para esa ejecucion desde POC:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File ./deploy/lightsail/prepare-release.ps1 -ReleaseTag beta-20261014-1
```

El script compila y verifica Java, compila la web para linux/amd64, exporta api.tar y web.tar y copia archivos de despliegue y SHA256SUMS. El resultado esta en deploy/lightsail/artifacts/<tag>, excluido de Git. El directorio no debe existir; usar otro tag para otro release. No contiene .env real ni claves. Las etiquetas base pueden actualizarse: el paquete exportado y sus huellas identifican el artefacto efectivamente desplegado.

## 2. Crear Lightsail

En la consola AWS elegir una region europea disponible, Linux/Unix y Ubuntu 24.04 LTS sin pila preinstalada. Seleccionar plan tras decidir la capacidad. Nombre sugerido crosscheck-beta. Crear y asociar una IP estatica.

Firewall Lightsail: abrir TCP 80 y 443; restringir SSH 22 a la IP del administrador. Mantener API 8080 sin publicar. Si se habilita IPv6, configurar tambien sus reglas; no crear un registro AAAA si no se ha probado esa ruta. Crear aviso de presupuesto AWS; los avisos no detienen automaticamente el gasto.

Fuentes: https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-firewall-and-port-mappings-in-amazon-lightsail.html y https://caddyserver.com/docs/automatic-https

## 3. DNS en Hostalia

Crear registro A de www.contrastaloquetecuenten.com hacia la IP estatica. Si www tiene un CNAME incompatible, sustituir solo ese registro. Conservar los registros de correo. Esta configuracion atiende www; el dominio sin www no se incluye automaticamente.

Comprobar la resolucion antes de iniciar Caddy. Mantener abiertos 80 y 443 para emision y renovacion del certificado.

## 4. Transferir e instalar

Usar la clave SSH de Lightsail desde tu equipo y scp/SFTP para copiar el contenido del directorio del release a un directorio del usuario ubuntu en el servidor. No enviar la clave privada al servidor. Los siguientes comandos se ejecutan alli, desde el directorio recibido:

```bash
sha256sum -c SHA256SUMS
sudo bash install-runtime.sh
sudo cp compose.yaml Caddyfile nginx.conf backup-h2.sh /opt/crosscheck/
sudo podman load -i api.tar
sudo podman load -i web.tar
sudo podman pull docker.io/library/caddy:2-alpine
```

El instalador requiere Ubuntu 24.04, instala Podman y podman-compose y los servicios systemd. El servidor usa Podman rootful para publicar 80/443; Java y Nginx siguen ejecutandose con usuarios sin privilegios dentro de los contenedores. No mezclar sudo podman con podman sin sudo, pues utilizan almacenes distintos. No usar DOCKER_HOST de Windows en Linux.

## 5. Configurar secretos en el servidor

```bash
sudo cp .env.example /opt/crosscheck/.env
sudo chmod 600 /opt/crosscheck/.env
sudoedit /opt/crosscheck/.env
```

Completar DOMAIN, ACME_EMAIL, RELEASE_TAG, BETA_USERNAME, BETA_PASSWORD_HASH y CONVERSATION_TOKEN_SECRET. Usar un secreto de conversacion nuevo y conservarlo para reinicios y restauraciones. Generar sus 32 bytes aleatorios codificados Base64:

```bash
openssl rand -base64 32
```

Generar el hash de password de acceso (la herramienta pide el password de forma interactiva):

```bash
sudo podman run --rm -it docker.io/library/caddy:2-alpine caddy hash-password
```

Guardar el hash entre comillas simples en .env para conservar los caracteres $:

```text
BETA_PASSWORD_HASH='$2a$...hash completo...'
```

La primera validacion usa ANALYSIS_PROFILE=replay. Para openai, aportar OPENAI_API_KEY, OPENAI_POLITICAL_ANALYSIS_AGENT_ID y POLITICAL_AGENT_REVISION=political-v21. No activar local ni compartir salidas de compose config o podman inspect que incluyan secretos.

## 6. Arranque y comprobacion

```bash
cd /opt/crosscheck
sudo env PODMAN_COMPOSE_PROVIDER=/usr/bin/podman-compose podman compose config --quiet
sudo systemctl enable --now crosscheck-beta.service
sudo env PODMAN_COMPOSE_PROVIDER=/usr/bin/podman-compose podman compose ps
sudo podman stats --no-stream
```

Abrir https://www.contrastaloquetecuenten.com con el usuario y password de beta. Comprobar password de acceso, redireccion HTTP a HTTPS, etiqueta beta, informe replay y recarga de una consulta completada. Probar la web y /api/analysis/jobs sin credenciales y con password incorrecto: deben responder 401 antes de llegar a Java. Verificar memoria, logs sin errores, persistencia y recuperacion de jobs al reiniciar Java. Reiniciar la instancia de forma controlada para validar el arranque automatico.

Para cambiar .env, recrear servicios mediante systemctl restart crosscheck-beta.service. Esto causa una breve interrupcion y conserva volumen. Solo despues de estas pruebas activar openai y efectuar una prueba real de consumo controlado.

## 7. Copias y restauracion

El script backup-h2.sh detiene brevemente Java, exporta el volumen H2 y vuelve a arrancarlo incluso si la exportacion falla. Los trabajos se conservan pero la API queda temporalmente indisponible. No copiar una H2 activa como si fuera una copia consistente.

```bash
sudo bash /opt/crosscheck/backup-h2.sh
sudo systemctl enable --now crosscheck-backup.timer
```

El timer ejecuta la copia a las 04:00 UTC. Guarda archivos con permisos privados en /var/backups/crosscheck. No elimina copias automaticamente: revisar espacio y definir retencion. Transferir copias cifradas fuera de la instancia y conservar por separado el secreto de conversacion y los datos de configuracion. Una copia en el mismo disco no protege frente a perder la instancia.

Restaurar primero en un entorno de prueba. En la instancia destino, parar la aplicacion y conservar una copia previa del volumen antes de importar:

```bash
sudo systemctl stop crosscheck-beta.service
if sudo podman volume exists crosscheck-beta-analysis-data; then
  echo "Existing volume: preserve it and use a fresh restore environment."
else
  sudo podman volume create crosscheck-beta-analysis-data
  sudo podman volume import crosscheck-beta-analysis-data /RUTA_ABSOLUTA/COPIA_VERIFICADA.tar && sudo systemctl start crosscheck-beta.service
fi
```

La importacion debe hacerse sobre un volumen nuevo o vacio, no mezclar datos de dos bases. No ejecutar un borrado automatico de volumen sobre la instancia en uso. Probar la restauracion antes de considerar fiable el plan de copias.

## 8. Actualizacion y vuelta al release anterior

Guardar release tag y paquete anterior. Hacer copia H2, parar el servicio, cargar las imagenes nuevas, reemplazar archivos de despliegue y actualizar RELEASE_TAG en .env; arrancar de nuevo. Conservar .env, volumen H2 y datos de Caddy. Verificar salud e informe. Para revertir imagenes, usar el tag anterior; si hubiera cambios incompatibles de esquema, requerir tambien una restauracion compatible de la base de datos.

## Estado y limites de esta preparacion

Validados localmente: Compose con valores ficticios, Caddy y Nginx mediante sus validadores reales, sintaxis de scripts Bash y PowerShell. No se han creado recursos AWS, modificado DNS, emitido certificados del dominio ni ejecutado OpenAI. No se ha probado Ubuntu/systemd real, copias/restauracion ni capacidad de 512 MB. Los contenedores locales actuales no se han recreado ni sus volumenes modificado.

Medicion puntual del entorno local el 14/10/2026: API aproximadamente 202 MB y web 8 MB. No incluye Caddy ni memoria del sistema operativo, carga simultanea o picos; no acredita viabilidad de 512 MB. El timer de copia fue validado con systemd-analyze calendar para 04:00 UTC.

Actualizacion de proteccion: lista de IP retirada por decision del usuario. Prueba integral en contenedores aislados: web/API sin credenciales y password incorrecto devuelven 401; navegador autenticado crea y consulta un trabajo COMPLETED usando X-Job-Credential. Maven verify y build frontend correctos; sin consumo OpenAI. Entorno de prueba eliminado al terminar. TLS real del dominio sigue pendiente del despliegue AWS.
