# Contrasta · CrossCheck

Prototipo responsive basado en los cuatro mockups v4 de `Mockups/`.

## Desarrollo

Requiere Node.js 20.17 o superior y npm.

```sh
npm install
npm run dev
```

## Compilación

```sh
npm run build
npm run preview
```

React, TypeScript, Vite, React Router, CSS Modules y Lucide. El hosting debe redirigir las rutas de la SPA a `index.html`.

## Alcance

- `/`: inicio con once categorías y descripciones del documento de requisitos; solo Análisis político permite escribir y enviar. Las categorías futuras muestran un aviso y desactivan editor, ejemplos y envío, conservando el borrador al volver. Analizar se desactiva para entradas vacías o compuestas solo por espacios. Las ideas rellenan el campo sin enviar.
- `/resultados`: categoría fija como texto e informe fijo ilustrativo, independientemente del texto introducido. La consulta mostrada también pertenece al ejemplo.
- Cuenta desplegable con cierre mediante segundo clic, clic fuera y Escape.
- Navegación móvil con diálogo modal, foco contenido y cierre con Escape.
- Perfil, preferencias, información y fuentes mediante diálogos. El tema claro es el único implementado; no hay autenticación.
- El historial es una muestra visual. El caso de lawfare abre el informe; el otro caso explica su carácter ilustrativo.
- Los seguimientos muestran un aviso de demostración. No se inventan enlaces a las referencias.
- Sin backend, OpenAI ni persistencia. Datos de ejemplo separados en `src/mocks/analysis.ts`.

Los estilos se organizan por funcionalidad y comparten variables de diseño en `src/shared/styles/tokens.css`.

Pie compartido en ambas páginas con año automático, nombre del proyecto, creador Alejandro Jiménez y etiqueta de prototipo.
