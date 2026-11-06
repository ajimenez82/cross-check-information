# Despliegue beta en AWS Lightsail

Preparacion actualizada el 19/10/2026. Dominio indicado: www.contrastaloquetecuentan.com. Instancia creada, segun confirmacion del usuario: alejimenez.dev, region Europa (Espana), eu-south-2, con IP estatica 15.216.113.27. Los archivos estan en POC/deploy/lightsail; esta preparacion no crea recursos AWS ni modifica el DNS.

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

En la consola AWS elegir una region europea disponible, Linux/Unix y Ubuntu 24.04 LTS sin pila preinstalada. Seleccionar plan tras decidir la capacidad. Instancia elegida: alejimenez.dev. IP estatica asociada: 15.216.113.27. Plan acordado: 1 GB con IPv4 publica.

Firewall Lightsail: abrir TCP 80 y 443; restringir SSH 22 a la IP del administrador. Mantener API 8080 sin publicar. Si se habilita IPv6, configurar tambien sus reglas; no crear un registro AAAA si no se ha probado esa ruta. Crear aviso de presupuesto AWS; los avisos no detienen automaticamente el gasto.

Fuentes: https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-firewall-and-port-mappings-in-amazon-lightsail.html y https://caddyserver.com/docs/automatic-https

## 3. DNS en Hostalia

En la zona DNS de contrastaloquetecuentan.com, crear un registro A con nombre www y valor 15.216.113.27 (IP estatica). Si www tiene un CNAME incompatible, sustituir solo ese registro. Conservar los registros de correo y los demas registros DNS. La configuracion publica solo www.contrastaloquetecuentan.com; contrastaloquetecuentan.com sin www no se redirige ni sirve automaticamente.

Comprobar la resolucion antes de iniciar Caddy. Mantener abiertos 80 y 443 para emision y renovacion del certificado.

## 4. Transferir e instalar

Usar la clave SSH de Lightsail desde tu equipo y scp/SFTP para copiar el contenido del directorio del release a un directorio del usuario ubuntu en el servidor. No enviar la clave privada al servidor. Los siguientes comandos se ejecutan alli, desde el directorio recibido:

