# Political-v13 — explicación obligatoria de publicaciones

01/10/2026. El esquema interno de síntesis exige `publicationPositions.reason` como texto no vacío y con algún carácter distinto de espacio. No admite null ni omitir la propiedad. Las instrucciones piden explicar el resultado de la muestra, especialmente cuando todas las candidatas se excluyen. La validación Java exige lo mismo; no genera ni sustituye explicaciones del modelo.

El contrato público v3 conserva sus reglas. La investigación y el agente guardado no cambian. La síntesis usa las instrucciones y el esquema actualizados por sesión. Copias: [instrucciones](versions/political-v13.synthesis.md), [esquema](versions/political-v13.synthesis.schema.json).

La revisión predeterminada, el ejemplo y la configuración local pasan a political-v13. Reiniciar el servicio y utilizar una conversación nueva para aplicar la revisión.

Validación: `mvn verify`, 339 pruebas registradas, 337 superadas y dos opcionales omitidas. Pruebas de esquema rechazan propiedad ausente, null, cadena vacía y solo espacios. Las pruebas Java rechazan explicaciones vacías incluso si hay publicaciones contables y mantienen el caso válido de todas las publicaciones excluidas con evidencia citable.

## Prueba real posterior — 01/10/2026

Una única síntesis real, reutilizando el expediente capturado, terminó COMPLETED y superó el ensamblado y la validación del backend. No se repitieron investigación ni capturas. Se usó el worker real en una base diagnóstica aislada; no es una ejecución completa nueva desde el frontend. El proceso cerró el servidor al terminar. La auditoría confirmó una sesión, un turno y las instrucciones v13.

Resultado: INSUFFICIENT_EVIDENCE, una fuente documental (Banco de España), cero publicaciones contables y S1 excluida como contexto. `publicationPositions.reason` contiene la explicación requerida. Se conservaron la fecha de publicación 16/10/2024 y la fecha de captura guardada; no se sustituyó esta última por la fecha de síntesis. Las otras tres fuentes permanecen como limitaciones de acceso.

Duración del trabajo diagnóstico: aproximadamente 15,4 segundos. Uso: 9.952 tokens de entrada, sin caché; 1.031 de salida; total 10.983. No se ejecutó otra inferencia. Respuesta redactada, auditoría y base privada en `bootstrap/target/v13-resume-*` y `bootstrap/target/political-v13-resume`.

La validación funcional de esta síntesis pasa; no acredita una investigación completa ni la calidad semántica general. Persisten aspectos editoriales: aparecen códigos internos como BACKGROUND y la explicación mezcla la exclusión por contexto con falta de causalidad. La causalidad no es requisito general para contar una postura editorial. También se arrastran limitaciones de investigación sobre documentos no capturados que no deben confundirse con evidencia verificada. El fallo real de v12 se conserva sin cambios.

Reproducción posterior en frontend a 1536 y 390 píxeles: correcta, sin porcentajes para la muestra vacía, con ficha de fuente y persistencia al recargar. Todas las peticiones interceptadas, sin consumo de API. [Revisión visual y hallazgos editoriales](../../../webapp/review/integration/political-v13-ui-review.md).
