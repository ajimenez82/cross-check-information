# Regresión: MISLEADING por falta de prueba

Respuesta del servicio aportada por el usuario el 20/10/2026. Se conserva el contenido del análisis sin corregir; se ha eliminado el token de conversación y se ha reformateado el JSON. No es una captura cruda de OpenAI ni un escenario replay: no debe reconstruirse artificialmente una respuesta del proveedor a partir de ella.

`expectations.json` contiene los criterios para evaluar futuras ejecuciones. El veredicto factual esperado corresponde al razonamiento escrito en esta respuesta, no a una verificación independiente de sus fuentes. P2 requiere revisar la postura adoptada en la publicación; la negativa citada no basta para QUESTIONS. No se fija un ganador editorial.

Las instrucciones incorporan ejemplos contrastivos generales: incertidumbre sobre pagos mediante terceros frente a una distorsión demostrada del denominador. La comprobación local verifica preparación, contrato y conservación del caso; no demuestra por sí sola que el modelo aplique las reglas. Pendientes publicación, despliegue y evaluación real con las instrucciones nuevas.

Preparación local: el JSON generado contiene exactamente el archivo actualizado de instrucciones. Caso comparado con el adjunto: igualdad del contenido tras retirar únicamente conversationToken. La revisión publicada sigue siendo political-v21; no se cambia su identificador hasta publicar y desplegar la nueva versión. El proveedor asíncrono envía la configuración empaquetada en cada sesión nueva, por lo que actualizar solo la consola OpenAI no actualiza el servicio desplegado.

Referencia metodológica: [evaluaciones de OpenAI](https://developers.openai.com/api/docs/guides/evals), con casos y criterios de comportamiento esperado.

Actualización posterior: publicada y desplegada political-v22. La prueba real evita MISLEADING en este caso, pero conserva problemas de alcance y contabilización de publicaciones. Consultar [informe v22](../../political-v22-replay-review.md). Las notas de pendiente anteriores describen el estado previo a esta publicación.
