export const demoUser = { name: 'Ale Jiménez', initials: 'AJ', description: 'Usuario de demostración' };
export const demoAnalysis = {
  query: 'El gobierno habla de lawfare en los casos abierto, que valoracion haces de esta afirmacion',
  title: '¿Está respaldada la acusación de lawfare?',
  context: 'La afirmación atribuye motivación política a causas que afectan al Gobierno y su entorno. El PDF examina esa tesis general.',
  summary: 'Según el documento, intervienen distintos órganos judiciales, existen revisiones, correcciones y votos particulares. Estos elementos debilitan una explicación general de coordinación partidista. [J2, J4, J7]',
  verdict: 'No acreditada con las evidencias examinadas',
  sources: [
    { id: 'J2', name: 'Cadena SER', description: 'Recuento de jueces · fuente periodística' },
    { id: 'J4', name: 'RTVE', description: 'Revisión del trámite de jurado' },
    { id: 'J7', name: 'Poder Judicial', description: 'Resolución y votos particulares' },
  ],
};
