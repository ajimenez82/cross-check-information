# Prueba real de entrega asíncrona — 30/09/2026

Resultado: una consulta real completada y mostrada en el frontend, incluida una recarga
mientras estaba en RUNNING y otra después de completarse. No se ejecutaron seguimientos
reales ni se repitió la inferencia. El usuario autorizó esta fase tras la revisión offline.

## Corrección previa

La preparación detectó un error del diseño anterior: una sesión con environment.type=none
requiere input inicial. El mock local había permitido crearla vacía. Se corrigió el
adaptador para enviar el input en el POST de creación; los seguimientos continúan usando
events. El worker guarda la huella antes de crear la sesión y pasa a polling después de
aceptarla, sin enviar el mismo input otra vez por events.

Si se pierde esa respuesta, se buscan la metadata exacta y el agente, y después el turno
cuyo input coincide. Una sesión recuperada sin turno visible permanece en recuperación;
no se interpreta como autorización para reenviar. Un rechazo confirmado antes de iniciar
falla sin reservar capacidad remota. Los envíos ambiguos mantienen su reserva.

Referencia consultada: [OpenAI, Run and continue sessions](https://developers.openai.com/api/docs/guides/agents-api/sessions).
Se mantiene el agente existente, gpt-5.4-mini, razonamiento low, service tier default,
esquema 3 y revisión political-v9. No se cambian sus instrucciones.

## Ejecución observada

Consulta: «¿Los límites al precio del alquiler reducen la oferta de vivienda en alquiler
en España? Analiza el periodo 2023–2025 y contrasta publicaciones con distintas posiciones.»

- analysisId: 81a2eac9-9776-42e6-be25-a0984fd76982.
- Aceptación: HTTP 202, QUEUED; después RUNNING y COMPLETED mediante HTTP 200.
- Creado a las 12:25:57.470 UTC; completado a las 12:26:57.093 UTC: 59,62 s en el servicio.
- Resultado visible y comprobado a los 64,85 s desde el inicio de la prueba de navegador.
- Navegador: un POST y 16 GET; recarga en RUNNING sobre el mismo analysisId.
- Lectura remota: una sesión con esa metadata y un único turno completed.
- Después de completar, otra recarga muestra el informe persistido.
- Resultado declarado por el agente: INSUFFICIENT_EVIDENCE, una afirmación y cinco fuentes.
- Posicionamiento: UNAVAILABLE; el agente considera que la muestra no permite comparar
  posiciones sobre toda España. No se muestran porcentajes inventados.

La representación y las referencias se revisaron visualmente. Esta ejecución no
constituye una auditoría factual de las fuentes. La síntesis incluye el código técnico
INSUFFICIENT_EVIDENCE dentro del texto generado: se anota para la futura revisión de
instrucciones, sin modificar ni regenerar la respuesta en esta fase.

## Evidencia y límites

[Captura del resultado](../../../webapp/review/integration/async-live-20260930.png).
[Registro y resultado con token redactado](../../../webapp/review/integration/async-live-20260930.json).
Los datos privados de recuperación permanecen únicamente en bootstrap/target, ignorado
por Git. No se han copiado claves API, credenciales de jobs ni tokens al registro publicable.

La prueba real terminó antes de 90 s: no demuestra una ejecución real superior a ese
umbral. La demora prolongada, timeouts, pérdida de respuestas y reinicios de persistencia
siguen cubiertos por pruebas locales. No se provocó una caída del proceso durante esta
inferencia. La lista remota de turnos devolvió usage=null: no se estima un coste ni se
interpreta como consumo cero.

Verificación previa: mvn verify, 284 pruebas registradas, 282 superadas y 2 comprobaciones
optativas de navegador omitidas. Incluye tres casos adicionales para el arranque y los
seguimientos. Se añadieron después dos regresiones focalizadas de recuperación sin turno
y rechazo confirmado de creación; la batería focalizada AnalysisJobsTest terminó con 24/24 casos correctos.

Los procesos temporales se detienen al cerrar la revisión. La base de esta prueba está
separada en bootstrap/target/async-live-20260930; la base habitual y el fichero de secretos
local no se modifican. Siguiente trabajo pendiente: evaluación semántica de fuentes y
valoraciones, con autorización por fase.
