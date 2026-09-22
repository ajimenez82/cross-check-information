import { createContext, useContext, useRef, useState, type ReactNode } from 'react';
import { isAnalysis, type Analysis } from '../analysis/api/analysisContract';
import { startAnalysis, AnalysisRequestError, type AnalysisFailure } from '../analysis/api/startAnalysis';

export interface Turn {
  id: string; text: string; createdAt: string; status: 'pending' | 'done' | 'error';
  analysis?: Analysis; error?: AnalysisFailure;
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
  start: (text: string) => string;
  send: (id: string, text: string, retryId?: string) => Promise<void>;
  clear: () => void;
}
const Context = createContext<Store | null>(null);
export function ConversationProvider({ children }: { children: ReactNode }) {
  const [initial] = useState(load);
  const [conversations, setConversations] = useState(initial.conversations);
  const [notice, setNotice] = useState(initial.notice);
  const current = useRef(conversations);
  const pending = useRef(new Set<string>());
  function update(next: Conversation[]) {
    current.current = next;
    setConversations(next);
    try { localStorage.setItem(storageKey, JSON.stringify(next)); }
    catch { setNotice('No se ha podido guardar el historial. Los cambios se conservan solo mientras esta página siga abierta.'); }
  }
  function patch(id: string, change: (item: Conversation) => Conversation) {
    update(current.current.map(item => item.id === id ? change(item) : item));
  }
  async function send(id: string, text: string, retryId?: string) {
    const conversation = current.current.find(item => item.id === id);
    if (!conversation || pending.current.has(id) || !text.trim()) return;
    pending.current.add(id);
    const turn: Turn = { id: retryId ?? crypto.randomUUID(), text, createdAt: new Date().toISOString(), status: 'pending' };
    patch(id, item => ({ ...item, turns: retryId ? item.turns.map(old => old.id === retryId ? turn : old) : [...item.turns, turn].slice(-100) }));
    try {
      const result = await startAnalysis(text, conversation.token);
      patch(id, item => ({ ...item, token: result.conversationToken,
        turns: item.turns.map(old => old.id === turn.id ? { ...turn, status: 'done', analysis: result.analysis } : old) }));
    } catch (error) {
      const failure = error instanceof AnalysisRequestError ? error.failure : interrupted;
      patch(id, item => ({ ...item, turns: item.turns.map(old => old.id === turn.id ? { ...turn, status: 'error', error: failure } : old) }));
    } finally { pending.current.delete(id); }
  }
  function start(text: string) {
    const id = crypto.randomUUID();
    update([{ id, title: text.trim().slice(0, 70), token: null, turns: [] }, ...current.current].slice(0, 50));
    void send(id, text);
    return id;
  }
  return <Context.Provider value={{ conversations, notice, start, send, clear: () => update([]) }}>{children}</Context.Provider>;
}
export function useConversations() {
  const store = useContext(Context);
  if (!store) throw new Error('ConversationProvider is required');
  return store;
}
