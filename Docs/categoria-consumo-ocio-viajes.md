# Categoría Consumo, ocio y viajes

Categoría activa después de Deportes. API: `CONSUMPTION_ANALYSIS`; interfaz: `category-3`
(se conserva el identificador existente). Instrucciones: `consumption-v4`.
Revisión global: `analysis-v37`. Los seguimientos de conversaciones con revisiones
anteriores requieren una conversación nueva; el historial se conserva.

## Criterios

- Pertinencia antes de investigar: fuera de ámbito devuelve `analysis: null` y
  `clarification.reason: CATEGORY_MISMATCH`, sin búsqueda ni veredicto.
- Conocimiento estable y razonamiento primero; consulta para valoraciones, rankings,
  precios y otros hechos específicos o cambiantes. Nunca inventar fuentes.
- Comparar la entidad exacta con alternativas de la misma zona y categoría, explicando
  el criterio de mejor, peor, calidad-precio o encanto, sin sustituir uno por otro.
- Considerar escalas, cantidad y fecha de reseñas, distribución, filtros y metodología.
  No equiparar anuncios u orden de búsqueda con rankings de calidad.
- Leer opiniones favorables y desfavorables accesibles, identificar patrones y distinguir
  incidentes aislados, cambios de gestión y estacionalidad. Explicar límites de la muestra.
- Evaluar argumentos desarrollados aunque procedan de terceros citados. La autoría
  identifica procedencia; no determina admisión ni peso.
- Mantener rankings agregados y reseñas individuales como evidencia documental, separados
  del recuento de publicaciones. Una crítica especializada argumentada puede ser elegible.
- Permitir síntesis e inferencias propias justificadas, sin convertir reputación en
  superioridad universal. No confundir evidencia insuficiente con falsedad u opinión.
- Reutilizar el contrato v3 y los veredictos existentes. No añadir un porcentaje conjunto
  de plataformas, reseñas y publicaciones ni nuevos campos de respuesta.

## Casos de aceptación semántica

| Entrada | Comportamiento esperado |
|---|---|
| Casa Mané es uno de los mejores restaurantes del Campo de Gibraltar | Identificar local, criterio, ámbito y comparadores; investigar reputación y argumentos sin fijar de antemano veredicto. |
| El Camping Roche de Conil es uno de los peores valorados de Conil | Comparar valoraciones equivalentes y cobertura local; unas pocas quejas no demuestran pertenecer a los peores. |
| Laponia es uno de los destinos de viaje con mayor encanto | Separar reputación argumentada, atractivos observables y preferencia; no inventar un ranking universal del encanto. |
| Este restaurante tiene 4,7 sobre 5 | Verificar plataforma y dato actual; no exigir una muestra editorial. |
| Un descuento del 20 % sobre 100 euros deja 80 euros | Cálculo directo sin búsqueda ni fuentes inventadas. |
| Mi color favorito para una maleta es el azul | Opinión sin investigación obligatoria. |
| ¿Cuándo son las elecciones generales? | Desajuste de categoría antes de investigar. |
| Artículo que explica datos de terceros favorables a la comparación | Posición admisible si desarrolla argumentos pertinentes, aunque su autor sea neutral. |

Estos casos son criterios de revisión; las pruebas de contrato no validan por sí solas
el comportamiento semántico real del modelo.

## Preparación y despliegue

Recurso: `POC/cross-check-service/infrastructure/src/main/resources/openai/unified/consumption-instructions.md`.
Para preparar una definición local sin red ni credenciales:

```powershell
./POC/cross-check-service/scripts/prepare-unified-agent.ps1 -Category consumption -Model gpt-5.4
```

Requiere reconstruir y reiniciar el backend y publicar el frontend compilado.
No se ha desplegado ni ejecutado una inferencia real como parte de esta creación.

## Revisión consumption-v2: comparaciones verificadas

Ajuste a partir de la ejecución de Camping Roche aportada el 05/11/2026:
- Verificar el método de ordenación: un puesto por calidad-precio no es un puesto por nota.
- Si se comparan puntuaciones directamente, identificar la comparación como cálculo del
  agente, indicar empates y contar solo establecimientos con puntuación visible.
- Una nota aislada o una comparación con un solo competidor no establece posición baja-media.
- Un fragmento de buscador sin corroborar no fundamenta ni confirma un ranking local completo.
- Conservar «uno de los peor valorados» sin exigir «el peor en todos los portales».
- Reflejar plataforma y cobertura decisivas en la conclusión, sin extrapolaciones universales.

