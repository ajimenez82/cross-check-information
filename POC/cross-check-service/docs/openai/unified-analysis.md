# Análisis político unificado

Revisión `political-v17`, 03/10/2026. Esta guía describe el flujo vigente.

## Flujo y responsabilidades

1. El frontend crea un trabajo mediante POST `/api/analysis/jobs` y consulta su estado.
2. Java valida la petición y persiste el intento antes del envío remoto.
3. Una ejecución del agente investiga con búsqueda web y devuelve el informe completo o una aclaración. No hay una segunda sesión de síntesis.
4. Java valida JSON, campos, enums, fechas, referencias internas y coherencia del contrato, mapea el informe y guarda el resultado.
5. El frontend muestra el informe y conserva el seguimiento tras recargar.

La IA es la autoridad del contenido. Java no descarga URLs, lee PDFs, coteja citas ni verifica publicaciones de forma independiente. No filtra fuentes por la accesibilidad de su propia red y no sustituye la explicación del criterio de selección. La aceptación de un informe acredita cumplimiento del contrato, no verdad factual.

Se mantiene el contrato v3 y sus cálculos mecánicos: agrupación de veredictos por afirmación, clasificación de la muestra según las decisiones del agente, porcentajes y ventana de fechas conocidas. Java no realiza una segunda evaluación de las pruebas. Las contradicciones entre campos del JSON siguen siendo errores de contrato. Los sellos de ejecución los proporciona el servidor; el contrato del agente usa null para fechas de consulta no disponibles.

## Configuración

Recursos canónicos:

- `infrastructure/src/main/resources/openai/unified/instructions.md`
- `infrastructure/src/main/resources/openai/unified/response.schema.json`

