# Contrasta lo que te cuentan

Prototipo responsive conectado a Cross Check Service. El modo `dev` devuelve resultados simulados; la integración `openai` del backend está preparada y pendiente de credenciales y pruebas reales. Consulta la [guía de integración](../cross-check-service/docs/phase-5-openai.md) antes de activarla.

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

El navegador solicita `/api/analysis/start` y Vite lo redirige al backend configurado. La variable contiene solo la dirección base, sin la ruta del endpoint. Se aplica al servidor de desarrollo y a preview; el despliegue necesita una redirección equivalente de `/api` en su servidor web. No añadas claves de OpenAI ni secretos de cifrado a la configuración del frontend.

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
- Estados de carga, errores con reintento manual, sesión caducada, clasificación no disponible y cero publicaciones. Los porcentajes proceden de las unidades de la respuesta, no representan probabilidad de verdad.
- Fuentes y enlaces recibidos del backend, con validación de la respuesta antes de mostrarla.
- Las consultas, respuestas y tokens se guardan en `localStorage` de este navegador (hasta 50 conversaciones y 100 turnos por conversación). Se pueden eliminar con «Borrar historial local». No hay sincronización entre dispositivos; otros usuarios del mismo perfil del navegador pueden acceder a estos datos.
- Recargar recupera el historial sin repetir peticiones. Una petición interrumpida requiere una acción explícita del usuario para reintentarse.

Los estilos se organizan por funcionalidad y comparten variables de diseño en `src/shared/styles/tokens.css`.

Pie compartido en ambas páginas con año automático, nombre del proyecto, creador Alejandro Jiménez y etiqueta de prototipo.

## Revisión de la conexión simulada

Arranca el servicio con el perfil `dev`, `CONVERSATION_TOKEN_SECRET` configurado y el escenario `CLASSIFIED`. Consulta las [instrucciones del servicio](../cross-check-service/README.md) y la [configuración de IntelliJ](../cross-check-service/docs/phase-4-conversation-tokens.md). En IntelliJ, añade `--crosscheck.development.scenario=CLASSIFIED` a los argumentos del programa. Después ejecuta `npm run dev` en esta carpeta.

1. Envía una consulta: debe aparecer tu texto y un informe identificado como simulado en su título y contenido.
2. Envía un seguimiento y recarga: ambos turnos deben permanecer en el historial.
3. Abre una conversación nueva y vuelve a la anterior desde el historial.
4. Cambia el escenario del servicio y reinícialo para revisar `ZERO_UNITS`, `INSUFFICIENT_EVIDENCE` o `TIMEOUT`.

El tiempo de espera del navegador es de 120 segundos; se puede ajustar con `VITE_ANALYSIS_TIMEOUT_MS` en `.env.local` y reiniciando Vite. No hay reintentos automáticos: tras un tiempo de espera, el servidor podría seguir procesando la petición.

La comprobación automatizada está en `scripts/verify.cjs`. Requiere Playwright disponible y Microsoft Edge instalado. Desde PowerShell, con el frontend y el backend `CLASSIFIED` arrancados:

```powershell
$env:APP_URL = 'http://127.0.0.1:5173'
$env:LIVE_API = '1'
# Set this only when Playwright is installed outside this project.
$env:PLAYWRIGHT_MODULE = 'C:\path\to\node_modules\playwright'
node scripts/verify.cjs
```

Comprueba contrato HTTP, cargas, seguimiento, recarga, historial, errores, reintentos explícitos, respuestas inválidas, enlaces inseguros y tamaños de pantalla. Con `LIVE_API=1` también comprueba navegador → proxy de Vite → Spring Boot, incluido el token cifrado de seguimiento. Las capturas se guardan en `review/integration/`.
