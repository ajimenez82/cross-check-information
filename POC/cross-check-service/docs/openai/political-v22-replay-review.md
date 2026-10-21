# political-v22: publicación, despliegue y prueba real

20/10/2026. Instrucciones publicadas y verificadas por lectura; modelo gpt-5.4-mini, nombre y contrato conservados. Release remoto `beta-20261020-5`. La sesión real recibió exactamente las instrucciones preparadas, comprobado por igualdad del texto. Se conserva la clave de conversaciones, la contraseña y el resto de configuración privada. Copia previa H2 correcta.

## Verificación técnica

Construcción de imagen con Maven verify: 307 pruebas, 305 superadas y dos omitidas; cero fallos. Una expectativa del token seguía en v21 y se actualizó a v22 antes de reconstruir. HTTPS, acceso autenticado, rechazo 401 sin credenciales, una sola creación POST, finalización y recarga correctos. Swagger sigue deshabilitado. Tres contenedores saludables.

Consulta exacta: Zapatero ganó una comisión por su asesoramiento en caso de Plus Ultra. Veredicto de la afirmación: `INSUFFICIENT_EVIDENCE`. Resultado final calculado: `SUPPORTED_BY_PUBLICATIONS`, con tres unidades SUPPORTS y una QUESTIONS. Nueve fuentes listadas; cuatro unidades de posición.

Respuesta cruda guardada sin modificaciones en `infrastructure/src/main/resources/openai/replay/PLUS_ULTRA_V22_LIGHTSAIL/response.json`. Recuperada por metadatos del trabajo con llamadas de lectura: no se generó una segunda inferencia para capturarla. Metadata y SHA-256 en el mismo directorio.

## Evaluación editorial: mejora parcial

- La falta de prueba concluyente se clasifica ahora como INSUFFICIENT_EVIDENCE y no MISLEADING. Este criterio se cumple en esta muestra.
- El contexto menciona beneficios indirectos, pero vuelve a restringir la explicación a recibir personalmente la comisión. El problema de alcance persiste parcialmente.
- Maldita ya no cuenta como QUESTIONS por reproducir una negativa, pero se excluye por falta de postura propia; debería contar como NO_EXPLICIT_POSITION si cumple acceso, alcance e independencia. Ocurre lo mismo con EFE según la justificación escrita.
- La atribución AUTHOR de la unidad asociada a Europa Press parece apoyarse en declaraciones de terceros. Requiere comprobar el argumento adoptado, sin asumir que narrarlo equivale a respaldarlo.
- S2 y S9 no figuran como unidades ni en excluded. La selección menciona La Vanguardia/Reuters, que no aparece en la lista final. La cobertura y su trazabilidad necesitan una revisión adicional.

No se han corregido manualmente posiciones ni fuentes, ni se ha forzado el veredicto. Esta ejecución acredita el despliegue y una mejora concreta; no acredita que los tres problemas editoriales estén resueltos. Las fuentes no se han verificado de forma independiente en esta evaluación. Los pasos siguientes deben centrarse en alcance coherente y contabilidad completa de publicaciones antes de otra prueba real.

Replay local verificado: `PLUS_ULTRA_V22_LIGHTSAIL` completa el procesamiento Java, muestra SUPPORTED_BY_PUBLICATIONS y supera recarga, orden móvil y comprobaciones de color. Capturas y resultado en `POC/webapp/review/integration/verdict-first/PLUS_ULTRA_V22_LIGHTSAIL*`. No hubo consumo OpenAI en esa reproducción.
