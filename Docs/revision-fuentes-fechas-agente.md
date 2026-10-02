# Revisión de fuentes y fechas del agente

Fecha: 26/09/2026. Estado: instrucciones publicadas y verificadas por lectura remota. Revisión local configurada: political-v6. Evaluación real pendiente.

## Cambio propuesto

Las instrucciones incorporan un procedimiento de verificación antes de redactar el informe: identificar el documento y la versión leídos, localizar su fecha de publicación, determinar el periodo realmente estudiado, evaluar su relación con la pregunta y construir después el periodo de la muestra. Las reglas se expresan en inglés en el archivo de instrucciones; la explicación al usuario continúa en español.

Se utilizan los campos existentes (`contribution`, `limitations`, `selectionCriteria`, `publishedAt`, `period` y `asOf`). No se añaden campos, categorías técnicas, cambios de modelo ni cambios visuales.

## Ejemplos para revisar

Son expectativas de comportamiento, no respuestas nuevas del modelo ni pruebas semánticas superadas.

| Situación | Tratamiento esperado |
| --- | --- |
| Ley fechada el 24 de mayo y publicada en el boletín el 25 | `publishedAt` corresponde al 25 si la fuente enlaza esa publicación. La fecha de la ley y su entrada en vigor se explican separadamente cuando sean relevantes. |
| Registro académico con solo el año 2023 | `publishedAt: null`; conservar «publicado en 2023» en la explicación cuando esté verificado. No inventar 1 de enero o 1 de noviembre. Buscar una fecha completa en la fuente editorial si está disponible. |
| Estudio sobre una regulación de 2020 ante una pregunta limitada a 2023–2025 | Describirlo como antecedente. No contarlo como posición sobre el periodo pedido salvo que la publicación aborde expresamente esa proposición temporal. Registrar su exclusión de la muestra si se evaluó para incluirlo. |
| Informe de 2026 que analiza datos de 2024–2025 | Identificarlo como retrospectivo, conservar su fecha real de 2026 y explicar el periodo de los datos. Puede utilizarse si no se exigió un corte histórico que lo excluya. |
| Página publicada en 2024, actualizada en 2026, con corte solicitado en 2025 | No asumir que el contenido actual ya existía en 2025. Usar una versión verificable disponible antes del corte o excluir el material posterior. |
| Página que solo muestra «actualizado el…» | No convertir esa fecha automáticamente en publicación original; `publishedAt: null` si no puede verificarse la publicación. Explicar la actualización si es relevante. |
| Metadatos y fecha visible contradictorios | Resolver qué documento, versión y tipo de fecha representa cada uno. Si persiste la contradicción, `publishedAt: null` y limitación explícita. |
| Solo se pudo leer la ficha de un estudio | Atribuir lo observado a la ficha, sin afirmar que se leyó el estudio completo. No mezclar una fecha de prepublicación con una versión posterior sin explicarlo. |
| Todas las fuentes clasificadas tienen fecha completa verificada | El periodo de la muestra va de la fecha mínima a la máxima, incluidas las reproducciones agrupadas. Fuentes excluidas y solo documentales no determinan esa ventana. |
| Alguna fuente clasificada carece de fecha completa verificable | `period: null` y explicación de la ventana incompleta; no inventar límites ni afirmar en la prosa una ventana no comprobada. |
| La fuente más reciente es de junio de 2026, sin corte establecido | No convertir automáticamente esa fecha en `asOf`; mantener `null` si no se estableció un corte de evidencias. |

## Qué se puede verificar ahora

- Coherencia de las instrucciones con el contrato existente.
- Preparación de una definición JSON válida con las instrucciones locales.
- Correspondencia de los ejemplos con las reglas propuestas.

Esto no demuestra que el modelo vaya a ejecutarlas correctamente. La comprobación real deberá revisar las páginas y versiones citadas, además del JSON. El validador Java detecta contradicciones internas, pero no autentica las fechas frente a las fuentes.

## Publicación y próximo paso

Se publicaron las instrucciones y se comprobó que su lectura remota coincide exactamente con el archivo local. Se conservaron modelo, esquema, herramientas, razonamiento, nombre y nivel de servicio. Se configuró political-v6 en el fichero local del servicio. No se ejecutó ninguna inferencia. El siguiente paso es acordar una consulta real acotada para comprobar fechas, alcance y clasificación. Al arrancar el servicio se cargará la revisión; si ya está ejecutándose, requiere reinicio. La prueba debe partir de una conversación nueva.

Archivo preparado: `POC/cross-check-service/docs/openai/political-analysis-instructions.md`.
