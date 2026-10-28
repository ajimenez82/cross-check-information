# Categoría Deportes

Implementación local: 21/10/2026. Instrucciones `sports-v1`.

## Comportamiento

Deportes permite contrastar hechos, estadísticas, récords, declaraciones y controversias
de cualquier deporte. Las instrucciones incluyen criterios específicos para decisiones
arbitrales de fútbol: identificar partido y jugada, consultar el reglamento aplicable,
distinguir decisión registrada de corrección y separar el criterio del VAR del criterio
de la decisión original.

Una interpretación concreta y controvertida no se descarta automáticamente como opinión.
Cuando no puede resolverse con pruebas suficientes, se investigan los argumentos de las
publicaciones sobre esa misma afirmación. La política existente deriva Respaldado por
publicaciones o Cuestionado por publicaciones si hay un predominio único en la muestra.
Los porcentajes no representan probabilidad de verdad ni consenso de todos los medios.

Se conservan los ocho resultados finales. El agente devuelve las conclusiones por
afirmación; Java calcula el resultado global y las distribuciones. El desacuerdo sobre
una jugada no convierte una sola afirmación en varias. Para textos con varias afirmaciones,
el contrato actual no calcula una distribución conjunta de publicaciones.

Las instrucciones exigen autoría explícita, deduplicación, diversidad de fuentes y una
descripción honesta del acceso a imágenes y vídeos. Una cita del entrenador no es la
postura del periodista. Una decisión discutible no prueba intencionalidad ni corrupción.
Los casos ambiguos generan una aclaración, conservando el contexto del partido.

## Integración

- Recurso de instrucciones: `POC/cross-check-service/infrastructure/src/main/resources/openai/unified/sports-instructions.md`.
- La categoría del frontend es `sports`; el valor de API es `SPORTS_ANALYSIS`.
- `POST /api/analysis/jobs` y la ruta síncrona admiten `category` opcional.
- Sin categoría ni conversación se conserva Política como valor predeterminado.
- Sin categoría en un seguimiento se hereda la del token. Una categoría explícita distinta
  se rechaza antes de llamar al proveedor.
- La categoría se conserva en trabajos persistidos, tokens e historial local, y participa
  en la comparación de solicitudes idempotentes. Los cuerpos antiguos sin categoría
  conservan su huella y pueden recuperarse sin reenviar una nueva inferencia.
- El historial antiguo sin categoría se interpreta como Política. Los ejemplos y el
  encabezado de resultados cambian según la categoría seleccionada.

El proveedor selecciona las instrucciones al crear cada sesión mediante la configuración
inline que ya utiliza la aplicación. Se reutiliza el ID remoto configurado, con instrucciones
distintas por sesión; no hace falta crear otro agente remoto. Deportes requiere el contrato
v3. El perfil `dev` sigue devolviendo informes explícitamente simulados.

Para preparar una definición revisable sin red ni credenciales:

```powershell
./POC/cross-check-service/scripts/prepare-unified-agent.ps1 -Category sports
```

Genera `bootstrap/target/sports-agent-unified.request.json`. Las instrucciones se incluyen
en el backend compilado. Para utilizar los cambios hay que reiniciar el backend actualizado
y recompilar/publicar el frontend si no se ejecuta con Vite en desarrollo.

## Casos de aceptación

| Caso | Resultado esperado |
|---|---|
| Hecho deportivo comprobable | Respaldado o Refutado según pruebas |
| Estadística con denominador materialmente alterado | Engañoso, explicando la distorsión |
| Roja discutida, predominio de análisis a favor | Respaldado por publicaciones si la conclusión factual es insuficiente |
| Penalti discutido, predominio de análisis en contra | Cuestionado por publicaciones bajo la misma condición |
| Empate o predominio mixto/sin posición | Evidencia insuficiente |
| Preferencia personal sin criterio evaluable | Opinión |
| Dos afirmaciones explícitas con conclusiones distintas | Sin veredicto único |
| Partido o jugador ambiguo | Aclaración antes del informe |
| Noticia con declaraciones enfrentadas, sin postura propia | Sin posición explícita, no Mixto |
| Reproducciones de un mismo análisis | Una sola unidad de posición |

Las pruebas automatizadas cubren selección de instrucciones, API, categoría del token,
seguimientos, rechazo de cambios de categoría, persistencia e idempotencia. El script
`POC/webapp/scripts/verify-sports.cjs` cubre selección, aclaraciones, seguimientos, recarga,
historial antiguo y regresión de Política en escritorio y móvil con respuestas simuladas.
Estas verificaciones no demuestran la calidad semántica de una inferencia real del modelo.

## Revisión global de conversaciones

