import { useEffect, useRef, useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { BookOpen, CircleHelp, Clock3, FileText, Info, Menu, MessageCircle, Plus, Shield, X } from 'lucide-react';
import { Brand } from '../shared/ui/Brand';
import { Dialog } from '../shared/ui/Dialog';
import { AccountMenu } from '../features/account/AccountMenu';
import s from './layout/Layout.module.css';

const sections = [
  { title: 'Acerca del proyecto', icon: Info, text: 'Contrasta / CrossCheck es un proyecto experimental para explorar afirmaciones políticas, su contexto y las evidencias disponibles. Esta versión es un prototipo interactivo con un resultado de demostración.' },
  { title: 'Cómo funciona', icon: BookOpen, text: 'Escribe una consulta o selecciona una idea y pulsa Analizar. En este prototipo siempre verás el mismo ejemplo. El informe separa contexto, síntesis, valoración documental y posiciones de las publicaciones.' },
  { title: 'Políticas de uso', icon: FileText, text: 'Utiliza el proyecto para examinar información con espíritu crítico. Las respuestas de esta demostración no constituyen una verificación nueva. Consulta las fuentes originales antes de extraer conclusiones.' },
  { title: 'Privacidad', icon: Shield, text: 'Este prototipo no envía tus consultas a un servidor ni a OpenAI. La consulta solo se utiliza para navegar a la demostración. No hay autenticación ni una cuenta real.' },
  { title: 'Ayuda', icon: CircleHelp, text: 'Introduce texto para activar Analizar. Nueva conversación te devuelve al inicio. En móvil puedes abrir la navegación con el botón de menú. No hay un servicio de recepción de comentarios en esta demostración.' },
];

export function App() {
  const [section, setSection] = useState<string | null>(null);
  const [mobile, setMobile] = useState(false);
  const drawer = useRef<HTMLDialogElement>(null);
  const location = useLocation();
  useEffect(() => { setMobile(false); window.scrollTo(0, 0); }, [location]);
  useEffect(() => { if (mobile) drawer.current?.showModal(); else drawer.current?.close(); }, [mobile]);
  const navigation = <><Link className="primary newConversation" to="/" onClick={() => setMobile(false)}><Plus />Nueva conversación</Link><div className={s.history}><h2><Clock3 />Historial</h2><Link className={location.pathname === '/resultados' ? s.selected : ''} to="/resultados"><MessageCircle /><span>Acusación de lawfare<small>Hoy, 10:24</small></span></Link><button onClick={() => { setMobile(false); setSection('Caso Leire Díez'); }}><MessageCircle /><span>Caso Leire Díez<small>Ayer, 10:24</small></span></button></div><nav className={s.footer} aria-label="Información del proyecto">{sections.map(({ title, icon: Icon }) => <button key={title} onClick={() => { setMobile(false); setSection(title); }}><Icon />{title}</button>)}</nav></>;
  return <div className={s.shell}><a className="skipLink" href="#main">Saltar al contenido</a><aside className={s.sidebar}><Link to="/" aria-label="Contrasta, inicio"><Brand /></Link>{navigation}</aside><header className={s.header}><button className={`iconButton ${s.mobileToggle}`} aria-label="Abrir navegación" aria-expanded={mobile} onClick={() => setMobile(true)}><Menu /></button><Link className={s.mobileBrand} to="/" aria-label="Contrasta, inicio"><Brand /></Link><AccountMenu onSelect={setSection} /></header><main id="main" className={s.main}><Outlet /></main><footer className={s.pageFooter}><p>© {new Date().getFullYear()} Contrasta · CrossCheck — Creado por Alejandro Jiménez</p><small>Prototipo de demostración</small></footer>
    <dialog ref={drawer} className={s.drawer} onCancel={() => setMobile(false)} onClick={e => { if (e.target === e.currentTarget) setMobile(false); }} aria-label="Navegación"><div className={s.drawerContent}><div className={s.drawerHead}><Brand /><button className="iconButton" aria-label="Cerrar navegación" onClick={() => setMobile(false)}><X /></button></div>{navigation}</div></dialog>
    {section && <Dialog title={section} onClose={() => setSection(null)}>{section === 'Mi perfil' ? <><span className="avatar">AJ</span><h3>Ale Jiménez</h3><p>Usuario de demostración</p><p>Este perfil ilustra el menú de cuenta del prototipo.</p></> : section === 'Preferencias' ? <><p>Apariencia</p><div className="preference"><span>Tema claro</span><span className="pill">Activo</span></div><p>El prototipo utiliza la apariencia de los mockups v4. Las opciones de tema estarán disponibles en una próxima versión.</p></> : <p>{sections.find(item => item.title === section)?.text ?? 'Esta conversación aparece como ejemplo visual en el historial. El análisis disponible en este prototipo es «Acusación de lawfare».'}</p>}</Dialog>}
  </div>;
}
