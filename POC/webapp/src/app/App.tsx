import { useEffect, useRef, useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { BookOpen, CircleHelp, Clock3, FileText, Info, Menu, MessageCircle, Plus, Shield, X } from 'lucide-react';
import { Brand } from '../shared/ui/Brand';
import { Dialog } from '../shared/ui/Dialog';
import { AccountMenu } from '../features/account/AccountMenu';
import s from './layout/Layout.module.css';
import { useConversations } from '../features/conversations/ConversationProvider';

const sections = [
  { title: 'Acerca del proyecto', icon: Info, text: 'Contrasta lo que te cuentan es un proyecto experimental para explorar afirmaciones políticas, su contexto y las evidencias disponibles. Los informes distinguen contexto, evidencias y posiciones de las publicaciones.' },
  { title: 'Cómo funciona', icon: BookOpen, text: 'Escribe una consulta o selecciona una idea y pulsa Analizar. Según el modo configurado, el servicio devuelve un informe simulado o consulta al agente de OpenAI. Los informes simulados se identifican en su contenido y no evalúan tu consulta.' },
  { title: 'Políticas de uso', icon: FileText, text: 'Utiliza el proyecto para examinar información con espíritu crítico. Los análisis pueden contener errores. Consulta las fuentes originales y las limitaciones antes de extraer conclusiones.' },
  { title: 'Privacidad', icon: Shield, text: 'Tus consultas se envían al servicio Java. En modo OpenAI, se envían también a OpenAI, que mantiene la sesión para los seguimientos; en modo simulado no se llama a OpenAI. El historial y las referencias de conversación se guardan en este navegador. Borrar el historial local no borra las sesiones en OpenAI. No hay autenticación ni una cuenta real.' },
  { title: 'Ayuda', icon: CircleHelp, text: 'Introduce texto para activar Analizar. Nueva conversación te devuelve al inicio. En móvil puedes abrir la navegación con el botón de menú. No hay un servicio de recepción de comentarios en esta demostración.' },
];

export function App() {
  const { conversations, notice, clear } = useConversations();
  const [section, setSection] = useState<string | null>(null);
  const [mobile, setMobile] = useState(false);
  const drawer = useRef<HTMLDialogElement>(null);
  const location = useLocation();
  useEffect(() => { setMobile(false); window.scrollTo(0, 0); }, [location]);
  useEffect(() => { if (mobile) drawer.current?.showModal(); else drawer.current?.close(); }, [mobile]);
  const navigation = <><Link className="primary newConversation" to="/" onClick={() => setMobile(false)}><Plus />Nueva conversación</Link><div className={s.history}><h2><Clock3 />Historial</h2>{conversations.length === 0 && <p>No hay consultas guardadas.</p>}{conversations.map(item => <Link key={item.id} className={location.pathname === '/resultados/' + item.id ? s.selected : ''} to={'/resultados/' + item.id}><MessageCircle /><span>{item.title}<small>{item.turns.at(-1)?.status === 'pending' ? 'En curso' : 'Guardada en este navegador'}</small></span></Link>)}{conversations.length > 0 && <button onClick={() => { if (window.confirm('¿Borrar el historial de este navegador?')) clear(); }}>Borrar historial local</button>}</div><nav className={s.footer} aria-label="Información del proyecto">{sections.map(({ title, icon: Icon }) => <button key={title} onClick={() => { setMobile(false); setSection(title); }}><Icon />{title}</button>)}</nav></>;
  return <div className={s.shell}><a className="skipLink" href="#main">Saltar al contenido</a><aside className={s.sidebar}><Link to="/" aria-label="Contrasta lo que te cuentan, inicio"><Brand /></Link>{navigation}</aside><header className={s.header}><button className={`iconButton ${s.mobileToggle}`} aria-label="Abrir navegación" aria-expanded={mobile} onClick={() => setMobile(true)}><Menu /></button><Link className={s.mobileBrand} to="/" aria-label="Contrasta lo que te cuentan, inicio"><Brand /></Link><AccountMenu onSelect={setSection} /></header><main id="main" className={s.main}>{notice && <p role="status" className={s.storageNotice}>{notice}</p>}<Outlet key={location.pathname} /></main><footer className={s.pageFooter}><p>© {new Date().getFullYear()} Contrasta lo que te cuentan — Creado por Alejandro Jiménez</p><small>Prototipo de demostración</small></footer>
    <dialog ref={drawer} className={s.drawer} onCancel={() => setMobile(false)} onClick={e => { if (e.target === e.currentTarget) setMobile(false); }} aria-label="Navegación"><div className={s.drawerContent}><div className={s.drawerHead}><Brand /><button className="iconButton" aria-label="Cerrar navegación" onClick={() => setMobile(false)}><X /></button></div>{navigation}</div></dialog>
    {section && <Dialog title={section} onClose={() => setSection(null)}>{section === 'Mi perfil' ? <><span className="avatar">AJ</span><h3>Ale Jiménez</h3><p>Usuario de demostración</p><p>Este perfil ilustra el menú de cuenta del prototipo.</p></> : section === 'Preferencias' ? <><p>Apariencia</p><div className="preference"><span>Tema claro</span><span className="pill">Activo</span></div><p>El prototipo utiliza la apariencia de los mockups v4. Las opciones de tema estarán disponibles en una próxima versión.</p></> : <p>{sections.find(item => item.title === section)?.text ?? 'Esta conversación aparece como ejemplo visual en el historial. El análisis disponible en este prototipo es «Acusación de lawfare».'}</p>}</Dialog>}
  </div>;
}