`./scripts/prepare-unified-agent.ps1` genera `bootstrap/target/political-agent-unified.request.json` sin red ni claves. Modelo gpt-5.4-mini, razonamiento low, servicio default, búsqueda web y multi-agent desactivado. La creación de cada sesión incluye esta configuración para no depender de una versión antigua del agente guardado. [Configuración oficial de Agents](https://developers.openai.com/api/docs/guides/agents-api/configuration).

En `config/application-local.yml`: OPENAI_API_KEY, OPENAI_POLITICAL_ANALYSIS_AGENT_ID y POLITICAL_AGENT_REVISION: political-v17. Perfil real `openai,local`; `dev,local` conserva su proveedor simulado y no cambia en esta entrega. La antigua variable de activación de captura se retira. Reiniciar el servicio y comenzar una conversación nueva tras cambiar revisión.

## Trabajos anteriores

Los resultados completados siguen consultables hasta su retención normal. Los trabajos pendientes cuya identidad de proveedor/revisión no coincide pasan a ANALYSIS_REVISION_UNAVAILABLE y no se reenvían. Si había un envío remoto incierto, se conserva la reserva mientras se reconcilia su estado; no se afirma que se haya cancelado. Los antiguos pasos locales o anteriores a la segunda ejecución se cierran sin enviar inferencia.

Se mantienen campos antiguos de persistencia únicamente para leer esos registros y retirarlos con seguridad. No existe ejecución nueva por etapas ni adquisición documental. No se ha borrado la base habitual `data/`; los artefactos diagnósticos de target se regeneran con las compilaciones limpias.

## Limpieza y validación

Retirados: expediente de evidencia, capturador HTTP/HTML/PDF, adaptador de investigación/captura/síntesis, recursos de ambas etapas, sus librerías exclusivas, pruebas, propuestas, revisiones y capturas específicas. Se conservan requisitos generales, contratos de API, conversaciones, seguridad y frontend.

Las pruebas del flujo unificado cubren una creación con informe completo, aclaraciones, referencias, fuentes conservadas sin descarga, seguimiento sin reenvíos y cierre/reconciliación de revisiones anteriores, incluido un envío antiguo con dos sesiones y respuesta de creación perdida. La suite general y la regresión de navegador se ejecutan antes de la prueba real.

## Resultados históricos de political-v16

- Compilación limpia y pruebas Maven correctas; suite final: 290 pruebas registradas, 288 superadas y dos opcionales omitidas.
- Dieciséis escenarios offline del navegador superados. Verificados contratos JSON y ejemplos del informe y aclaración.
- Configuración unificada publicada en el agente guardado y comprobada por lectura; nombre y modelo conservados. Las instrucciones de la sesión real coinciden con el recurso local.
- Una consulta real desde el frontend: «Zapatero ganó una comisión por su asesoramiento en caso de Plus Ultra». COMPLETED en 51,72 segundos observados desde el navegador, un POST y trece GET; recarga durante ejecución y persistencia después de completarla.
- Una única sesión y un único turno remoto. El agente devolvió INSUFFICIENT_EVIDENCE y dos fuentes (EL PAÍS y un documento alojado en Primicias). Java conservó ambas sin acceder a sus URLs. Cero unidades contables según las decisiones del agente, con «No calculable» en móvil y escritorio.
- Uso: 145.032 tokens de entrada, de ellos 112.640 en caché; 3.416 de salida; total 148.448. Una sola inferencia, sin reintento. No se estimó importe facturado.

La prueba confirma el transporte y procesamiento, no una verificación independiente de las afirmaciones del informe. Las decisiones editoriales proceden del agente; por ejemplo, su explicación sobre ausencia de postura propia se muestra tal como la devolvió. La calidad de esas interpretaciones sigue siendo responsabilidad de las instrucciones y del modelo, sin un filtro documental adicional en Java.

[Respuesta redactada y métricas](../../../webapp/review/integration/unified-live-result.json), [escritorio](../../../webapp/review/integration/unified-desktop.png), [móvil](../../../webapp/review/integration/unified-mobile.png). Los tokens de acceso y secretos no se incluyen en estos artefactos. Servidores diagnósticos detenidos al terminar; para revisar el servicio hay que arrancar el JAR actualizado o reiniciar IntelliJ.

## political-v17: investigación periodística central

La valoración de publicaciones es una tarea central y obligatoria para una afirmación individual, aunque no exista resolución oficial ni prueba factual concluyente. El objetivo de cobertura para controversias ampliamente tratadas es 8–12 publicaciones de al menos cinco medios, buscando prensa, medios digitales, televisión, investigación, análisis y opinión. Es un objetivo de investigación, no una cuota: cualquier limitación real se declara y no se inventan lecturas ni se busca hasta forzar un ganador.

Se distingue una postura adoptada por el autor de una acusación meramente reproducida. Una noticia pertinente sin postura propia cuenta como NO_EXPLICIT_POSITION; no se excluye por ser informativa. Se mantienen las reproducciones deduplicadas y la atribución de opiniones a sus autores.

Para SINGLE con valoración factual insuficiente, el predominio único de SUPPORTS o QUESTIONS produce el único veredicto final SUPPORTED_BY_PUBLICATIONS o QUESTIONED_BY_PUBLICATIONS. Empate, predominio mixto o sin posición, o ausencia de muestra evaluable conservan INSUFFICIENT_EVIDENCE. No cambian el contrato v3, la política Java ni la limitación actual de MULTIPLE.

Las métricas y capturas anteriores corresponden a v16: no acreditan la calidad del nuevo comportamiento. La ampliación de investigación puede aumentar tiempo y consumo; requiere una nueva evaluación real para comprobar cobertura y clasificación.

Validación v17 (03/10/2026): Maven verify correcto, incluidas las pruebas existentes de predominio, empate y conservación de los demás veredictos. Configuración publicada en el agente guardado y verificada por lectura, conservando nombre y modelo. No se ha ejecutado una nueva inferencia real en esta revisión. Reiniciar el servicio y abrir una conversación nueva para usar political-v17.
