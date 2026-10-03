# Prueba real political-v17 — 03/10/2026

Consulta: «Zapatero ganó una comisión por su asesoramiento en caso de Plus Ultra».

## Ejecución
- COMPLETED en 61,478 segundos observados desde el navegador; una creación y recuperación tras recarga.
- Una sesión y un turno remoto; instrucciones v17 verificadas contra el recurso local.
- Frontend muestra SUPPORTED, tal como resulta del veredicto factual enviado por el agente.
- Ocho fuentes: siete piezas periodísticas de seis medios y un PDF judicial alojado en Primicias.
- Medios periodísticos: elDiario.es (dos), EL PAÍS, El Confidencial, Europa Press, La Vanguardia y The Objective.
- Siete unidades: tres SUPPORTS (42,86 %), cuatro NO_EXPLICIT_POSITION (57,14 %), cero QUESTIONS y cero MIXED. El documento judicial no es una unidad editorial.
- Entrada: 222.174 tokens (183.296 en caché); salida: 6.901; total: 229.075. Sin estimación de coste.
- No se descargaron ni verificaron fuentes en Java. Los títulos, fechas y afirmaciones son contenido del agente.

## Evaluación
El transporte y la presentación funcionan; no se considera validada la calidad del análisis.
1. La explicación desplaza el cobro personal al entorno del sujeto y la existencia de un contrato. Esa justificación no establece por sí sola la proposición exacta.
2. El agente elige SUPPORTED, por lo que no se activa la regla de veredicto por publicaciones. No es un fallo del cálculo Java. Si el veredicto factual fuese insuficiente, esta muestra conservaría INSUFFICIENT_EVIDENCE porque predomina NO_EXPLICIT_POSITION.
3. No devuelve piezas identificadas como opinión o televisión y solo incluye siete publicaciones periodísticas; no explica específicamente estas carencias respecto al objetivo de cobertura.
4. Fija asOf en 23/07/2026, coincidente con la fuente más reciente devuelta, sin explicar ese corte para una consulta de octubre.
5. La clasificación de posturas todavía requiere evaluación: atribuir una acusación a una fuente no demuestra que el autor la adopte.

La revisión es de coherencia del informe generado, no una comprobación independiente del caso ni una conclusión sobre la persona. No se han cambiado instrucciones ni repetido la inferencia tras observar estos resultados.

[Resultado redactado](../../../webapp/review/integration/v17-live-result.json), [escritorio](../../../webapp/review/integration/v17-desktop.png), [móvil](../../../webapp/review/integration/v17-mobile.png).
