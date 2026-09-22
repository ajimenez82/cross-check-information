# Cross Check Service

Backend Java de CrossCheck. **Fase 4: referencias de conversación cifradas**. Una aplicación ejecutable, organizada en cinco módulos Maven.

Revisión de esta entrega: [tokens cifrados, clave e IntelliJ](docs/phase-4-conversation-tokens.md). Escenarios: [API de la fase 3](docs/phase-3-api.md). Contrato base: [fase 2](docs/phase-2-contract.md).

## Requisitos

- JDK 26 (verificado con Oracle JDK 26.0.2.1).
- Maven 3.9.16.
- Spring Boot 4.1.1 y Lombok 1.18.48, fijados en el POM raíz.

Comprobar `java -version`, `javac -version` y `mvn -version`. Maven debe mostrar Java 26; `JAVA_HOME` debe apuntar al JDK, sin el sufijo `bin`.

Referencias de compatibilidad: [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [Lombok](https://projectlombok.org/changelog) y [procesador Maven](https://projectlombok.org/setup/maven).

## Compilar y ejecutar

Desde esta carpeta:

```powershell
mvn clean verify
java -jar bootstrap/target/cross-check-bootstrap-0.0.1-SNAPSHOT.jar
```

Para activar la API simulada:

```powershell
./scripts/initialize-token-secret.ps1
java -jar bootstrap/target/cross-check-bootstrap-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Desde otra terminal, `./scripts/verify-api.ps1` comprueba una consulta y su seguimiento. El perfil dev escucha en `127.0.0.1`; para las peticiones usar `http://127.0.0.1:8080`.

Desde otra terminal:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

La respuesta debe incluir `"status":"UP"`; también puede listar los grupos de salud `liveness` y `readiness`. Detener el servicio con `Ctrl+C`.

El puerto por defecto es 8080. Se puede cambiar mediante `SERVER_PORT` o un argumento:

```powershell
java -jar bootstrap/target/cross-check-bootstrap-0.0.1-SNAPSHOT.jar --server.port=8081
```

La raíz `/` devuelve 404: el backend no sirve la interfaz. El endpoint `POST /api/analysis/start` está disponible únicamente con dev. Actuator expone únicamente `health`, sin detalles internos. No se necesita clave de OpenAI ni base de datos para arrancar.

## Módulos

| Módulo | Responsabilidad y dependencias |
|---|---|
| `domain` | Modelo de dominio; Java puro, sin dependencias externas. |
| `application` | Casos de uso y contratos; depende de `domain`. |
| `infrastructure` | Adaptadores externos; depende de `application` y `domain`. |
| `presentation` | Entrada HTTP; depende de `application`, Spring MVC y validación. |
| `bootstrap` | Arranque y ensamblado; depende de los módulos de aplicación y externos. Único JAR ejecutable. |

Paquete raíz: `com.crosscheck`. `domain` contiene el modelo y sus invariantes; `application`, el caso de uso, sus puertos y DTO; `infrastructure`, los adaptadores simulados; `presentation`, el controller y los errores HTTP. `bootstrap` ensambla explícitamente la API de desarrollo.

## Code language convention

Use English for all identifiers (variables, parameters, fields, classes and methods), code comments and Javadoc, including tests and configuration comments. User-facing Spanish text, example content and project documentation may remain in Spanish.

## IntelliJ y Lombok

1. Abrir el `pom.xml` de esta carpeta como proyecto Maven.
2. Seleccionar JDK 26 en Project SDK y en el JRE del ejecutor/importador Maven. La versión de IntelliJ debe admitir este JDK.
3. Seleccionar Maven 3.9.16, disponible en `C:\Users\jimen\Data\Apps\apache-maven-3.9.16` en el equipo de desarrollo actual.
4. Instalar o habilitar el plugin Lombok en Settings → Plugins y recargar Maven.
5. Si IntelliJ no reconoce el código generado, revisar Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing.
6. Ejecutar `com.crosscheck.bootstrap.CrossCheckApplication`, o utilizar el JAR de los comandos anteriores.

La compilación Maven configura explícitamente Lombok como procesador en `infrastructure`, `presentation` y `bootstrap`. `domain` y `application` deshabilitan el procesamiento de anotaciones. Lombok no se incluye en el JAR ejecutable.

Se utiliza `@Slf4j` en el arranque. En fases posteriores se priorizarán `record` para datos inmutables, `@RequiredArgsConstructor` para dependencias y `@Builder` solo cuando resulte útil; se evitará `@Data` por defecto.

## Git y alcance

El `.gitignore` del servicio excluye `target`, metadatos locales del IDE, logs y configuración local sensible. Los POM, recursos y `lombok.config` se versionan.

El servicio permite consultar su salud y, con dev, ejecutar análisis simulados por HTTP utilizando tokens cifrados. La clave CONVERSATION_TOKEN_SECRET es obligatoria en dev. No hay integración OpenAI ni cambios en el frontend en esta entrega. La siguiente fase comenzará después de revisar esta base y confirmar su inicio.

## Verificación de la fase 1

- `mvn clean verify`: compilación y empaquetado de todos los módulos correctos con JDK 26.0.2.1 y Maven 3.9.16. Todavía no hay pruebas unitarias ni lógica de negocio.
- Arranque del JAR en un puerto libre y comprobación HTTP de salud.
- Comprobación de que Lombok queda fuera de las bibliotecas del ejecutable y de que Git ignora los artefactos y la configuración local.

Durante la compilación, Lombok 1.18.48 emite una advertencia por el uso interno de `sun.misc.Unsafe::objectFieldOffset`, obsoleto en Java 26. No impide compilar; se mantiene visible para poder revisar futuras actualizaciones de Lombok.
