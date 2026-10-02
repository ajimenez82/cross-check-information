# Diagnóstico local de timeouts

## Cambios

Java registra recepción y finalización de `/api/analysis/start`, estado HTTP y duración con el mismo `requestId` enviado al navegador. El adaptador registra inicio, envío de entrada, finalización remota, validación del informe y finalización o tipo de fallo. El contexto MDC se restaura al terminar la petición. No se registran consultas, cuerpos, claves, tokens ni identificadores de sesiones remotas.

La finalización del filtro significa que el servidor terminó de procesar la petición; no garantiza que el navegador haya recibido todo el cuerpo. Para distinguir ambos puntos se correlacionan cabeceras y errores de red del navegador.

Se corrigió un fallo reproducible: al agotarse el plazo durante la lectura del cuerpo, `response.json()` ocultaba la cancelación y la convertía en respuesta inválida. Ahora se conserva como `CLIENT_TIMEOUT`, incluido `X-Request-Id` cuando las cabeceras ya llegaron.

## Pruebas sin OpenAI

`POC/webapp/scripts/verify-timeouts.cjs` levanta un servidor HTTP local en 8098. Vite lo recibe como destino del proxy. No lee credenciales ni llama al proveedor real.

Desde `POC/webapp`, en una terminal:

```powershell
$env:API_PROXY_TARGET = 'http://127.0.0.1:8098'
$env:VITE_ANALYSIS_TIMEOUT_MS = '1500'
npm run dev -- --port 5179 --strictPort
```

En otra terminal con Playwright disponible (configurar `PLAYWRIGHT_MODULE` si está instalado fuera del proyecto):

```powershell
node scripts/verify-timeouts.cjs
```

Los valores reducidos son exclusivos de esta prueba; no cambian los límites normales de 90 segundos en Java y 120 en el frontend.

Resultados comprobados:

- Consulta y seguimiento con 700 ms de retraso: respuesta válida.
- Servidor simulado devuelve HTTP 504 después de 700 ms: mensaje de timeout del servicio e identificador visible.
- Servidor espera 2400 ms y navegador cancela a los 1500 ms: timeout del navegador.
- Servidor envía cabeceras y demora el cuerpo: timeout del navegador, sin confundirlo con contrato inválido, e identificador conservado.
- No hay reintentos automáticos ni una valoración factual para errores técnicos.

El diagnóstico se guarda en `POC/webapp/review/integration/timeout-diagnostics.json`. Si falla la prueba, guarda además una captura; no serializa peticiones ni tokens.

## Alcance del resultado

Esta fase reproduce y corrige un error en el tratamiento de cancelaciones. No establece retrospectivamente la causa del timeout observado con OpenAI: en aquella ejecución faltaba trazabilidad para distinguir navegador, proxy y procesamiento Java. No se ha repetido ninguna consulta de pago ni aumentado los tiempos de espera. La siguiente prueba real podrá correlacionarse por `requestId`.

## Prueba real autorizada — 25/09/2026

Se ejecutaron exactamente dos consultas desde el frontend, sin reintentos, con los plazos normales (Java 90 s, navegador 120 s). Ambas respuestas llegaron al navegador, pasaron la validación del contrato y se mostraron en pantalla.

| Consulta | HTTP | Tiempo navegador | Tiempo Java | Valoración |
|---|---|---|---|---|
| ¿Es obligatorio votar en las elecciones generales de España? | 200 | 21,776 s | 21,394 s | REFUTED |
| ¿Y si no acudo a votar, pueden imponerme una multa? | 200 | 15,126 s | 15,033 s | REFUTED |

Identificadores de correlación: `6a6857cf-263a-4c4f-b33e-1e4bf61eaf50` y `d72a87fb-6bea-437f-b98d-e3275e686ba5`. En ambos se registraron recepción, inicio del proveedor, entrada enviada, turno remoto completado, informe validado y finalización HTTP 200. No hubo errores de red registrados. El seguimiento utilizó el token recibido y conservó el contexto de España y las elecciones generales.

Las respuestas mantienen la proposición original, no generan posiciones editoriales a partir de leyes o información administrativa (`UNAVAILABLE`) y dejan las horas de consulta desconocidas en `null`.

Consumo informado por los turnos remotos:

| Turno | Tokens de entrada | Entrada en caché (incluida) | Tokens de salida |
|---|---|---|---|
| Inicial | 46.107 | 28.160 | 1.184 |
| Seguimiento | 63.498 | 51.712 | 1.115 |

El importe facturado, incluidas herramientas, queda pendiente de consultar en Usage; estos son contadores, no una medición monetaria.

Artefactos de diagnóstico excluidos de Git en `bootstrap/target`: `live-traced-results.json`, `live-traced-review.log`, `live-traced-usage.json` y capturas `live-traced-0.png` / `live-traced-1.png`. No incluyen claves ni tokens de conversación. Los servicios locales se detienen al terminar la revisión.

Conclusión: el recorrido completo y el seguimiento pasan esta comprobación. No se reprodujo el timeout anterior; su causa histórica sigue sin estar demostrada. No se aumentaron plazos ni se introdujeron reintentos automáticos.

Observación de contenido pendiente: el agente devolvió `documentarySupport: "REFUTED"` en ambos informes. El veredicto y la explicación son coherentes, pero ese campo debería contener una descripción en español, no repetir el código técnico. La comprobación de conectividad no implica aprobar ese detalle de presentación ni una auditoría exhaustiva de todas las fuentes.


### Ajuste de respaldo documental — 25/09/2026

Se precisaron y actualizaron las instrucciones locales y remotas: documentarySupport debe contener una o dos frases explicativas en español, vinculadas a las evidencias y sus límites; no un código, una etiqueta aislada, puntuación ni porcentaje editorial. Se incluyeron ejemplos y una comprobación de coherencia antes de devolver el informe. Se verificó por lectura remota la igualdad de las instrucciones. La revisión local pasa a political-v3; el modelo permanece sin cambios. No se ejecutaron análisis: la eficacia del ajuste queda pendiente de la siguiente prueba real en una conversación nueva.
