# Pruebas reales political-v19 y replay

Capturas reales: 03/10/2026. Verificación local: 05/10/2026.

Cada caso ejecutó un turno real del agente; se comprobó por lectura que las instrucciones coinciden con la huella guardada. La respuesta original se conserva sin corregir antes de pasar por Java. La prueba de navegador posterior utiliza solo replay.

| Escenario | Resultado final Java/FE | Fuentes devueltas | Tiempo de captura aproximado | Tokens totales |
|---|---|---:|---:|---:|
| ACLARACION_V19 | ACLARACIÓN | 0 | 19.3 s | 18704 |
| ALQUILERES_V19 | SUPPORTED_BY_PUBLICATIONS | 6 | 268.3 s | 839321 |
| PLUS_ULTRA_V19 | SUPPORTED | 8 | 86.2 s | 294205 |
| VOTO_OBLIGATORIO_V19 | REFUTED | 5 | 188.0 s | 696112 |

## Verificación local

- Maven verify: 304 pruebas registradas, 302 superadas y 2 opcionales omitidas.
- Cuatro escenarios COMPLETED desde el navegador contra el servicio Java real en perfil replay, con una creación por consulta, persistencia tras recarga y sin desbordamiento horizontal en móvil.
- Sin cliente HTTP de OpenAI ni claves requeridas en replay; perfiles incompatibles, escenarios desconocidos, consulta diferente y continuación no grabada rechazados.
- La revisión de conversación incorpora escenario y huella del contenido.
- JSON original, metadatos y resultados finales se guardan separados. Las respuestas finales del navegador tienen el token de conversación redactado. No se guardan claves ni identificadores remotos en las capturas públicas.

## Lectura de los resultados del agente

- Plus Ultra: el agente devolvió respaldo factual; el caso reproduce la interpretación directa o mediante terceros. No constituye una comprobación independiente del caso.
- Voto: el agente devolvió REFUTED y justificó una búsqueda más corta por tratarse de una cuestión normativa definida.
- Alquileres: el agente devolvió INSUFFICIENT_EVIDENCE con tres SUPPORTS, dos QUESTIONS y un MIXED; Java derivó SUPPORTED_BY_PUBLICATIONS (50 % de respaldo, predominio único). Este caso comprueba la distinción entre la valoración factual de la afirmación y el veredicto final.
- Aclaración: el agente solicitó situación, lugar o evento antes de emitir un informe.
- Los tres informes tienen asOf null: no convierten la última fecha de fuente en corte temporal.

## Aspectos todavía pendientes de calidad

Las capturas no garantizan cumplimiento completo del prompt ni una muestra representativa. Alquileres devuelve seis fuentes, por debajo del objetivo 8–12. Además, menciona un estudio de 2016–2022 frente al periodo solicitado 2024–2025, y declara noticias sin postura clara aunque todas sus unidades se clasifican direccionales o mixtas. Esa coherencia de alcance y atribución requiere revisión antes de dar por validado el análisis. El contenido original se conserva deliberadamente para poder reproducir estos puntos.

## Revisar localmente

[Guía de perfiles y consultas exactas](replay.md). Resultados finales y capturas de escritorio/móvil: `POC/webapp/review/integration/replay/`. Respuestas de OpenAI: `infrastructure/src/main/resources/openai/replay/<ESCENARIO>/response.json`.

Los procesos de verificación son temporales; el script los cierra al terminar. No se ha cambiado el prompt del agente tras observar estos resultados.
