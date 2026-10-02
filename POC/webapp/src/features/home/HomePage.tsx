import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { TriangleAlert, ArrowRight, ChevronRight, FileText, Link as LinkIcon, MessageCircle } from 'lucide-react';
import { CategorySelector } from '../../shared/ui/CategorySelector';
import s from './Home.module.css';
import { categories } from '../../shared/model/categories';
import { useConversations } from '../conversations/ConversationProvider';

const ideas = [
  { text: '¿Qué evidencias respaldan esta declaración?', icon: MessageCircle },
  { text: '¿Está este dato político fuera de contexto?', icon: FileText },
  { text: 'Contrasta las afirmaciones de esta noticia', icon: LinkIcon },
];
export function HomePage() {
  const { start } = useConversations();
  const location = useLocation();
  const [query, setQuery] = useState('');
  const [saving, setSaving] = useState(false);
  const submitting = useRef(false);
  useEffect(() => { setQuery(''); }, [location.key]);
  const [categoryId, setCategoryId] = useState(categories[0].id);
  const category = categories.find(item => item.id === categoryId)!;
  const input = useRef<HTMLTextAreaElement>(null);
  const navigate = useNavigate();
  return <div className={s.page}><div className={s.intro}><div className={s.bubble}><MessageCircle /></div><h1>¿Qué información quieres contrastar?</h1><p>Comparte lo que te han contado y descubre qué lo respalda, qué lo contradice y qué todavía no se sabe.</p></div>
    <form className={s.form} onSubmit={async e => { e.preventDefault(); if (!category.available || !query.trim() || submitting.current) return; submitting.current = true; setSaving(true); try { const id = await start(query); if (id) navigate('/resultados/' + id); } finally { submitting.current = false; setSaving(false); } }}><div className={s.category}><CategorySelector value={categoryId} onChange={setCategoryId} /></div><div className={s.description}><FileText /><p id="category-description">{category.description}</p></div><div id="category-availability" role="status">{!category.available && <p className={s.unavailable}><TriangleAlert size={20} /><span>Esta categoría aún no está disponible. Próximamente podrás realizar consultas sobre este tema.</span></p>}</div><label htmlFor="query">Tu consulta</label><textarea ref={input} id="query" disabled={!category.available} value={query} onChange={e => setQuery(e.target.value)} placeholder="Escribe una pregunta, una afirmación o pega un enlace…" aria-describedby="query-help" /><div className={s.formFooter}><small id="query-help">Admite texto y enlaces</small><button className="primary" type="submit" disabled={saving || !category.available || !query.trim()}>Analizar<ArrowRight size={20} /></button></div></form>
    <section className={s.suggestions}><h2>Prueba con una idea</h2><div className={s.ideas}>{ideas.map(({ text, icon: Icon }) => <button key={text} disabled={!category.available} onClick={() => { setQuery(text); input.current?.focus(); }}><Icon /><span>{text}</span><ChevronRight className={s.chevron} /></button>)}</div><p>Selecciona un ejemplo y completa tu consulta antes de analizar.</p></section>
  </div>;
}
