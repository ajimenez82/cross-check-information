# Political-v20: textos de resultado y prueba real

Fecha de revision: 13/10/2026.

## Publicacion

Instrucciones publicadas y verificadas por lectura en el agente configurado. Modelo gpt-5.4-mini y nombre conservados. Revision local y valor por defecto actualizados a political-v20. Contrato JSON v3 sin cambios.

## Captura original

Escenario: PLUS_ULTRA_V20. Consulta: «Zapatero ganó una comisión por su asesoramiento en caso de Plus Ultra».

response.json conserva el texto original; metadata.json incluye huellas de respuesta e instrucciones. Se envio una sola consulta real. El campo usage remoto vino nulo: no se dispone de una cifra de consumo en esta captura.

La respuesta cita nueve fuentes de seis medios: EL PAIS, RTVE, La Vanguardia, La Voz de Galicia, laSexta y El Espanol. Clasifica ocho unidades contables: seis SUPPORTS y dos NO_EXPLICIT_POSITION. La valoracion documental es INSUFFICIENT_EVIDENCE; la politica Java deriva SUPPORTED_BY_PUBLICATIONS.

## Revision editorial

La explicacion tiene dos frases y mantiene la limitacion probatoria; Java ya no antepone otra frase generica de insuficiencia. El titulo, contexto y detalle documental cumplen funciones identificables.

No se considera perfecta la calidad editorial: la sintesis sigue repitiendo la conclusion documental en su ultima frase. Ademas, el contexto incluye intermediarios y sociedades, pero la explicacion se concentra en el cobro personal; merece revisar la coherencia del alcance. Estos son hallazgos de evaluacion, no correcciones aplicadas al contenido capturado. No se han verificado independientemente las alegaciones de las fuentes.

## Reproduccion

En IntelliJ: perfiles replay,local y argumento --crosscheck.replay.scenario=PLUS_ULTRA_V20. Reiniciar Java con el codigo actualizado y abrir una conversacion nueva. Pegar la consulta exacta anterior. La reproduccion no llama a OpenAI.

## Validacion tecnica

Maven verify: 306 pruebas, 304 superadas y 2 omitidas, sin fallos ni errores. El nuevo replay completa el procesamiento Java y el navegador con SUPPORTED_BY_PUBLICATIONS. Verificados recarga, fuentes, desplegables y orden movil. Capturas y resultado redactado en POC/webapp/review/integration/verdict-first/PLUS_ULTRA_V20*. Los servidores de prueba se cerraron al finalizar.