`AGENT_REVISION` controla la compatibilidad de las conversaciones de todas las categorías.
El valor inicial global es `analysis-v23`; sustituye a `POLITICAL_AGENT_REVISION` y a la
revisión fija de Deportes. Incrementarlo al desplegar cambios de instrucciones o contrato.
Los tokens anteriores no permiten nuevos seguimientos, pero el historial guardado permanece.
La categoría sigue seleccionando las instrucciones. Las etiquetas internas de los recursos
(por ejemplo, `sports-v1`) identifican su contenido, no la compatibilidad del token.
El perfil dev mantiene su valor predeterminado `development-v1`; replay conserva la revisión
del escenario grabado para reproducirlo fielmente. En ambos casos la revisión es común a las categorías.

## Investigación proporcional — analysis-v25

Política y Deportes permiten resolver conocimiento estable y razonamiento sencillo sin
búsqueda obligatoria. El informe explica esa base y no inventa fuentes: las referencias
quedan vacías. Las dudas relevantes, hechos actuales, cifras, declaraciones y reglas
aplicables requieren consulta. Las controversias mantienen investigación y evaluación
de posiciones independientes; el objetivo de 8–12 publicaciones se limita a controversias
ampliamente cubiertas. Si el análisis de publicaciones no es necesario, se utiliza
UNAVAILABLE con el motivo explícito, sin afirmar que no existan publicaciones.
Verificación automatizada: el lector v3 acepta conclusiones resueltas sin fuentes externas.
Esta comprobación no demuestra el comportamiento semántico de una inferencia real.


## Ajustes de investigación y presentación

Las instrucciones distinguen conocimiento estable, cálculo con datos aportados, hechos que
requieren verificación y controversias que requieren análisis de publicaciones. Las
preferencias puras no requieren investigación salvo que se solicite evidencia. Deportes
solo exige contexto de partido en afirmaciones sobre eventos concretos y conserva la
intención pragmática al interpretar preguntas negativas. La interfaz distingue análisis
sin fuentes externas y aclara que una distribución de publicaciones no se calculó.


## Revisión analysis-v26: definiciones y uso

La prueba real aportada por el usuario clasificó el Madrid–Barcelona como derbi en sentido
amplio, sin fuentes. El defecto observado es el salto de una definición principalmente
local a otra amplia sin verificarla ni justificar su aplicación. El texto no afirmaba
literalmente que ambos clubes estuvieran en la misma ciudad. El veredicto por sí solo
no determina si la respuesta es correcta: debe estar respaldado por una interpretación
pertinente y fuentes cuando esa distinción sea decisiva.

Política y Deportes exigen ahora una consulta dirigida cuando el resultado dependa de
acepciones distintas, excepciones o usos institucionales. Deben conectar criterio, hechos
y conclusión, respetar las palabras del usuario y explicar las condiciones decisivas
en el propio veredicto. Una recomendación lingüística no demuestra una prohibición
absoluta; un uso institucional no demuestra consenso universal. La comprobación de
referencias no exige un recuento de posiciones de medios.

### Casos de aceptación semántica para una ejecución real

| Entrada | Comportamiento que debe comprobarse |
|---|---|
| Un partido entre Real Madrid y F.C. Barcelona está considerado como un derbi | Consulta dirigida de definición y uso; fuentes reales; explicación de la acepción aplicada. No se fija de antemano Respaldado o Refutado. |
| Es un derbi local entre equipos de la misma ciudad | Conserva la condición local y comprueba si se cumple; no rescata la afirmación usando una acepción amplia. |
| ¿Cuál es la denominación recomendada para ese enfrentamiento? | Responde sobre la recomendación terminológica, sin convertirla en exclusión de todos los usos alternativos. |
| Todos los derbis enfrentan a equipos de la misma ciudad | Distingue una característica habitual de una condición universal y verifica definiciones o contraejemplos pertinentes. |
| Dos victorias de tres partidos equivalen al 75 % | Refuta mediante 2/3, aproximadamente 66,7 %, sin búsqueda obligatoria. |
| La camiseta de mi equipo es la más bonita | Opinión sin búsqueda ni fuentes inventadas. |
| Este jugador debió ser expulsado por esa entrada | Identifica la acción, consulta reglas y pruebas y analiza posiciones cuando corresponde. |

Estos casos son criterios de revisión, no resultados observados ni pruebas de inferencia
ya ejecutadas. Las pruebas automatizadas del contrato no validan su calidad semántica.

## Revisión analysis-v27
Todas las categorías, incluidas las futuras, deben comprobar pertinencia antes de investigar. Un desajuste devuelve analysis null y clarification OTHER con aviso explícito. Se preservan solapamientos y seguimientos pertinentes. Conocimiento estable primero; web ante incertidumbre material o hechos que requieren verificación. Un matiz terminológico no obliga por sí solo a buscar.

## Revisión analysis-v28
Política y Deportes utilizan gpt-5.4 con reasoning.effort medium y servicio default. Esta revisión distingue las nuevas conversaciones de las ejecutadas con Mini. Requiere reconstruir el servicio y crear una conversación nueva.
