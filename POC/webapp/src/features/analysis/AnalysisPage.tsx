import { useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { FileText, CircleCheck, CircleX, TriangleAlert, CircleHelp, MessageCircle, GitFork, Newspaper, Link as LinkIcon, List, Send } from 'lucide-react';
import { categories } from '../../shared/model/categories';
import { Dialog } from '../../shared/ui/Dialog';
import { useConversations, type Turn } from '../conversations/ConversationProvider';
import { positionLabels, verdictLabels, type Analysis, type Source, type Position, type PublicationTrace } from './api/analysisContract';
import s from './Analysis.module.css';

const formatDate = (value: string) => new Date(value).toLocaleString('es-ES');
const positions = Object.keys(positionLabels) as Position[];
const verdictIcons = {
  SUPPORTED: CircleCheck, REFUTED: CircleX, MISLEADING: TriangleAlert,
  INSUFFICIENT_EVIDENCE: CircleHelp, OPINION: MessageCircle, NO_SINGLE_VERDICT: GitFork,
  SUPPORTED_BY_PUBLICATIONS: Newspaper, QUESTIONED_BY_PUBLICATIONS: Newspaper,
};

function ReferenceButtons({ ids, sources, onSource }: { ids: string[]; sources: Source[]; onSource: (source: Source) => void }) {
  return <div className={s.references}>{ids.map(id => {
    const source = sources.find(item => item.id === id);
    return source && <button key={id} onClick={() => onSource(source)} aria-label={`Ver referencia ${source.title}`}>[{id}]</button>;
  })}</div>;
}
function TraceDetails({ trace, sources, onSource }: { trace?: PublicationTrace | null; sources: Source[]; onSource: (source: Source) => void }) {
  if (!trace) return <p>Trazabilidad no disponible</p>;
  const owners = { AUTHOR: 'Autor de la publicación', THIRD_PARTY: 'Tercero citado', NONE: 'Sin postura propia', UNDETERMINED: 'Autoría no determinada' };
  const periods = { REQUESTED_PERIOD: 'Periodo analizado', RETROSPECTIVE: 'Evaluación retrospectiva', BACKGROUND: 'Antecedente', UNKNOWN: 'Relación temporal desconocida' };
  const matches = { MATCH: 'Coincide con la proposición', PARTIAL: 'Coincidencia parcial', OUT_OF_SCOPE: 'Fuera del alcance', UNCERTAIN: 'Alcance incierto' };
  const attributions = { AUTHOR: 'Argumento del autor', THIRD_PARTY: 'Opinión de un tercero', DESCRIPTIVE: 'Descripción' };
  const relations = { SUPPORTS: 'Respalda', QUESTIONS: 'Cuestiona', CONTEXT: 'Aporta contexto' };
  return <div className={s.trace}>
    <p><strong>Quién sostiene la postura:</strong> {owners[trace.stanceOwner]}{trace.stanceHolder && ` · ${trace.stanceHolder}`}</p>
    <p><strong>Alcance:</strong> {trace.scope.outcome} · {trace.scope.geography}</p>
    {trace.scope.studyPeriod && <p><strong>Periodo estudiado:</strong> {trace.scope.studyPeriod}</p>}
    {trace.scope.policy && <p><strong>Medida analizada:</strong> {trace.scope.policy}</p>}
    <p>{matches[trace.scope.match]} · {periods[trace.temporalRelation]}</p>
    <p>{trace.scope.explanation}</p>
    <p><strong>Argumentos y referencias</strong></p>
    {trace.arguments.map((argument, index) => <div key={index}>
      <p><strong>{attributions[argument.attribution]} · {relations[argument.relation]}</strong></p>
      <p>{argument.paraphrase}</p>
      <p>{argument.locator ? `Localización: ${argument.locator}` : 'Localización no disponible'}</p>
      <ReferenceButtons ids={[argument.sourceId]} sources={sources} onSource={onSource} />
    </div>)}
    <p className={s.scope}>Justificación declarada en el análisis; no es una verificación independiente de la fuente.</p>
  </div>;
}
function Report({ data, onSource }: { data: Analysis; onSource: (source: Source) => void }) {
  const VerdictIcon = verdictIcons[data.verdict.status];
  const classification = data.publicationPositions;
  const total = classification.units.length;
  const counts = positions.map(position => ({ position, count: classification.units.filter(unit => unit.position === position).length }));
  return <div className={s.report}>
    <article className={s.article}>
      <div className={s.author}><img src="/assets/brand.svg" alt="" /><strong>Contrasta</strong><time dateTime={data.analyzedAt}>{formatDate(data.analyzedAt)}</time></div>
      <h2 className={s.reportTitle}>{data.title}</h2>
      {data.asOf && <p>Corte de las evidencias: {data.asOf}</p>}
      <section className={s.textSection}><FileText /><div><h3>Contexto</h3><p>{data.context}</p></div></section>
      <section className={s.textSection}><List /><div><h3>Síntesis</h3><p>{data.summary}</p><ReferenceButtons ids={data.summarySourceIds} sources={data.sources} onSource={onSource} /></div></section>
      <section className={s.verdict} data-verdict={data.verdict.status}><span className={s.flag} aria-hidden="true"><VerdictIcon /></span><div><h3>Valoración final</h3><strong className={s.verdictLabel}>{verdictLabels[data.verdict.status]}</strong><p>{data.verdict.explanation}</p><p>Respaldo documental: {data.verdict.documentarySupport}</p><ReferenceButtons ids={data.verdict.sourceIds} sources={data.sources} onSource={onSource} /></div></section>
      {data.claimAnalysis && <section className={s.trace} aria-label="Valoración por afirmaciones">
        <h3>Valoración por afirmaciones</h3><p>{data.claimAnalysis.analysisTarget}</p>
        <p>{data.claimAnalysis.decomposition.explanation}</p>
        {data.claimAnalysis.claims.map(claim => <section key={claim.id} className={s.breakdown}>
          <h4>{claim.proposition}</h4><strong>{verdictLabels[claim.verdict.status]}</strong>
          <p>{claim.verdict.explanation}</p><p>Respaldo documental: {claim.verdict.documentarySupport}</p>
          <ReferenceButtons ids={claim.verdict.sourceIds} sources={data.sources} onSource={onSource} />
        </section>)}
      </section>}
      {data.limitations.length > 0 && <section className={s.limitations}><h3>Limitaciones</h3><ul>{data.limitations.map((limit, index) => <li key={index}>{limit}</li>)}</ul></section>}
    </article>
    <aside className={s.positions}>
      <h3>Posiciones en las publicaciones</h3>
      {classification.availability === 'UNAVAILABLE' ? <p><strong>Medición no disponible</strong></p> : total === 0 ? <p><strong>No calculable</strong> · 0 unidades clasificadas</p> : <>
        <p><strong>{total} unidades clasificadas</strong></p>
        <div className={s.dynamicBar} aria-hidden="true">{counts.map(({ position, count }) => <span key={position} data-position={position} style={{ width: `${100 * count / total}%` }} />)}</div>
        <ul className={s.legend}>{counts.map(({ position, count }) => <li key={position}>{positionLabels[position]}: {count} de {total} ({(100 * count / total).toLocaleString('es-ES', { maximumFractionDigits: 1 })} %)</li>)}</ul>
      </>}
      {classification.reason && <p>{classification.reason}</p>}
      {classification.proposition && <p><strong>Proposición:</strong> {classification.proposition}</p>}
      {classification.selectionCriteria && <p><strong>Selección:</strong> {classification.selectionCriteria}</p>}
      {classification.period && <p>Periodo: {classification.period.from} — {classification.period.to}</p>}
      {classification.consultedAt && <p>Consulta de la muestra: {formatDate(classification.consultedAt)}</p>}
      {classification.units.length > 0 && <details><summary>Ver desglose de publicaciones</summary>{classification.units.map(unit => <section key={unit.id} className={s.breakdown}><strong>{positionLabels[unit.position]}</strong><p>{unit.explanation}</p><ReferenceButtons ids={unit.sourceIds} sources={data.sources} onSource={onSource} /><TraceDetails trace={unit.trace} sources={data.sources} onSource={onSource} /></section>)}</details>}
      {classification.excluded.length > 0 && <details><summary>Publicaciones excluidas ({classification.excluded.length})</summary>{classification.excluded.map(item => <section className={s.breakdown} key={item.sourceId}><p>{item.reason}</p><ReferenceButtons ids={[item.sourceId]} sources={data.sources} onSource={onSource} /><TraceDetails trace={item.trace} sources={data.sources} onSource={onSource} /></section>)}</details>}
      <p className={s.scope}>Describe la muestra examinada; no mide opinión pública ni probabilidad de verdad. Las reproducciones agrupadas cuentan como una sola unidad.</p>
    </aside>
    <section className={s.sources}><h3><LinkIcon />Fuentes</h3>{data.sources.length ? <div>{data.sources.map(source => <button key={source.id} onClick={() => onSource(source)}><FileText /><span><strong>[{source.id}] {source.title}</strong><small>{source.contribution}</small></span></button>)}</div> : <p>No se han aportado fuentes para este resultado.</p>}</section>
  </div>;
}
function PendingTurn({ turn }: { turn: Turn }) {
  const [now, setNow] = useState(Date.now());
  useEffect(() => { const timer = window.setInterval(() => setNow(Date.now()), 1000); return () => window.clearInterval(timer); }, []);
  const labels = { QUEUED: 'Consulta en cola…', SUBMITTING: 'Preparando el análisis…', RUNNING: 'Analizando la consulta…', RECOVERING: 'Recuperando el estado del análisis…', COMPLETED: 'Guardando el resultado…', FAILED: 'Consultando el resultado…' };
  const disconnected = turn.job?.connectionLost || !navigator.onLine;
  return <div className={s.loading} role="status">
    <p>{disconnected ? 'Sin conexión con el servicio. Recuperaremos el mismo análisis al reconectar.' : turn.job?.status ? labels[turn.job.status] : 'Enviando la consulta guardada…'}</p>
    {now - Date.parse(turn.createdAt) >= 90000 && <p>El análisis está tardando más de lo habitual.</p>}
    <p>Puedes recargar esta página: el seguimiento continuará sin crear otra solicitud.</p>
  </div>;
}
function FailedTurn({ turn, onRetry, onRecover }: { turn: Turn; onRetry: (text: string) => void; onRecover: () => void }) {
  const [text, setText] = useState(turn.text);
  const error = turn.error!;
  return <section className={s.error} role="alert"><h2>No se ha completado el análisis</h2><p>{error.message}</p>
    {error.uncertain && <p>La ejecución podría haber continuado en el servidor. Iniciar otra consulta puede repetir el trabajo.</p>}
    {error.requestId && <small>Referencia de diagnóstico: {error.requestId}</small>}
    {turn.job && error.code === 'INVALID_RESPONSE' ? <button className="primary" onClick={onRecover}>Recuperar este análisis</button> : error.resetRequired || turn.job ? <Link to="/" className="primary">Iniciar nueva conversación</Link> :
      <form onSubmit={event => { event.preventDefault(); onRetry(text); }}>
        <label htmlFor={`retry-${turn.id}`}>Consulta para reintentar</label>
        <textarea id={`retry-${turn.id}`} value={text} onChange={event => setText(event.target.value)} />
        <button className="primary" disabled={!text.trim()}>Reintentar</button>
      </form>}
  </section>;
}
export function AnalysisPage() {
  const { id } = useParams();
  const { conversations, send, recover } = useConversations();
  const conversation = conversations.find(item => item.id === id);
  const [source, setSource] = useState<Source | null>(null);
  const [followUp, setFollowUp] = useState('');
  if (!id && conversations[0]) return <Navigate to={`/resultados/${conversations[0].id}`} replace />;
  if (!conversation) return <div className={s.page}><h1>No hay una conversación disponible</h1><p>Puede haberse borrado el historial de este navegador.</p><Link className="primary" to="/">Nueva conversación</Link></div>;
  const lastTurn = conversation.turns.at(-1);
  const pending = lastTurn?.status === 'pending';
  const canFollowUp = Boolean(conversation.token) && lastTurn?.status === 'done'
    || lastTurn?.status === 'clarification' && (Boolean(conversation.token) || lastTurn.clarification?.reason === 'CONTEXT_UNAVAILABLE');
  return <div className={s.page}>
    <h1 className="srOnly">Conversación de análisis político</h1>
    <div className={s.toolbar}><div><strong>Categoría:</strong><span>{categories[0].name}</span></div></div>
    <p className={s.warning}>Prototipo · revisa las fuentes y las limitaciones de cada informe. Los informes marcados como simulados no evalúan tu consulta.</p>
    {conversation.turns.map(turn => <section key={turn.id} className={s.turn} aria-busy={turn.status === 'pending'}>
      <section className={s.message} aria-label="Tu consulta"><span className="avatar">AJ</span><div><strong>Ale Jiménez</strong><time dateTime={turn.createdAt}>{formatDate(turn.createdAt)}</time><p>{turn.text}</p></div></section>
      {turn.status === 'pending' && <PendingTurn turn={turn} />}
      {turn.status === 'error' && <FailedTurn turn={turn} onRecover={() => recover(conversation.id, turn.id)} onRetry={text => { void send(conversation.id, text, turn.id); }} />}
      {turn.analysis && <Report data={turn.analysis} onSource={setSource} />}
      {turn.status === 'clarification' && turn.clarification && <section className={s.notice} aria-label="Aclaración necesaria">
        <h2>Necesito una aclaración</h2><p>{turn.clarification.question}</p>
        <p>Responde en el campo de seguimiento para continuar.</p>
      </section>}
    </section>)}
    <form className={s.composer} onSubmit={event => {
      event.preventDefault();
      if (canFollowUp && followUp.trim()) { void send(conversation.id, followUp); setFollowUp(''); }
    }}><label className="srOnly" htmlFor="follow-up">Pregunta de seguimiento</label><input id="follow-up" disabled={!canFollowUp} value={followUp} onChange={event => setFollowUp(event.target.value)} placeholder="Escribe una pregunta o pega un enlace…" /><button className="primary" disabled={!canFollowUp || !followUp.trim()} aria-label="Enviar seguimiento"><Send /></button></form>
    {pending && <p className={s.notice}>Espera a que termine esta consulta para enviar un seguimiento.</p>}
    {source && <Dialog title={source.title} onClose={() => setSource(null)}>
      <p>{source.contribution}</p>{source.publisher && <p>Publicación: {source.publisher}</p>}
      <p>Fecha de publicación: {source.publishedAt ?? 'No disponible'}</p><p>Consulta: {source.consultedAt ? formatDate(source.consultedAt) : 'No disponible'}</p>
      <a href={source.url} target="_blank" rel="noopener noreferrer">Abrir fuente en una pestaña nueva</a>
      <p>Consulta el contenido original y las limitaciones indicadas. En los informes simulados, los enlaces son ilustrativos.</p>
    </Dialog>}
  </div>;
}
