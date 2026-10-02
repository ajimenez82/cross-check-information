# Political-v10 — instrucciones candidatas para revisión

Estado actualizado: political-v10 publicada el 30/09/2026; esquema 3. La prueba real fue rechazada por respuesta inválida. Véase el [informe de publicación y diagnóstico](political-v10-live-review.md).

El resto de este documento conserva el alcance y la validación de la fase de preparación local, anterior a la publicación.

## Archivos

- [Instrucciones candidatas completas](political-analysis-instructions-v3.md): fichero que incorpora el script de preparación.
- [Copia de las instrucciones anteriores](versions/political-v9.instructions.md): conserva el contenido local previo a este cambio.
- [16 casos de evaluación semántica](political-v10-source-review-cases.md): expectativas sintéticas; todavía no ejecutadas con el modelo.
- [Auditoría que motiva el cambio](../../../../Docs/auditoria-fuentes-informe-2026-09-30.md).

El sufijo v3 del fichero identifica el contrato. Political-v10 es la siguiente revisión propuesta de comportamiento, no otro agente ni otro esquema. La copia de v9 conserva instrucciones; no representa un registro completo de todas las versiones históricas de OpenAI.

## Cambios para revisar

1. Se introduce una secuencia previa a la síntesis: comprobar autor, versión, localizador, periodo, ámbito, política, variable y método para cada hallazgo relevante.
2. Las referencias de segunda mano conservan el periodo del estudio original. Se distingue repetir una evidencia de reproducir una publicación: varios argumentos originales pueden compartir estudio sin ser varias comprobaciones empíricas independientes.
3. La prosa debe conservar el indicador medido y los límites de causalidad. No puede convertir anuncios en viviendas arrendadas, predicciones en observaciones o antecedentes en evidencia actual.
4. Se exige integrar los hallazgos que matizan la conclusión y explicar la función de sus citas.
5. La postura editorial se evalúa separadamente de la suficiencia de la evidencia. Un ensayo descriptivo puede tener postura propia elegible, sin que ello cambie el veredicto documental.
6. El ámbito nacional se decide por la proposición que la publicación realmente aborda. No exige que cada pieza mida todas las regiones, pero tampoco permite inventar una conclusión nacional a partir de un dato local.
7. Una muestra SINGLE ya evaluada conserva AVAILABLE y sus exclusiones aunque tenga cero unidades contables. La falta de acceso puede justificar UNAVAILABLE. La regla MULTIPLE se mantiene.
8. Los códigos de enum se reservan a los campos técnicos; el texto visible emplea descripciones españolas.

Las reglas anteriores de fechas completas o null, distinción autor/tercero, polaridad, contexto, aclaraciones, deduplicación y requisitos COUNT/EXCLUDE continúan vigentes. No se añaden campos al JSON ni se fija un resultado político deseado.

## Preparación local

Desde la carpeta del servicio:

```powershell
./scripts/prepare-openai-agent.ps1 -Model gpt-5.4-mini -SchemaVersion 3
```

Genera `bootstrap/target/political-agent-v3.request.json` con las instrucciones candidatas. El script no lee la API key ni publica. Ese archivo generado todavía no corresponde a la configuración remota activa.

El modelo sigue siendo gpt-5.4-mini, razonamiento low, service tier default y búsqueda web live. No se modifica POLITICAL_AGENT_REVISION ni el fichero local de secretos en esta fase.

## Límites de validación y siguiente fase

Validación local realizada el 30/09/2026:

- Preparación del JSON completada sin publicar el agente.
- Comparación exacta del JSON generado: instrucciones iguales al fichero candidato; todos los demás campos iguales a la plantilla, incluido el esquema.
- `verify-agent-schema.py`: 36 comprobaciones de esquema y 3 adicionales de respuesta v3 superadas; coincidencia de plantilla, contrato interno y configuración confirmada. Se utilizó jsonschema 4.26.0 en una carpeta ignorada de `bootstrap/target`.
- Los 16 casos semánticos permanecen NOT_RUN. No se han ejecutado inferencias ni se ha medido todavía el cumplimiento de las instrucciones por el modelo.

La comprobación de esquema demuestra que el contrato y los ejemplos representables se mantienen; no que el modelo obedezca las nuevas reglas. Los 16 casos son una rúbrica de evaluación pendiente, no 16 pruebas superadas.

Tras revisar este cambio, la siguiente fase propuesta es publicar la configuración preparada, comprobarla por lectura remota y coordinar entonces la revisión local political-v10. Las sesiones nuevas toman la configuración guardada del agente; los resultados históricos no se regeneran. Referencia: [OpenAI, configuración de agentes](https://developers.openai.com/api/docs/guides/agents-api/configuration).

No se han cambiado Java, el frontend o el esquema; tampoco se han realizado inferencias de pago en esta fase.