```bash
sha256sum -c SHA256SUMS
sudo bash install-runtime.sh
sudo cp compose.yaml Caddyfile nginx.conf start-web.sh backup-h2.sh /opt/crosscheck/
sudo cp -r sites /opt/crosscheck/
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

Completar DOMAIN, ACME_EMAIL, RELEASE_TAG y CONVERSATION_TOKEN_SECRET. Usar un secreto de conversacion nuevo y conservarlo para reinicios y restauraciones. Generar sus 32 bytes aleatorios codificados Base64:

```bash
openssl rand -base64 32
```

El acceso web es público: Caddy mantiene HTTPS sin autenticación básica.

La primera validacion usa ANALYSIS_PROFILE=replay. Para openai, aportar OPENAI_API_KEY, OPENAI_POLITICAL_ANALYSIS_AGENT_ID y POLITICAL_AGENT_REVISION=political-v21. No activar local ni compartir salidas de compose config o podman inspect que incluyan secretos.

## 6. Arranque y comprobacion

```bash
cd /opt/crosscheck
sudo env PODMAN_COMPOSE_PROVIDER=/opt/crosscheck-tools/bin/podman-compose podman compose config --quiet
sudo systemctl enable --now crosscheck-beta.service
sudo env PODMAN_COMPOSE_PROVIDER=/opt/crosscheck-tools/bin/podman-compose podman compose ps
sudo podman stats --no-stream
```

Abrir https://www.contrastaloquetecuentan.com con el usuario y password de beta. Comprobar password de acceso, redireccion HTTP a HTTPS, etiqueta beta, informe replay y recarga de una consulta completada. Probar la web y /api/analysis/jobs sin credenciales y con password incorrecto: deben responder 401 antes de llegar a Java. Verificar memoria, logs sin errores, persistencia y recuperacion de jobs al reiniciar Java. Reiniciar la instancia de forma controlada para validar el arranque automatico.

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

Tras el arranque, esperar tanto a API y web saludables como a que HTTPS responda correctamente en `/healthz`. Caddy puede tardar unos segundos adicionales en aceptar conexiones aunque los otros contenedores ya esten saludables. Reintentar la comprobacion HTTPS durante un plazo acotado antes de activar la vuelta al release anterior.

## Estado y limites de esta preparacion

Validados localmente: Compose con valores ficticios, Caddy y Nginx mediante sus validadores reales, sintaxis de scripts Bash y PowerShell. El usuario ha creado la instancia y asociado la IP estatica; estos recursos no fueron creados desde la automatizacion. No se ha confirmado aun el cambio DNS, emision de certificados del dominio ni despliegue remoto. No se ha ejecutado OpenAI en estas validaciones. No se ha probado Ubuntu/systemd real, copias/restauracion ni capacidad de 512 MB. Los contenedores locales actuales no se han recreado ni sus volumenes modificado.

Medicion puntual del entorno local el 14/10/2026: API aproximadamente 202 MB y web 8 MB. No incluye Caddy ni memoria del sistema operativo, carga simultanea o picos; no acredita viabilidad de 512 MB. El timer de copia fue validado con systemd-analyze calendar para 04:00 UTC.

Actualizacion de proteccion: lista de IP retirada por decision del usuario. Prueba integral en contenedores aislados: web/API sin credenciales y password incorrecto devuelven 401; navegador autenticado crea y consulta un trabajo COMPLETED usando X-Job-Credential. Maven verify y build frontend correctos; sin consumo OpenAI. Entorno de prueba eliminado al terminar. TLS real del dominio sigue pendiente del despliegue AWS.

## Varios subdominios en la misma instancia

Dominio principal: contrastaloquetecuentan.com. Sitio actual: www.contrastaloquetecuentan.com. Caddyfile contiene opciones globales e importa sites/*.caddy; sites/contrasta.caddy contiene el sitio actual y su proteccion con password.

Para otra aplicacion, crear otro archivo .caddy con su dominio y reverse_proxy a su contenedor web, anadir sus servicios a la red accesible por Caddy y apuntar su registro A a la misma IP estatica. Solo Caddy publica 80/443: los demas contenedores tienen puertos internos. Cada aplicacion debe tener volumenes y configuracion propios. Si se separan las aplicaciones en proyectos Compose distintos, definir una red externa compartida para sus frontends y Caddy antes de enlazarlos; los servicios Java y las bases no necesitan compartir esa red.

El paquete de release incluye sites y sus huellas. Al actualizar, conservar los archivos de sitios adicionales que se hayan creado en el servidor; no sustituir ni borrar la carpeta completa a ciegas. Reiniciar el servicio Caddy cuando cambien sus archivos. El Caddy actual sigue formando parte del stack Contrasta: parar todo ese stack tambien parara el proxy de los otros sitios. Antes de desplegar una segunda aplicacion independiente, separar el ciclo de vida del proxy como servicio compartido y medir memoria de la instancia.

El nombre AWS alejimenez.dev identifica la instancia, no configura el dominio por si mismo. Los nombres internos de Compose, servicio systemd y volumen H2 siguen siendo crosscheck-beta y no dependen del nombre de la instancia.

Cambio de dominio del 19/10/2026: usar DOMAIN=www.contrastaloquetecuentan.com tambien en el .env privado del servidor si ya existe. El ejemplo de configuracion no sobrescribe ese archivo. Caddy usa DOMAIN para seleccionar el sitio y emitir su certificado; el nombre de la instancia AWS alejimenez.dev se conserva. DNS y HTTPS del dominio definitivo siguen pendientes de comprobacion.

## DNS confirmado (20/10/2026)

El usuario confirma el registro del dominio y el registro A www. Verificacion publica mediante el resolvedor 1.1.1.1: www.contrastaloquetecuentan.com resuelve a 15.216.113.27. No se obtuvo registro AAAA en esa comprobacion. HTTPS y preparacion remota del servidor siguen pendientes.

## Preparacion remota realizada (20/10/2026)

Acceso SSH verificado como ubuntu a 15.216.113.27. Clave local en Deployment/LightsailDefaultKey-eu-south-2.pem, excluida de Git. OpenSSH rechazo inicialmente sus permisos; se restringio la ACL al usuario Windows actual sin cambiar el contenido de la clave. La identidad del servidor quedo registrada en Deployment/known_hosts, tambien excluido de Git.

Servidor confirmado: Ubuntu 24.04.4 LTS, unos 909 MiB de RAM y 35 GB de disco disponibles. Instalados Podman 4.9.3 y podman-compose 1.0.6 desde Ubuntu. Unidades systemd instaladas y verificadas mediante systemd-analyze verify. podman-restart.service habilitado; la aplicacion aun no se ha iniciado. El instalador informa de un kernel nuevo pendiente de reinicio.

Los scripts transferidos para la instalacion estan en /home/ubuntu/crosscheck-setup. El log remoto de instalacion es install.log en esa carpeta. No contiene claves del proveedor.

Compatibilidad del servidor: podman-compose 1.0.6 del repositorio Ubuntu fallo al iniciar la cadena de dependencias API -> web -> Caddy. El instalador se actualiza para usar podman-compose 1.6.0 fijado en un entorno virtual dedicado /opt/crosscheck-tools. La unidad systemd y las copias seleccionan ese proveedor explicitamente. La salida de Compose se omite del journal para que los proveedores que imprimen argumentos no expongan variables privadas; consultar salud y logs de los contenedores para diagnostico.

## Estado de despliegue (20/10/2026)

Release beta-20261020-2 compilado localmente con Maven verify correcto y frontend correcto; imagenes transferidas y cargadas en Lightsail. Archivos operativos en /opt/crosscheck; paquete en /home/ubuntu/crosscheck-releases/beta-20261020-2. Las correcciones operativas del primer arranque se incorporaron al paquete y a sus huellas.

Aplicacion arrancada en replay, con PLUS_ULTRA_V20. API y web saludables; Caddy en ejecucion. Credenciales aleatorias del usuario beta y secreto de conversacion generados directamente en el servidor, sin imprimirlos. Copia local de acceso en Deployment/beta-access.txt, excluida de Git y con ACL restringida al usuario Windows. .env remoto privado; no contiene una clave OpenAI y no se ejecutan inferencias reales.

Correcciones del servidor Ubuntu: proveedor podman-compose 1.6.0 en entorno virtual y plugin golang-github-containernetworking-plugin-dnsname para la red CNI. No se borraron los volumenes del stack durante estas correcciones. La copia selecciona el contenedor mediante labels Podman en vez de ps -q api, que no acepta el proveedor instalado.

Certificado de Lets Encrypt emitido para www.contrastaloquetecuentan.com. HTTPS local en el servidor devuelve 401 sin credenciales, como corresponde. Desde el entorno externo de comprobacion, TCP 443 da timeout; verificar/abrir HTTPS TCP 443 en el firewall IPv4 de Lightsail. HTTP 80 permitio validar el certificado. UFW del servidor esta inactivo. La prueba integral desde el dominio publico sigue pendiente de ese acceso.

Copia consistente H2 ejecutada y timer diario activo. Probada restauracion a un volumen distinto: una API replay aislada arranco saludable con esa copia; despues se eliminaron solo el contenedor y volumen de prueba. No se ha probado aun recuperar un resultado concreto despues de un reinicio de la instancia. Las copias siguen en el mismo servidor; la copia externa cifrada sigue pendiente.

Memoria observada en reposo: API aproximadamente 165 MB, web 3 MB, Caddy 13 MB. No constituye prueba de concurrencia. Kernel nuevo instalado, reinicio controlado y comprobacion posterior pendientes. Los logs de contenedores se consultan con sudo podman logs; la salida de Compose se oculta en el servicio para evitar registrar variables privadas.

Recuperacion del proxy tras reiniciar Java: Nginx resuelve api en cada nueva ventana DNS de 5 segundos, usando el resolvedor real del contenedor obtenido por start-web.sh. Esto evita conservar la IP antigua que puede cambiar al parar/arrancar Java durante las copias H2. El arranque web del Compose de servidor usa ese script y la plantilla nginx.conf.

## Verificacion publica completada (20/10/2026)

El usuario abrio HTTPS TCP 443 en Lightsail. Verificada la URL publica https://www.contrastaloquetecuentan.com con certificado valido y usuario/password. Web y API sin credenciales devuelven 401. Navegador Edge autenticado: una sola creacion POST, replay PLUS_ULTRA_V20 completado con SUPPORTED_BY_PUBLICATIONS, resultado visible y recuperado tras recargar. /api/docs devuelve 404.

Verificada persistencia con una consulta adicional: el resultado COMPLETED se recupera con la misma X-Job-Credential tras recrear todo el stack. Probada tambien una copia consistente seguida de reinicio completo de la instancia: mismo trabajo y mismo resultado recuperados tras arrancar automaticamente. Kernel activo comprobado: 7.0.0-1014-aws. No se ejecutaron inferencias reales de OpenAI.

Se corrigio un 502 detectado durante la prueba: Java cambio de IP interna al parar/arrancar para una copia y Nginx conservaba la direccion antigua. El proxy de servidor ahora usa DNS dinamico con validez de 5 segundos, tomando el resolvedor real de /etc/resolv.conf mediante start-web.sh. Plantilla validada con nginx -t dentro del contenedor. El script usa una plantilla mktemp compatible con Alpine.

El paquete archivado en /home/ubuntu/crosscheck-releases/beta-20261020-2 incluye las correcciones operativas y sus huellas SHA256 se verificaron en el servidor. El servicio y el timer de copias estan activos. Credenciales de acceso en Deployment/beta-access.txt (local, privado y excluido de Git). Perfil remoto actual: replay. Quedan pendientes activar OpenAI, verificar su funcionamiento real y preparar una copia externa cifrada con politica de retencion. La prueba funcional no equivale a una prueba de carga.

## OpenAI activado y contraseña sincronizada (20/10/2026)

Estado actual: release `beta-20261020-3`, perfil `openai`, revisión `political-v21`. API y web con imágenes nuevas, tres contenedores saludables, HTTPS operativo y Swagger deshabilitado. Se transfirieron de forma privada las credenciales OpenAI de la configuración local y se conservó el secreto de las conversaciones. Las consultas nuevas consumen la API real. Abrir una conversación nueva al pasar de replay a OpenAI.

Se aplicó en Caddy la contraseña editada por el usuario en `Deployment/beta-access.txt`, generando su hash y recreando el proxy. Cambiar ese archivo local por sí solo no actualiza Caddy: hay que regenerar `BETA_PASSWORD_HASH` en el `.env` privado del servidor y recrear el contenedor edge. El archivo privado remoto de acceso también se sincronizó. No incluir contraseñas ni claves en el repositorio ni en comandos visibles.

Comprobaciones: sin credenciales 401; acceso con la contraseña nueva correcto; una sola consulta real de Zapatero completada, visible tras recargar; `/api/docs` devuelve 404. El color de `QUESTIONS` es rojo `#d64545` tanto en la barra como en la leyenda, comprobado en navegador. El resultado fue `INSUFFICIENT_EVIDENCE`, con siete publicaciones: tres de respaldo y cuatro sin posición explícita. Respuesta original conservada para replay en `PLUS_ULTRA_V21_LIGHTSAIL`; consultar `POC/cross-check-service/docs/openai/replay.md`.

