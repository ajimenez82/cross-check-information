# Contrasta lo que te cuentan

Prototipo responsive conectado a Cross Check Service. El modo `dev` devuelve resultados simulados; el perfil `openai` utiliza el agente configurado. El nuevo transporte asíncrono se verifica en esta fase sin consultas de pago. Consulta la [guía de integración](../cross-check-service/docs/phase-5-openai.md) antes de activarla.

## Desarrollo

Requiere Node.js 20.17 o superior y npm.

```sh
npm install
npm run dev
```

## Configuración de la API

La dirección base del backend se configura en el archivo `.env` de esta carpeta:

```dotenv
API_PROXY_TARGET=http://127.0.0.1:8080
```

Puedes editar este valor compartido o sobrescribirlo en `.env.local` para tu equipo; ese archivo está excluido de Git. Reinicia Vite después de cambiarlo.

El navegador crea trabajos con `POST /api/analysis/jobs` y consulta su estado con `GET /api/analysis/jobs/{analysisId}`; Vite redirige ambas operaciones al backend configurado. La variable contiene solo la dirección base, sin la ruta del endpoint. Se aplica al servidor de desarrollo y a preview; el despliegue necesita una redirección equivalente de `/api` en su servidor web. No añadas claves de OpenAI ni secretos de cifrado a la configuración del frontend.

## Compilación

```sh
npm run build
npm run preview
```

React, TypeScript, Vite, React Router, CSS Modules y Lucide. El hosting debe redirigir las rutas de la SPA a `index.html`.

## Alcance

- `/`: inicio con once categorías y descripciones del documento de requisitos; solo Análisis político permite escribir y enviar. Las categorías futuras muestran un aviso y desactivan editor, ejemplos y envío, conservando el borrador al volver. Analizar se desactiva para entradas vacías o compuestas solo por espacios. Las ideas rellenan el campo sin enviar.
- `/resultados/:id`: consulta introducida por el usuario e informes recibidos de la API. `/resultados` abre la conversación más reciente cuando existe.
- Cuenta desplegable con cierre mediante segundo clic, clic fuera y Escape.
- Navegación móvil con diálogo modal, foco contenido y cierre con Escape.
- Perfil, preferencias, información y fuentes mediante diálogos. El tema claro es el único implementado; no hay autenticación.
- Historial local de conversaciones y seguimientos. Cada seguimiento envía el token de la última respuesta; una conversación nueva comienza sin token.
- Estados de cola, preparación, análisis, recuperación y errores, sesión caducada, clasificación no disponible y cero publicaciones. Los porcentajes proceden de las unidades de la respuesta, no representan probabilidad de verdad.
- Fuentes y enlaces recibidos del backend, con validación de la respuesta antes de mostrarla.
- Las consultas, respuestas, tokens de conversación y credenciales de trabajos pendientes se guardan en `localStorage` de este navegador (hasta 50 conversaciones y 100 turnos por conversación). Se pueden eliminar con «Borrar historial local»: se pierde el acceso a los trabajos pendientes, pero no se cancela su ejecución. No se eliminan conversaciones automáticamente al alcanzar el límite. No hay sincronización entre dispositivos; otros usuarios del mismo perfil del navegador pueden acceder a estos datos.
- Recargar recupera los trabajos pendientes por su ID. Si se perdió la respuesta inicial, se repite el POST con la misma clave, credencial y cuerpo guardados. Las respuestas terminadas se muestran desde el historial. Las interrupciones del transporte síncrono antiguo no se reenvían automáticamente.

Los estilos se organizan por funcionalidad y comparten variables de diseño en `src/shared/styles/tokens.css`.

Pie compartido en ambas páginas con año automático, nombre del proyecto, creador Alejandro Jiménez y etiqueta de prototipo.

## Revisión de la conexión simulada

Arranca el servicio con el perfil `dev`, `CONVERSATION_TOKEN_SECRET` configurado y el escenario `CLASSIFIED`. Consulta las [instrucciones del servicio](../cross-check-service/README.md) y la [configuración de IntelliJ](../cross-check-service/docs/phase-4-conversation-tokens.md). En IntelliJ, añade `--crosscheck.development.scenario=CLASSIFIED` a los argumentos del programa. Después ejecuta `npm run dev` en esta carpeta.

1. Envía una consulta: debe aparecer tu texto y un informe identificado como simulado en su título y contenido.
2. Envía un seguimiento y recarga: ambos turnos deben permanecer en el historial.
3. Abre una conversación nueva y vuelve a la anterior desde el historial.
4. Cambia el escenario del servicio y reinícialo para revisar `ZERO_UNITS`, `INSUFFICIENT_EVIDENCE` o `TIMEOUT`.

Cada petición HTTP tiene un límite de 15 segundos; esto no limita la duración total del análisis. El navegador consulta cada 3 segundos (según el servidor), con espera creciente de 3 a 15 segundos ante fallos temporales y respetando `Retry-After`. Pausa las consultas de estado cuando la pestaña está oculta o el navegador está desconectado. A los 90 segundos muestra un aviso informativo y continúa esperando. `VITE_ANALYSIS_TIMEOUT_MS` ya no se utiliza.

La solicitud completa se guarda **antes** de enviarla. Si no hay almacenamiento disponible, no se envía. Se requieren un navegador actual y localhost o HTTPS (Web Crypto y Web Locks). Las pestañas del mismo origen coordinan las escrituras del historial y las solicitudes al mismo trabajo. No hay sincronización entre dispositivos.

Un trabajo fallido o caducado no genera otro automáticamente. «Recuperar este análisis», cuando aparece ante una respuesta inválida, consulta/repite la solicitud con la misma identidad. Iniciar una nueva conversación es una acción distinta que puede crear otra ejecución.

## Comprobaciones automatizadas

Con el frontend en marcha y el backend **dev** en `CLASSIFIED`:

```powershell
$env:APP_URL = 'http://127.0.0.1:5173'
$env:LIVE_API = '1'
# Set only if Playwright is installed outside this project.
$env:PLAYWRIGHT_MODULE = 'C:\path\to\node_modules\playwright'
node scripts/verify-async.cjs
```

`LIVE_API=1` conecta además con el backend configurado en Vite: usar exclusivamente el perfil `dev` para esta revisión sin coste. Sin esa variable, las respuestas se interceptan en el navegador. La batería cubre recarga, respuesta inicial perdida, desconexión, fallo de almacenamiento, errores de acceso, caducidad, fallo terminal, recuperación manual, varias pestañas, plazo HTTP, espera prolongada, aclaraciones y seguimiento. `ASYNC_REVIEW_DIR` permite indicar una carpeta existente para capturas de escritorio/móvil.

`scripts/verify-verdicts.cjs` y `scripts/verify-trace.cjs` conservan la revisión focalizada de etiquetas, iconos, distribución y trazabilidad usando respuestas de trabajos sintéticas. `verify-claims.cjs` y `verify-temporal-error.cjs` se ejecutan desde `OpenAiApiTest` con `-Dbrowser.claims=true -Dbrowser.temporal=true`: adaptan la ruta síncrona de prueba a trabajos terminados para verificar el contrato de los informes; la ejecución asíncrona real se comprueba por separado en `verify-async.cjs` y las pruebas Java de jobs.

`verify.cjs` y `verify-timeouts.cjs` quedan como comprobaciones históricas del transporte síncrono y no son la batería del frontend actual. Usar `verify-async.cjs` para esta versión.
