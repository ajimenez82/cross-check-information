# Fase 4 · Referencias cifradas

El endpoint y el proveedor de análisis simulado siguen activándose con `dev`, pero las referencias ya utilizan cifrado autenticado real. No hay registro en memoria, base de datos ni almacenamiento de sesiones en el backend.

## Preparación y ejecución

Desde la carpeta del servicio:

```powershell
mvn clean verify
./scripts/initialize-token-secret.ps1
java -jar bootstrap/target/cross-check-bootstrap-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

El script genera 32 bytes mediante un generador criptográfico y configura `CONVERSATION_TOKEN_SECRET` en Base64 en la terminal actual. No imprime la clave, no escribe archivos y conserva una clave existente. El servidor hereda la variable al arrancar.

Sin clave, con Base64 inválido o con una longitud distinta de 32 bytes, dev no arranca. El error de configuración no incluye el valor rechazado. El arranque sin dev sigue permitiendo consultar salud sin necesitar esta clave.

En otra terminal:

```powershell
./scripts/verify-api.ps1
```

Los escenarios de análisis de la fase 3 siguen disponibles, por ejemplo `--crosscheck.development.scenario=CLASSIFIED`.

### IntelliJ

En Run → Edit Configurations → configuración de CrossCheckApplication:

- Program arguments: `--spring.profiles.active=dev --crosscheck.development.scenario=CLASSIFIED`.
- Environment variables: añadir `CONVERSATION_TOKEN_SECRET` con la clave Base64 generada.

Para copiar la clave de la terminal actual sin imprimirla:

```powershell
Set-Clipboard -Value $env:CONVERSATION_TOKEN_SECRET
```

Pegarla en el campo de variables de entorno de IntelliJ. Mantener la misma clave para los siguientes arranques; no compartir una configuración que incluya la clave ni incorporarla a Git.

La variable creada por el script no es permanente: al cerrar la terminal se pierde salvo que se conserve en otro lugar. IntelliJ abierto previamente no recibe automáticamente esa variable. Un archivo `.env` no se carga automáticamente con esta configuración.

## Formato y validación

Se utiliza [Nimbus JOSE+JWT 10.10](https://central.sonatype.com/artifact/com.nimbusds/nimbus-jose-jwt/10.10), con JWE compacto, algoritmo `dir` y cifrado `A256GCM`. Nimbus genera los valores aleatorios de cada cifrado y verifica la autenticación. Referencia: [JWE con clave compartida](https://connect2id.com/products/nimbus-jose-jwt/examples/jwe-with-shared-key).

El encabezado público contiene únicamente algoritmo, método de cifrado y tipo versionado `crosscheck-conversation-v1`. Dentro del contenido cifrado se incluyen:

- `sessionId`: referencia de sesión.
- `category`: categoría.
- `agentRevision`: revisión del agente.
- `expiresAt`: instante de caducidad UTC, conservando precisión temporal.

No se incluyen consultas, respuestas, informes ni claves. Se aceptan exclusivamente los algoritmos y el tipo previstos; se rechazan cabeceras adicionales, compresión y representaciones Base64URL no canónicas. El token tiene cinco segmentos JWE y deja de utilizar el prefijo `dev_`.

La validación de integridad ocurre antes de leer la caducidad. Un token manipulado, con formato incorrecto o cifrado con otra clave produce 400 `INVALID_CONVERSATION_REFERENCE`. Un token íntegro caducado produce 410 `CONVERSATION_REFERENCE_EXPIRED`, incluido el instante exacto de vencimiento. La categoría y revisión se comprueban también en el caso de uso antes de llamar al proveedor.

La configuración `MAX_TOKEN_LENGTH` sigue siendo 4096 por defecto; el mínimo ahora es 512. El codec limita tanto la entrada como la salida. `CONVERSATION_TOKEN_TTL` mantiene PT2H por defecto; cada resultado correcto renueva el plazo. La clave solo se proporciona mediante `CONVERSATION_TOKEN_SECRET`, sin valor predeterminado.

Los tokens previos del registro en memoria ya no se aceptan. Se ha retirado `DEV_REFERENCE_CAPACITY`, porque ya no existe ese registro.

## Qué revisar

1. Iniciar el servidor con una clave y enviar una consulta: debe devolver un token JWE y un informe simulado.
2. Enviar un seguimiento con ese token: debe devolver 200 y «Seguimiento simulado».
3. Detener y arrancar el servidor desde la misma terminal, manteniendo clave y revisión: el token anterior debe seguir admitiéndose mientras no haya caducado.
4. Cambiar un carácter del contenido cifrado: debe devolver 400 y ningún informe.
5. Arrancar con una clave nueva: el token anterior debe devolver 400.
6. Para revisar caducidad, arrancar con `--crosscheck.analysis.conversation-ttl=PT5S`, obtener un token, esperar más de cinco segundos y enviar el seguimiento: debe devolver 410.

La legibilidad tras reiniciar no garantiza que una sesión remota siga disponible. El proveedor continúa siendo simulado y no conserva contexto semántico; la integración real corresponde a la fase 5.

## Límites

El token es una credencial portadora: quien lo posea puede usarlo mientras sea válido. El cifrado no añade autenticación de usuarios. No hay revocación individual ni protección contra reutilización de un token válido. Cambiar la clave invalida todas las referencias anteriores; no se implementa rotación con varias claves.

La clave no se registra en logs ni se incluye en mensajes de error. Las respuestas de la API mantienen `Cache-Control: no-store`. Los errores de autenticación no revelan si falló la clave, la etiqueta o el contenido.

## Pruebas

Las pruebas verifican ida y vuelta, aleatoriedad, nuevo codec con la misma clave, clave distinta, manipulación del encabezado/IV/contenido/etiqueta, caducidad, políticas de algoritmos, contenido cifrado inválido, límites y arranque con configuración ausente o incorrecta.

La API se sigue probando con peticiones HTTP reales. Las claves de las pruebas se generan en memoria, sin secretos fijos en el repositorio.

Verificación de esta entrega: 112 pruebas (54 de dominio/aplicación, 15 de cifrado y 43 de API/configuración). También se comprobó el JAR con reinicios reales: la referencia se aceptó con la misma clave y se rechazó con una clave nueva. Los procesos temporales se detuvieron y la clave de comprobación no se persistió.

```powershell
mvn clean verify
```

Antes de la fase 5 se presentará el trabajo previsto y se esperará confirmación.
