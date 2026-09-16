import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, ChevronRight, FileText, Link as LinkIcon, MessageCircle } from 'lucide-react';
import { CategorySelector } from '../../shared/ui/CategorySelector';
import s from './Home.module.css';

const ideas = [
  { text: '¿Qué evidencias respaldan esta declaración?', icon: MessageCircle },
  { text: '¿Está este dato político fuera de contexto?', icon: FileText },
  { text: 'Contrasta las afirmaciones de esta noticia', icon: LinkIcon },
];
export function HomePage() {
  const [query, setQuery] = useState('');
  const input = useRef<HTMLTextAreaElement>(null);
  const navigate = useNavigate();
  return <div className={s.page}><div className={s.intro}><div className={s.bubble}><MessageCircle /></div><h1>¿Qué información quieres contrastar?</h1><p>Plantea una afirmación o comparte un enlace.<br />Consulta el análisis, sus fuentes y la valoración final.</p></div>
    <form className={s.form} onSubmit={e => { e.preventDefault(); if (query.trim()) navigate('/resultados'); }}><div className={s.category}><CategorySelector /></div><div className={s.description}><FileText /><p>Contrasta afirmaciones y declaraciones políticas, comprende su contexto y distingue hechos documentados, opiniones e interpretaciones.</p></div><label htmlFor="query">Tu consulta</label><textarea ref={input} id="query" value={query} onChange={e => setQuery(e.target.value)} placeholder="Escribe una pregunta, una afirmación o pega un enlace…" aria-describedby="query-help" /><div className={s.formFooter}><small id="query-help">Admite texto y enlaces</small><button className="primary" type="submit" disabled={!query.trim()}>Analizar<ArrowRight size={20} /></button></div></form>
    <section className={s.suggestions}><h2>Prueba con una idea</h2><div className={s.ideas}>{ideas.map(({ text, icon: Icon }) => <button key={text} onClick={() => { setQuery(text); input.current?.focus(); }}><Icon /><span>{text}</span><ChevronRight className={s.chevron} /></button>)}</div><p>Selecciona un ejemplo y completa tu consulta antes de analizar.</p></section>
  </div>;
}
