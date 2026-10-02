# Primera prueba real — 24/09/2026

Resultado: conexión técnica correcta; validación semántica pendiente de corrección.

Se realizaron dos consultas desde Chromium/Edge contra el frontend Vite, el backend con perfiles `openai,local` y el agente remoto configurado con GPT-5.4 mini. No hubo reintentos ni consultas adicionales de análisis.

| Consulta | HTTP | Tiempo observado |
|---|---|---|
| ¿Es obligatorio votar en las elecciones generales de España? | 200 | 27,741 s |
| ¿Y si no acudo a votar, pueden imponerme una multa? | 200 | 18,313 s |

## Comprobaciones correctas

- El informe supera la validación estructural y se presenta en el frontend.
- El seguimiento envía exactamente el token recibido y conserva el contexto de país y tipo de elecciones sin repetirlos en la pregunta.
- Se presentan fuentes oficiales del Ministerio del Interior y del BOE.
- Se comprobó independientemente que la página de Interior citada establece que nadie puede ser obligado a votar: https://infoelectoral.interior.gob.es/es/proceso-electoral/el-derecho-de-sufragio/.

## Problemas encontrados

1. **Polaridad del veredicto:** ambas respuestas devuelven SUPPORTED aunque niegan la proposición preguntada. La primera explicación incluso dice «La afirmación es falsa». La primera clasificación reformula la proposición en negativo; la segunda conserva «puede dar lugar a una multa», pero marca SUPPORTS en unidades que la contradicen. Se debe fijar una misma proposición y polaridad para contexto, veredicto y posiciones, con ejemplos de preguntas afirmativas y negativas.
2. **Fuentes documentales frente a posiciones:** se cuentan la ley y una página oficial informativa como unidades de posicionamiento. Es necesario precisar la separación entre prueba documental y publicación con postura evaluable, sin forzar una medición editorial cuando solo se han consultado normas e información administrativa.
3. **Trazabilidad temporal:** todas las consultas figuran a las 00:00 UTC, aunque la prueba se ejecutó alrededor de las 17:17 UTC. No se ha acreditado una hora real de consulta; no debe presentarse una hora inventada como metadato exacto.
4. **Consumo:** no se registraron contadores de tokens en el log actual. La consulta de metadatos no permitió recuperar los de esta sesión. El importe queda pendiente de comprobar en Usage de OpenAI; no se estima como si fuera una medición real.

Los dos resultados completos y las capturas de diagnóstico se guardaron en `bootstrap/target/live-openai-results.json` y `bootstrap/target/live-openai-0.png` / `live-openai-1.png`, excluidos de Git. No contienen la clave ni el token de conversación.

No se modificaron las instrucciones remotas ni se repitieron pruebas de pago tras los hallazgos. La fase se detiene para revisión antes de corregir y repetir la validación semántica.

## Corrección y segunda prueba autorizada

Se actualizaron las instrucciones del mismo agente remoto manteniendo GPT-5.4 mini y el esfuerzo bajo. Se verificó su contenido mediante recuperación posterior. La revisión local es `political-v2`, para que no se continúen con ella los tokens de las sesiones previas.

- Se mantiene la polaridad de la proposición en contexto, valoración y posiciones, con ejemplos de preguntas afirmativas y negativas.
- Las normas y la información administrativa no se cuentan como muestra editorial.
- `consultedAt` acepta `null` en fuentes y clasificación cuando no existe metadato temporal fiable. Se actualizaron esquema, dominio y frontend, sin sustituirlo por una hora inventada.

Se enviaron exactamente dos nuevas consultas (la inicial y su seguimiento). La primera devolvió HTTP 200 en 24,565 segundos. Ambas ejecuciones remotas terminaron en `completed` y devolvieron `REFUTED`, clasificación `UNAVAILABLE` con explicación y horas de consulta nulas. El seguimiento mantuvo España y las elecciones generales como contexto.

La automatización del navegador agotó su espera para la segunda respuesta. Se comprobó que la ejecución remota ya había terminado y se recuperaron ambas respuestas guardadas, sin reenviarlas. Por tanto, no se acredita un tiempo HTTP fiable para el seguimiento de esta segunda ronda. Una reproducción local de esas respuestas pasó la validación del frontend y mostró ambos informes; esta reproducción no equivale a una nueva prueba de red extremo a extremo.

Consumo devuelto por los metadatos de los turnos (no importe facturado): primera consulta, 70.724 tokens de entrada (46.080 en caché) y 1.328 de salida; seguimiento, 121.727 de entrada (95.232 en caché) y 1.357 de salida. El coste exacto, incluidas herramientas, debe contrastarse en Usage de OpenAI.

Validación local: 162 pruebas Java sin fallos ni errores y compilación del frontend correcta. Los resultados remotos recuperados están en `bootstrap/target/live-openai-v2-recovered.json`; las capturas de su reproducción están en `bootstrap/target/live-openai-replay-0.png` y `live-openai-replay-1.png`.

Límite: estos dos ejemplos verifican las correcciones observadas, no garantizan coherencia factual para toda consulta política. El timeout del navegador sigue siendo una incidencia a investigar si vuelve a producirse.
