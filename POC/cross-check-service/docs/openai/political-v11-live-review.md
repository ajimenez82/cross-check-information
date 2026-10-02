# Political-v11 — implementación y revisión real

Actualizado el 01/10/2026. Las etapas están implementadas y la compilación pasa. **La aceptación funcional real sigue fallida**: la síntesis fue rechazada por incumplir las referencias y la elegibilidad temporal. No se presenta como un análisis satisfactorio.

## Configuración y publicación

El agente guardado mantiene el nombre `cross-check-political-v1`. Se publicaron las instrucciones y el esquema de investigación de `infrastructure/src/main/resources/openai/evidence/research.*`. La lectura posterior del agente y de la sesión confirmó las instrucciones. Modelo gpt-5.4-mini, razonamiento low, servicio default, multi_agent desactivado. Revisión local y predeterminada: political-v11.

La síntesis usa una nueva sesión con `synthesis.md` y `synthesis.schema.json`, sin herramientas. No requiere un segundo agente guardado. Las instrucciones de esa sesión también coincidieron con el fichero local. Véase [configuración de Agents](https://developers.openai.com/api/docs/guides/agents-api/configuration).

SHA256 de investigación, normalizando saltos a LF: `2b22fa81ea5069f0d6c7515d980fe416896c51904b0ec255fa2b53df88664063`.

## Consulta y ejecución

Consulta: «¿Los límites al precio del alquiler reducen la oferta de vivienda en alquiler en España? Analiza el periodo 2023–2025 y contrasta publicaciones con distintas posiciones.»

La investigación terminó el 30/09/2026. Una interrupción del entorno de más de dos horas hizo que el trabajo local superase su plazo de diez minutos. El estado original se conserva como FAILED / ANALYSIS_DEADLINE_EXCEEDED. Ese tiempo transcurrido no representa latencia del modelo.

Se reutilizó la investigación guardada, sin repetirla. La captura independiente recuperó dos hallazgos del Banco de España; las otras tres URLs únicas no pudieron capturarse. Una entrada repetida de Esade se agrupó por URL, conservando todos sus hallazgos propuestos. Sus autores discrepantes quedaron sin confirmar. No se sortearon los bloqueos ni se sustituyeron las fuentes.

Para completar el diagnóstico se inició un trabajo separado en una base H2 aislada, desde la etapa SYNTHESIS y con el expediente recuperado. Se ejecutó el worker real; no se modificó el trabajo caducado ni se presentó esto como recuperación automática normal. Una investigación y una síntesis en total, sin reintentos de inferencia.

## Motivo del rechazo

La síntesis propuso INSUFFICIENT_EVIDENCE, pero su apartado de publicaciones incumplió el contrato:

- Intentó contar S1 como NO_EXPLICIT_POSITION aunque sus hallazgos estaban marcados BACKGROUND. La ausencia de postura no elimina el requisito de periodo elegible.
- Introdujo los IDs de fuente S2, S3 y S4 en `findingIds` para excluir documentos inaccesibles. Esos IDs no son hallazgos aceptados.

El validador mantuvo ambas restricciones. El trabajo terminó FAILED / INVALID_ANALYSIS_OUTPUT. No se alteró la respuesta para aparentar una prueba superada ni se mostró un informe final válido en el frontend.

La siguiente mejora concreta es reducir la ambigüedad del paquete de síntesis: separar las fuentes sin hallazgos de los candidatos evaluables, explicitar los IDs permitidos y las restricciones temporales por candidato. Debe cubrirse con pruebas antes de una nueva inferencia. Esto no sustituye la evaluación semántica de posturas.

## Validación y consumo

- `mvn verify`: compilación y pruebas completas superadas tras detener el proceso de prueba que bloqueaba el JAR en Windows. Dos pruebas opcionales de navegador omitidas por esa suite.
- Regresión independiente del frontend: 16 escenarios offline superados; no demuestra éxito del informe real v11.
- Las pruebas Java incluyen contrato del expediente, captura, ensamblado, duplicados y recuperación por etapas. La corrección de duplicados superó 27 pruebas focalizadas antes de la suite completa.
- Investigación: 204.909 tokens de entrada, de ellos 169.984 en caché; 4.600 de salida; total 209.509.
- Síntesis: 9.919 de entrada, sin caché; 1.089 de salida; total 11.008.

Las cifras son uso registrado por la API, no importe facturado. El modelo económico no garantiza bajo consumo en una investigación con búsqueda. No se estimó precio ni se ejecutaron consultas adicionales.

Los artefactos privados, capturas textuales completas, credenciales de trabajo, respuestas originales y bases diagnósticas quedan en `bootstrap/target`, excluido de Git. No copiar claves, tokens ni esos ficheros privados a documentación pública.

## Límites de aceptación

Los controles detectan referencias inválidas y citas ausentes, pero no prueban que el modelo interprete bien causalidad o postura. EV05 y la evaluación semántica amplia siguen pendientes. La nueva arquitectura está implementada; la validación real satisfactoria de extremo a extremo no está cerrada.