Se detectó y corrigió el cierre de la unidad de copia: systemd detenía el monitor conmon del contenedor reiniciado. `crosscheck-backup.service` usa `KillMode=process` para dejar ese monitor activo al terminar el script. La copia consistente se repitió con `Result=success`; la API volvió a estar saludable. Corrección incluida en el paquete archivado y huellas verificadas. Siguen pendientes la copia externa cifrada, la política de retención y pruebas de carga.

## Revisión political-v22 desplegada (20/10/2026)

Release actual `beta-20261020-5`, perfil openai, revisión `political-v22`. Copia H2 previa correcta, paquete SHA-256 verificado, contenedores saludables. Credenciales y secreto conservados. Instrucciones publicadas en OpenAI y empaquetadas en la imagen Java; igualdad con las instrucciones recibidas por la sesión real verificada.

Una prueba real de Zapatero completó con `SUPPORTED_BY_PUBLICATIONS` y factual `INSUFFICIENT_EVIDENCE`; acceso y recarga correctos. Captura exacta `PLUS_ULTRA_V22_LIGHTSAIL`. La mejora editorial es parcial: persisten restricciones al cobro personal y exclusiones indebidas de piezas descriptivas. Ver `POC/cross-check-service/docs/openai/political-v22-replay-review.md`. Abrir una conversación nueva para usar esta revisión.

