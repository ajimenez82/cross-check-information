import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { isAnalysis, isClarification, type Analysis, type Clarification } from '../analysis/api/analysisContract';
import { failureMessage, type AnalysisFailure } from '../analysis/api/startAnalysis';

import { createJob, isPendingJob, requestJob, JobRequestError, type PendingJob } from '../analysis/api/analysisJobs';

export interface Turn {
  id: string; text: string; createdAt: string; status: 'pending' | 'done' | 'error' | 'clarification';
  analysis?: Analysis; clarification?: Clarification; error?: AnalysisFailure; job?: PendingJob;
}
export interface Conversation { id: string; title: string; token: string | null; turns: Turn[] }
const storageKey = 'crosscheck.conversations.v1';
const interrupted: AnalysisFailure = {
  code: 'INTERRUPTED', message: 'La página se cerró antes de recibir el resultado.',
  uncertain: true, resetRequired: false,
};
function load(): { conversations: Conversation[]; notice: string } {
  try {
    const raw = localStorage.getItem(storageKey);
    if (!raw) return { conversations: [], notice: '' };
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed) || parsed.length > 50) throw new Error();
    const conversations = parsed.map((value: unknown): Conversation => {
      const item = value as Conversation;
      if (!item || typeof item.id !== 'string' || !/^[a-f0-9-]{36}$/.test(item.id) ||
          typeof item.title !== 'string' || !(item.token === null || typeof item.token === 'string') ||
          !Array.isArray(item.turns) || item.turns.length > 100) throw new Error();
      const turns = item.turns.map((turn: Turn): Turn => {
        if (!turn || typeof turn.id !== 'string' || typeof turn.text !== 'string' ||
            !Number.isFinite(Date.parse(turn.createdAt))) throw new Error();
        if (turn.status === 'done' && isAnalysis(turn.analysis)) return { ...turn, error: undefined };
        if (turn.status === 'clarification' && isClarification(turn.clarification))
          return { ...turn, analysis: undefined, error: undefined };
        if (turn.job !== undefined && (!isPendingJob(turn.job) || turn.job.body.text !== turn.text)) throw new Error();
        if (turn.status === 'pending' && turn.job) return turn;
        if (turn.status === 'pending') return { ...turn, status: 'error', analysis: undefined, error: interrupted };
        if (turn.status === 'error' && turn.error && typeof turn.error.code === 'string' &&
            typeof turn.error.message === 'string' && typeof turn.error.uncertain === 'boolean' &&
            typeof turn.error.resetRequired === 'boolean' &&
            (turn.error.requestId === undefined || typeof turn.error.requestId === 'string')) {
          return { ...turn, analysis: undefined };
        }
        throw new Error();
      });
      if (new Set(turns.map(turn => turn.id)).size !== turns.length) throw new Error();
      return { id: item.id, title: item.title, token: item.token, turns };
    });
    if (new Set(conversations.map(item => item.id)).size !== conversations.length) throw new Error();
    return { conversations, notice: '' };
  } catch {
    return { conversations: [], notice: 'No se ha podido recuperar el historial local. Puedes continuar con una nueva conversación.' };
  }
}
interface Store {
  conversations: Conversation[]; notice: string;
  start: (text: string) => Promise<string | null>;
  send: (id: string, text: string, retryId?: string) => Promise<void>;
  clear: () => void;
  recover: (id: string, turnId: string) => void;
}
const Context = createContext<Store | null>(null);
export function ConversationProvider({ children }: { children: ReactNode }) {
  const [initial] = useState(load);
  const [conversations, setConversations] = useState(initial.conversations);
  const [notice, setNotice] = useState(initial.notice);
  const current = useRef(conversations);
  const active = useRef(new Map<string, AbortController>());
  const nextPoll = useRef(new Map<string, number>());
  const failures = useRef(new Map<string, number>());
  const mounted = useRef(false);

  // Every tab merges against the latest persisted history under the same lock.
  async function mutate(change: (items: Conversation[]) => Conversation[]): Promise<boolean> {
    if (!navigator.locks) {
      setNotice('Este navegador no permite guardar y recuperar las solicitudes con seguridad. Usa una versión actual de Edge, Chrome o Firefox en localhost o HTTPS.');
      return false;
    }
    return navigator.locks.request(storageKey, async () => {
      try {
        const saved = load();
        if (saved.notice) throw new Error('Unreadable history');
        const next = change(saved.conversations);
        localStorage.setItem(storageKey, JSON.stringify(next));
        current.current = next;
        if (mounted.current) setConversations(next);
        return true;
      } catch {
        if (mounted.current) setNotice('No se ha podido guardar el historial. No se enviarán nuevas consultas hasta que haya almacenamiento disponible. Los trabajos ya aceptados pueden continuar en el servicio.');
        return false;
      }
    });
  }
  async function patchTurn(id: string, turnId: string, key: string, change: (turn: Turn, item: Conversation) => Conversation) {
    return mutate(items => items.map(item => {
      const turn = item.turns.find(value => value.id === turnId);
      return item.id === id && turn?.job?.key === key && turn.status === 'pending' ? change(turn, item) : item;
    }));
  }
  function replaceTurn(item: Conversation, turn: Turn): Conversation {
    return { ...item, turns: item.turns.map(old => old.id === turn.id ? turn : old) };
  }
  async function poll(id: string, turnId: string, key: string, signal: AbortSignal) {
    // A second tab can observe progress, but only one tab requests a job at a time.
    await navigator.locks.request(`crosscheck.job.${key}`, { ifAvailable: true }, async lock => {
      if (!lock || signal.aborted) return;
      const turn = load().conversations.find(item => item.id === id)?.turns.find(item => item.id === turnId);
      if (!turn?.job || turn.status !== 'pending' || turn.job.key !== key) return;
      const job = turn.job;
      if (Date.now() >= Date.parse(job.expiresAt)) {
        await patchTurn(id, turnId, key, (old, item) => replaceTurn(item, { ...old, status: 'error',
          error: { code: 'ANALYSIS_JOB_EXPIRED', message: 'Ha caducado el plazo de recuperación. No se ha enviado una nueva solicitud.', uncertain: true, resetRequired: true } }));
        return;
      }
      try {
        const view = await requestJob(job, signal);
        if (signal.aborted) return;
        failures.current.delete(key);
        nextPoll.current.set(key, Date.now() + Math.min(15, view.pollAfterSeconds ?? 3) * 1000);
        await patchTurn(id, turnId, key, (old, item) => {
          const updated = { ...old, job: { ...job, analysisId: view.analysisId, status: view.status, expiresAt: view.expiresAt, connectionLost: false } };
          if (view.status === 'COMPLETED' && view.result) {
            const result = view.result;
            return { ...replaceTurn(item, result.analysis
              ? { ...updated, job: undefined, status: 'done', analysis: result.analysis, error: undefined }
              : { ...updated, job: undefined, status: 'clarification', clarification: result.clarification, error: undefined }), token: result.conversationToken };
          }
          if (view.status === 'FAILED' && view.error) return replaceTurn(item, { ...updated, status: 'error', error: {
            code: view.error.code, message: failureMessage(view.error.code, view.error.message), requestId: view.error.requestId ?? undefined,
            uncertain: view.error.executionState !== 'NOT_STARTED', resetRequired: true,
          } });
          return replaceTurn(item, updated);
        });
      } catch (error) {
        if (signal.aborted) return;
        const failure = error instanceof JobRequestError ? error : new JobRequestError('NETWORK_ERROR', 'No se puede consultar el estado.', true);
        const count = (failures.current.get(key) ?? 0) + 1;
        failures.current.set(key, count);
        nextPoll.current.set(key, Date.now() + Math.max(Math.min(15, 3 * 2 ** Math.min(count - 1, 3)), failure.retryAfterSeconds) * 1000);
        await patchTurn(id, turnId, key, (old, item) => replaceTurn(item, failure.temporary
          ? { ...old, job: { ...old.job!, connectionLost: true } }
          : { ...old, status: 'error', error: { ...failure.failure, resetRequired: failure.failure.code !== 'INVALID_RESPONSE' } }));
      }
    });
  }
  useEffect(() => {
    mounted.current = true;
    const refresh = () => {
      const saved = load();
      current.current = saved.conversations;
      setConversations(saved.conversations);
      if (saved.notice) setNotice(saved.notice);
      for (const [key, controller] of active.current) {
        if (!saved.conversations.some(item => item.turns.some(turn => turn.status === 'pending' && turn.job?.key === key))) controller.abort();
      }
    };
    const tick = () => {
      if (document.hidden || !navigator.onLine || !navigator.locks) return;
      for (const item of current.current) for (const turn of item.turns) {
        const key = turn.job?.key;
        if (turn.status !== 'pending' || !key || active.current.has(key) || Date.now() < (nextPoll.current.get(key) ?? 0)) continue;
        const controller = new AbortController();
        active.current.set(key, controller);
        void poll(item.id, turn.id, key, controller.signal).finally(() => {
          if (active.current.get(key) === controller) active.current.delete(key);
        });
      }
    };
    const storage = (event: StorageEvent) => { if (event.key === storageKey || event.key === null) refresh(); };
    const resume = () => { tick(); };
    window.addEventListener('storage', storage);
    window.addEventListener('online', resume);
    document.addEventListener('visibilitychange', resume);
    const interval = window.setInterval(tick, 1000);
    tick();
    return () => {
      mounted.current = false;
      window.clearInterval(interval);
      window.removeEventListener('storage', storage);
      window.removeEventListener('online', resume);
      document.removeEventListener('visibilitychange', resume);
      for (const controller of active.current.values()) controller.abort();
      active.current.clear();
    };
  }, []);
  async function send(id: string, text: string, retryId?: string) {
    if (!text.trim()) return;
    await mutate(items => items.map(item => {
      if (item.id !== id || item.turns.some(turn => turn.status === 'pending')) return item;
      if (item.turns.length >= 100) { setNotice('Esta conversación ha alcanzado 100 turnos. Inicia una nueva conversación.'); return item; }
      // Retries of legacy synchronous failures are explicit new submissions.
      const previous = item.turns.find(turn => turn.id === retryId);
      if (retryId && (!previous || previous.job)) return item;
      const turn: Turn = { id: retryId ?? crypto.randomUUID(), text, createdAt: new Date().toISOString(), status: 'pending', job: createJob(text, item.token) };
      return { ...item, turns: retryId ? item.turns.map(old => old.id === retryId ? turn : old) : [...item.turns, turn] };
    }));
  }
  async function start(text: string) {
    if (!text.trim()) return null;
    const id = crypto.randomUUID();
    let created = false;
    const saved = await mutate(items => {
      // Never evict an active job to make room in the history.
      if (items.length >= 50) {
        setNotice('El historial ha alcanzado 50 conversaciones. Guarda lo que necesites y borra el historial antes de crear otra.');
        return items;
      }
      created = true;
      return [{ id, title: text.trim().slice(0, 70), token: null,
        turns: [{ id: crypto.randomUUID(), text, createdAt: new Date().toISOString(), status: 'pending', job: createJob(text, null) }] }, ...items];
    });
    return saved && created ? id : null;
  }
  function recover(id: string, turnId: string) {
    void mutate(items => items.map(item => ({ ...item, turns: item.turns.map(turn => item.id === id && turn.id === turnId && turn.job && turn.error?.code === 'INVALID_RESPONSE'
      ? { ...turn, status: 'pending', error: undefined } : turn) })));
  }
  function clear() {
    if (!navigator.locks) { setNotice('Este navegador no admite el acceso seguro al historial.'); return; }
    for (const controller of active.current.values()) controller.abort();
    void navigator.locks.request(storageKey, async () => {
      try { localStorage.removeItem(storageKey); current.current = []; setConversations([]); setNotice(''); }
      catch { setNotice('No se ha podido borrar el historial local.'); }
    });
  }
  return <Context.Provider value={{ conversations, notice, start, send, clear, recover }}>{children}</Context.Provider>;
}
export function useConversations() {
  const store = useContext(Context);
  if (!store) throw new Error('ConversationProvider is required');
  return store;
}
