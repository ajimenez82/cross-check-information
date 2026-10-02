# Revisión del resultado real v13 en la interfaz

01/10/2026. Reproducción offline de la respuesta real aceptada: `political-v13-result.json`. Token de conversación redactado. Todas las peticiones de análisis interceptadas por Playwright; cero llamadas al backend o a OpenAI. No es una prueba nueva de extremo a extremo.

## Resultado

Correcto a 1536 y 390 píxeles, sin errores JavaScript ni desbordamiento horizontal:

- Evidencia insuficiente se presenta con su icono y estilo.
- Cero publicaciones contables muestra «No calculable · 0 unidades clasificadas», sin barra ni porcentajes.
- La única publicación excluida conserva su explicación y trazabilidad.
- En móvil el posicionamiento está después del análisis y antes de fuentes; en escritorio está a la derecha.
- Las referencias abren la ficha del Banco de España, con fechas de publicación y consulta diferenciadas.
- Recargar conserva el resultado sin otra petición de análisis.

Evidencias: [escritorio](political-v13-1536.png), [móvil](political-v13-390.png), [ficha de fuente móvil](political-v13-source-390.png). Script reproducible: `scripts/verify-v13-result.cjs`, con `PLAYWRIGHT_MODULE` apuntando a la instalación disponible y `APP_URL` al frontend local.

## Hallazgos editoriales pendientes

1. La prosa del proveedor muestra ANALYSIS, REPORTING y BACKGROUND. Los campos estructurados sí se traducen, pero esos textos libres llegan así desde la síntesis.
2. La explicación mezcla contexto fuera del periodo elegible con ausencia de causalidad. No demostrar causalidad no basta para excluir una postura editorial: son criterios distintos.
3. Las limitaciones mencionan S2–S4, que no figuran como fuentes consultables porque no se capturaron. Deben identificarse de forma comprensible sin presentarlas como evidencia verificada.
4. Hay afirmaciones heredadas de investigación sobre documentos no capturados. Debe distinguirse lo investigado de lo verificado.

No se ha maquillado ni modificado la respuesta guardada. No se requieren cambios de distribución para este caso. La corrección siguiente corresponde al paquete y las instrucciones de síntesis; después habrá que comprobar una ejecución completa nueva.