Casos de revisión: listado por calidad-precio con notas en otro orden; ocho resultados con
solo siete notas y empate en la mínima; nota aislada de 3,7/5; directorio inaccesible con
fragmento de buscador; discrepancia entre plataformas ante «uno de los peor valorados».
No se fija un veredicto esperado para Roche: se exige una justificación fiel a lo consultado.
Estos casos no constituyen una validación semántica ejecutada del modelo.

Este ajuste solo modifica instrucciones y revisión de conversaciones. Para aplicarlo hay
que reconstruir y reiniciar el backend; si AGENT_REVISION está fijado en la configuración
real, actualizarlo a analysis-v35. No requiere recompilar el frontend por este ajuste.

## Revisión consumption-v3: alcance y evidencia contraria

Motivada por la segunda ejecución de Roche aportada el 05/11/2026. Se conserva el alcance
original en analysisTarget y en la proposición: no añadir «sin matices» ni exigir acuerdo
universal entre plataformas. Cada fuente debe distinguir comparación favorable, contraria
y ausencia de comparación suficiente. Esta última es una laguna, no una contradicción.

La suficiencia se justifica según cobertura local, método, muestras, actualidad e
independencia, sin unanimidad ni votación entre portales. Una comparación sólida puede
ser suficiente, pero una sola plataforma no demuestra automáticamente reputación general.
La discrepancia real exige ponderación explícita y simétrica de la evidencia.

Casos de aceptación semántica adicionales:

| Caso | Criterio de revisión |
|---|---|
| Comparación local sólida en A y nota aislada en B | B no se presenta como comparación contraria; justificar suficiencia sin exigir unanimidad. |
| Etiqueta Bien y dos competidores Regular | Examinar el conjunto, empates y metodología antes de inferir pertenencia o exclusión del grupo inferior. |
| Puesto 70/70 entre alojamientos especiales | No convertirlo en puesto entre campings ni en prueba adicional de esa clasificación. |
| Afirmación ordinaria sobre reputación | No añadir «sin matices» o «en todas las plataformas» al objetivo. |
| Dos comparaciones locales realmente opuestas | Explicar su cobertura, método y fuerza antes del veredicto, sin decidir por votación ni imponer insuficiencia. |

No se preasigna el veredicto para Roche. Estos son criterios de revisión y no resultados
observados de una nueva inferencia. Aplicación: reconstruir y reiniciar backend, actualizar
AGENT_REVISION a analysis-v36 si está fijado externamente y abrir conversación nueva.

## Revisión consumption-v4: versiones de fuentes y reconsideración

La prueba de seguimiento aportada el 05/11/2026 cambia las notas de camping.info sin
reconciliar versiones y devuelve Refutado sobre una objeción metodológica, en lugar de
cerrar la reconsideración de la afirmación original. Se añaden estas reglas:

- Comprobar entidad, idioma, filtros, población, escala y versión ante datos discrepantes.
- Registrar nota y número de reseñas juntos por URL; no mezclar agregados localizados.
- No interpretar diferencias como evolución temporal ni atribuirlas a filtros por idioma
  sin verificar la causa. Si no se resuelve, exponer la discrepancia y limitar la inferencia.
- En seguimientos de reconsideración, conservar la proposición original y responder sobre
  el peso de la fuente en el razonamiento. Concluir sobre la afirmación original sin
  inventar el veredicto previo cuando no esté disponible en el contexto.
- Mantener como tales las preguntas puramente metodológicas; pedir aclaración si falta
  la proposición que debe reconsiderarse. No añadir una segunda afirmación artificial.

Casos de aceptación semántica (no ejecuciones observadas):

| Caso | Comportamiento esperado |
|---|---|
| ES: 2,5 con 2 reseñas; EN: 3,7 con 12 | Comprobar versiones y filtros; sin prueba, no anunciar una bajada temporal ni una causa concreta. |
| Nota distinta por subcategoría o filtro visible | Identificar el indicador y población; no reemplazar silenciosamente el agregado general. |
| Causa de discrepancia no resuelta | Conservar notas, recuentos y URL; explicar incertidumbre sin anular automáticamente evidencia de otras plataformas. |
| Seguimiento pide reconsiderar el veredicto por una fuente | Mantener la proposición original, responder a la objeción y justificar el resultado revisado, sin convertir la objeción en el veredicto principal. |
| Contexto conserva proposición pero no veredicto anterior | Resolver la afirmación sin inventar qué etiqueta tenía antes. |
| Pregunta exclusivamente metodológica | Respetar el objetivo expresamente solicitado. |

Aplicación: reconstruir y reiniciar backend, fijar AGENT_REVISION en analysis-v37 si hay
una configuración externa y abrir conversación nueva. No se ha ejecutado una inferencia
real para validar semánticamente esta revisión.
