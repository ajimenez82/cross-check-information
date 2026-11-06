import { BookOpen, CircleHelp, FileText, Info, Shield } from 'lucide-react';
import s from './layout/Layout.module.css';

const contact = <a href="mailto:contacto@contrastaloquetecuentan.com">contacto@contrastaloquetecuentan.com</a>;

export const sections = [
  { title: 'Acerca del proyecto', icon: Info, content: <>
    <p>«Contrasta lo que te cuentan» es un proyecto experimental que utiliza un modelo avanzado de inteligencia artificial para ayudarte a examinar afirmaciones, preguntas e información que encuentras en tu día a día.</p>
    <p>Puedes explorar temas de política, deportes, consumo, ocio y viajes, dentro de las categorías disponibles. La herramienta busca y analiza fuentes, aporta contexto y distingue entre hechos comprobables, opiniones y cuestiones para las que todavía no hay evidencias suficientes.</p>
    <p>El proyecto está en fase beta y continúa evolucionando. Los análisis pueden contener errores u omisiones: su propósito es ayudarte a formar tu propio criterio, facilitando explicaciones y referencias que puedas consultar.</p>
  </> },
  { title: 'Cómo funciona', icon: BookOpen, content: <>
    <h3>1. Plantea tu consulta</h3>
    <p>Selecciona una categoría y escribe una pregunta, una afirmación o un enlace. Puedes utilizar los ejemplos como punto de partida. Cuanto más concreto sea el contexto —qué ocurrió, dónde y cuándo—, más preciso podrá ser el análisis.</p>
    <h3>2. La herramienta analiza la información</h3>
    <p>Un modelo avanzado de inteligencia artificial interpreta tu consulta, busca información en la web y examina las fuentes disponibles. Si necesita más contexto, puede pedirte una aclaración antes de elaborar el resultado.</p>
    <h3>3. Consulta la valoración y sus motivos</h3>
    <p>El informe incluye una valoración final, su explicación, las fuentes citadas y las limitaciones del análisis. Según la consulta, puede indicar:</p>
    <ul>
      <li><strong>Respaldada o refutada:</strong> las evidencias examinadas apoyan o contradicen la afirmación.</li>
      <li><strong>Engañosa o fuera de contexto:</strong> la formulación omite contexto o puede llevar a una conclusión equivocada.</li>
      <li><strong>Evidencia insuficiente:</strong> no hay información suficiente para concluir. Esto no significa que la afirmación sea falsa.</li>
      <li><strong>Interpretación u opinión:</strong> la afirmación depende de valoraciones o criterios que no pueden resolverse únicamente con hechos.</li>
      <li><strong>Sin veredicto único:</strong> la consulta reúne cuestiones que requieren conclusiones diferentes.</li>
      <li><strong>Respaldada o cuestionada por las publicaciones:</strong> describe lo que sostienen las publicaciones analizadas, sin convertir ese respaldo o cuestionamiento en una prueba definitiva.</li>
    </ul>
    <h3>4. Explora las publicaciones y las fuentes</h3>
    <p>Cuando hay información suficiente, el apartado «Posiciones en las publicaciones» muestra cuáles respaldan la proposición analizada, cuáles la cuestionan, cuáles presentan una postura mixta y cuáles no expresan una posición explícita.</p>
    <p>Los porcentajes describen únicamente la muestra examinada: no representan la opinión pública ni la probabilidad de que una afirmación sea cierta. Las reproducciones agrupadas cuentan como una sola unidad. Puedes consultar el desglose, los motivos de clasificación y las referencias para revisar el contenido original.</p>
    <h3>5. Continúa la conversación</h3>
    <p>Puedes pedir aclaraciones, aportar información adicional o profundizar en el resultado dentro de la misma conversación, conservando el contexto disponible. El historial de este navegador permite volver a las consultas guardadas. Para cambiar de tema o de categoría, inicia una nueva conversación.</p>
  </> },
  { title: 'Políticas de uso', icon: FileText, content: <p>Utiliza el proyecto para examinar información con espíritu crítico. Los análisis pueden contener errores. Consulta las fuentes originales y las limitaciones antes de extraer conclusiones.</p> },
  { title: 'Privacidad', icon: Shield, content: <>
    <p>Las consultas y la información que aportas se envían a los servicios que procesan el análisis, incluido un proveedor externo de inteligencia artificial. Para permitir preguntas de seguimiento, se utiliza el contexto de la conversación.</p>
    <p>El historial, las consultas y los resultados se guardan también en este navegador. Si compartes el dispositivo o el perfil del navegador, otras personas que lo utilicen podrían acceder a ese historial.</p>
    <p>Puedes eliminarlo mediante «Borrar historial local». Esta acción elimina la copia guardada en el navegador; no elimina por sí misma los datos procesados por los servicios externos ni cancela los análisis que ya estén en curso.</p>
    <p>Evita incluir contraseñas, datos bancarios o información personal sensible que no sea necesaria para tu consulta.</p>
    <p>Para consultas sobre privacidad, puedes escribir a {contact}.</p>
  </> },
  { title: 'Ayuda', icon: CircleHelp, content: <>
    <p>Selecciona una categoría disponible, escribe tu consulta y pulsa «Analizar». Puedes introducir una afirmación, formular una pregunta o pegar un enlace, añadiendo el contexto que consideres relevante.</p>
    <p>Cuando recibas el resultado, podrás explorar las fuentes y continuar la conversación para pedir aclaraciones o aportar más información. Para empezar otro tema, pulsa «Nueva conversación». En móvil, encontrarás la navegación y el historial en el botón de menú.</p>
    <p>Si encuentras un problema, tienes alguna duda o quieres enviar una sugerencia, escríbenos a {contact}. Si se trata de un error, describe qué ocurrió e incluye la referencia de diagnóstico si aparece en pantalla.</p>
  </> },
];

export function InformationSection({ title }: { title: string }) {
  return <div className={s.informationContent}>{sections.find(section => section.title === title)?.content}</div>;
}
