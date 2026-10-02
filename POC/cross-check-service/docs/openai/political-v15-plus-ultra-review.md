# Political-v15 — prueba completa sobre Plus Ultra

01/10/2026. Consulta solicitada por el usuario: «Zapatero ganó una comisión por su asesoramiento en caso de Plus Ultra». Se procesó como afirmación que debía contrastarse, sin asumirla cierta.

## Resultado funcional

COMPLETED / INSUFFICIENT_EVIDENCE. La investigación, captura independiente, síntesis, validación y presentación completaron el recorrido con una sola petición de creación. Se recargó durante RUNNING y después de completar; se recuperó el mismo trabajo y persistió el resultado. Revisado a 1536 y 390 píxeles, sin desbordamiento horizontal. Sin publicaciones contables se muestra «No calculable», sin porcentajes.

Tiempo observado desde el navegador: 81 segundos; ejecución local del trabajo: aproximadamente 74 segundos. Un POST y 20 GET de seguimiento. Una sesión/turno de investigación y una sesión/turno de síntesis, con instrucciones verificadas por lectura. No se repitieron inferencias.

Se propusieron tres fuentes. Solo se capturó evidencia del documento parlamentario: un fragmento aceptado. EFE y EL PAÍS no pudieron capturarse y quedaron fuera de la evidencia. El documento no editorial se excluyó de los porcentajes; cero publicaciones contables. La revisión valida el funcionamiento del flujo, no una investigación exhaustiva ni la verdad o falsedad de la afirmación.

## Revisión de evidencia y problemas encontrados

Se revisó el [diario de sesiones original](https://www.congreso.es/public_oficiales/L15/SEN/DS/CO/DS_C_15_654.PDF). Es del Senado, de 9 de abril de 2026. En la página 3, una pregunta atribuye una supuesta comisión a la empresa del compareciente y pregunta por la intervención de Zapatero. La transcripción acredita que se formuló esa pregunta; no confirma por sí sola un cobro de Zapatero. El informe mantiene la distinción entre alegación y cobro acreditado.

Hallazgos pendientes, sin corregir artificialmente el resultado guardado:

1. Publisher incorrecto: «Congreso de los Diputados» por la web de alojamiento, cuando el documento identifica al Senado. La captura literal no verifica automáticamente este metadato de investigación.
2. Localizador incorrecto: el modelo indicó página 2; el pasaje está en la página 3. La coincidencia textual no valida el número de página declarado.
3. La fecha visible del PDF no se extrajo: `publishedAt` quedó null. No se inventó una fecha, pero el adaptador tiene una limitación de lectura de fechas documentales.
4. El hallazgo utiliza DESCRIPTIVE/QUESTIONS aunque recoge una alegación en una pregunta. Conviene revisar atribución y relación semántica; no produjo un voto porque la fuente no es editorial.
5. El investigador marcó BACKGROUND y un periodo histórico aunque el usuario no fijó un corte temporal. Aquí no cambia la exclusión, ya justificada por el tipo de documento; su aplicación en otras consultas requiere revisión.
6. Las limitaciones de acceso se repiten con distinta redacción: una versión viene de Java y otra de la síntesis. El título documental es demasiado largo para una ficha cómoda.

La frase «evidencia insuficiente» describe la evidencia aceptada por esta ejecución; no debe interpretarse como una conclusión exhaustiva sobre todo el caso. La pérdida de dos fuentes limita la muestra.

## Consumo y artefactos

Investigación: 173.495 tokens de entrada (141.824 en caché), 3.444 de salida; total 176.939. Síntesis: 10.214 de entrada (sin caché), 723 de salida; total 10.937. Total registrado: 187.876 tokens. No se calcula importe facturado.

[Respuesta y métricas redactadas](../../../webapp/review/integration/political-v15-plus-ultra.json), [escritorio](../../../webapp/review/integration/political-v15-plus-ultra-1536.png), [móvil](../../../webapp/review/integration/political-v15-plus-ultra-390.png). Credenciales de trabajo, auditoría remota y base H2 quedan en target, excluido de Git. Se usaron servidores de prueba y base aislados.

Prioridad siguiente: verificar identidad documental y localizadores, y mejorar la recuperación de fuentes accesibles. La aceptación estructural no debe confundirse con una auditoría completa de contenido.