## Aviso de prototipo retirado (21/10/2026)

Release actual `beta-20261021-1`. Imagen web nueva sin el aviso de prototipo/simulaciones. API reutilizada exactamente desde `beta-20261020-5`, etiquetada también con el tag nuevo para mantener el Compose existente. Perfil `openai` y revisión `political-v22` conservados, al igual que credenciales y datos.

Paquete web y SHA-256 archivados en `/home/ubuntu/crosscheck-releases/beta-20261021-1`; para reconstruir este release se requiere la imagen API del release anterior. Copia H2 previa correcta. Stack activo y contenedores saludables. Navegador autenticado comprobado en escritorio y móvil con un informe guardado en un contexto aislado: aviso ausente, etiquetas Beta de cabecera y pie conservadas. Cero solicitudes a los endpoints de análisis durante la verificación; sin consumo OpenAI. Acceso sin credenciales sigue devolviendo 401.


## Acceso público (2026-11-03)
Se elimina basic_auth del sitio de Caddy y las variables BETA_USERNAME y BETA_PASSWORD_HASH de Compose. Las referencias anteriores a acceso con contraseña describen el despliegue histórico. El cambio se hará efectivo al actualizar sites/contrasta.caddy y recrear edge en el servidor. Las variables antiguas del .env pueden eliminarse; ya no se utilizan.

