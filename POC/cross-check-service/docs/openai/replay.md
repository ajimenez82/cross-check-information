# Pruebas reales y reproducción local

El perfil `replay` reproduce respuestas originales de political-v19 y political-v20 sin conectarse a OpenAI. Usa el parser v3 real, sus validaciones, el cálculo de veredicto del servicio y el flujo de trabajos asíncronos que consume el frontend. No sirve informes finales precalculados.

## Ejecutar desde IntelliJ

En la configuración de ejecución del servicio:
- **Active profiles:** `replay,local`.
- **Program arguments:** `--crosscheck.replay.scenario=PLUS_ULTRA_V19`.
- **Working directory:** la carpeta `POC/cross-check-service`.

Como alternativa, configura `REPLAY_SCENARIO=PLUS_ULTRA_V19` como variable de entorno. No mezcles `replay` con `dev` ni `openai`. La clave de OpenAI no se utiliza; el secreto de conversación local sigue siendo necesario para los tokens del servicio.

Arranca el frontend normalmente, con API_PROXY_TARGET apuntando al puerto de este servicio. Abre una conversación nueva y pega la consulta exacta del escenario. Para cambiar de caso, cambia el argumento o variable, reinicia Java y abre otra conversación nueva. El perfil usa `data/replay`; puedes cambiarla con REPLAY_DATABASE_PATH.

También puedes ejecutar el JAR:
```powershell
java -jar bootstrap/target/cross-check-bootstrap-0.0.1-SNAPSHOT.jar --spring.profiles.active=replay,local --crosscheck.replay.scenario=PLUS_ULTRA_V19
```

## Casos

| Escenario | Consulta exacta |
|---|---|
| PLUS_ULTRA_V20 | Zapatero ganó una comisión por su asesoramiento en caso de Plus Ultra |
| PLUS_ULTRA_V19 | Zapatero ganó una comisión por su asesoramiento en caso de Plus Ultra |
| VOTO_OBLIGATORIO_V19 | En España es obligatorio votar en las elecciones generales. |
| ALQUILERES_V19 | La limitación de los precios del alquiler ha reducido la oferta de viviendas en alquiler en Cataluña entre 2024 y 2025. |
| ACLARACION_V19 | ¿Está volviendo la situación a la normalidad? |

## Archivos y fidelidad

En `infrastructure/src/main/resources/openai/replay/<ESCENARIO>/`:
- `response.json`: texto JSON original emitido por OpenAI, antes del procesamiento de Java.
- `metadata.json`: consulta, revisión, modelo, fechas de captura, consumo y huellas SHA-256 de instrucciones y respuesta. Sin claves, tokens de conversación ni identificadores de sesiones remotas.

No se corrigen respuestas defectuosas para hacerlas pasar. Si una captura no cumple el contrato, replay debe reproducir el error de procesamiento. La huella detecta cambios accidentales del archivo; no es una firma criptográfica de autoría.

El ciclo remoto de OpenAI se sustituye por un proveedor local sin red. Los identificadores de trabajo, sesión local y tokens se generan de nuevo; no se reproducen cabeceras HTTP ni credenciales. Los tiempos y sellos del servicio son los de la reproducción. Las fechas y el contenido que escribió la IA permanecen en la captura. Una captura histórica no equivale a una consulta actualizada.

Estos escenarios son primeros turnos independientes. Una pregunta distinta o una continuación no grabada produce INVALID_ANALYSIS_OUTPUT; abre una conversación nueva para repetir. El escenario y la huella forman parte de la revisión de conversación, para impedir reutilizar tokens entre capturas distintas.

## Añadir una prueba real

Primero genera la configuración actual con `./scripts/prepare-unified-agent.ps1`. Después:
```powershell
python scripts/capture-openai-replay.py --scenario NUEVO_CASO_V19 --query "Consulta exacta"
```

Este comando **sí usa OpenAI y genera consumo**. Lee la configuración local sin imprimir secretos. Conserva el estado privado bajo bootstrap/target/replay-capture para reanudar una sesión en curso sin reenviarla. Si el envío inicial queda incierto, se detiene para reconciliarlo en vez de duplicarlo. Una respuesta ya capturada no se sobrescribe: usa otro nombre para una nueva ejecución. No borres ese estado mientras una captura esté pendiente.

Compila de nuevo para incluir capturas nuevas en el classpath/JAR. Las capturas permiten repetir las pruebas de Java sin llamadas remotas. El modo dev tradicional conserva sus escenarios sintéticos.

## Comprobación de navegador automatizada

Con el JAR compilado, Java 26 disponible en JAVA_HOME y Playwright instalado, ejecuta desde webapp:
```powershell
node scripts/verify-replay.cjs
```
PLAYWRIGHT_MODULE permite indicar la ruta de un módulo Playwright externo. Usa Microsoft Edge en modo headless, reserva los puertos 5179 y 8097, arranca procesos ocultos propios y los cierra al terminar. Prueba cada escenario sin clave de OpenAI, comprueba recarga y vista móvil, y guarda resultados redactados y capturas en review/integration/verdict-first. Requiere el secreto de conversación de config/application-local.yml.

## Resultados verificados

[Informe de capturas reales y reproducción local](political-v19-replay-review.md). Los cuatro casos completaron el procesamiento y la presentación en navegador; el informe separa esa validación técnica de las limitaciones del análisis generado.

Para reproducir la nueva redaccion usa `--crosscheck.replay.scenario=PLUS_ULTRA_V20`. Para comprobar solo ese caso en navegador, establece `REPLAY_SCENARIOS=PLUS_ULTRA_V20` antes de ejecutar el script.