## Release beta-20261105-1 desplegada (05/11/2026)

Release completa generada desde el codigo actual (base Git 256aab9 mas los cambios de las paginas informativas), con las imagenes API y web etiquetadas beta-20261105-1. Incluye la categoria Consumo, ocio y viajes, el ajuste de cabecera y los textos acordados de Acerca del proyecto, Como funciona, Privacidad y Ayuda.

Maven verify correcto: 325 pruebas superadas y 2 omitidas. Build frontend correcto. Paquete y SHA256SUMS verificados en /home/ubuntu/crosscheck-releases/beta-20261105-1. Copia H2 previa mediante crosscheck-backup.service con Result=success. Se conservan configuracion, secretos, volumenes y perfil openai con AGENT_REVISION=analysis-v33. Release anterior disponible: beta-20261103-1.

El primer intento activo la recuperacion por comprobar HTTPS antes de que Caddy aceptara conexiones. Version anterior recuperada y segundo despliegue correcto tras incorporar reintentos HTTPS. API y web saludables, servicio activo y verificacion publica externa HTTP 200 tanto en la pagina principal como en /healthz. Bundle servido: /assets/index-yNiQpIUO.js, con los textos actualizados y correo de contacto comprobados. No se han generado consultas de IA durante la comprobacion de produccion.
