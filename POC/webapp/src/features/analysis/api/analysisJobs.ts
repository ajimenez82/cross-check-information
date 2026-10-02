import { isAnalysisResponse, type AnalysisResponse } from './analysisContract';
import { AnalysisRequestError } from './startAnalysis';

export type JobStatus = 'QUEUED' | 'SUBMITTING' | 'RUNNING' | 'RECOVERING' | 'COMPLETED' | 'FAILED';
export interface PendingJob {
  key: string;
  accessToken: string;
  body: { text: string; conversationToken: string | null };
  analysisId?: string;
  expiresAt: string;
  status?: JobStatus;
  connectionLost?: boolean;
}
export interface JobView {
  analysisId: string; status: JobStatus; createdAt: string; updatedAt: string;
  completedAt: string | null; expiresAt: string; pollAfterSeconds: number | null;
  result: AnalysisResponse | null;
  error: { code: string; message: string; requestId: string | null; executionState: 'NOT_STARTED' | 'CONFIRMED' | 'UNKNOWN' } | null;
}
const uuid = /^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/i;
const object = (value: unknown): value is Record<string, unknown> => !!value && typeof value === 'object' && !Array.isArray(value);
const date = (value: unknown): value is string => typeof value === 'string' && Number.isFinite(Date.parse(value));
const statuses: JobStatus[] = ['QUEUED', 'SUBMITTING', 'RUNNING', 'RECOVERING', 'COMPLETED', 'FAILED'];
export function isPendingJob(value: unknown): value is PendingJob {
  if (!object(value) || typeof value.key !== 'string' || !uuid.test(value.key) ||
      typeof value.accessToken !== 'string' || !/^[A-Za-z0-9_-]{42}[AEIMQUYcgkosw048]$/.test(value.accessToken) ||
      !object(value.body) || typeof value.body.text !== 'string' ||
      !(value.body.conversationToken === null || typeof value.body.conversationToken === 'string') || !date(value.expiresAt)) return false;
  return (value.analysisId === undefined || typeof value.analysisId === 'string' && uuid.test(value.analysisId)) &&
    (value.status === undefined || statuses.includes(value.status as JobStatus)) &&
    (value.connectionLost === undefined || typeof value.connectionLost === 'boolean');
}
export function createJob(text: string, conversationToken: string | null): PendingJob {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return {
    key: crypto.randomUUID(), accessToken: btoa(String.fromCharCode(...bytes)).replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, ''),
    body: { text, conversationToken }, expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
  };
}
function isJobView(value: unknown): value is JobView {
  if (!object(value) || typeof value.analysisId !== 'string' || !uuid.test(value.analysisId) ||
      !statuses.includes(value.status as JobStatus) || !date(value.createdAt) || !date(value.updatedAt) || !date(value.expiresAt)) return false;
  if (value.status === 'COMPLETED') return date(value.completedAt) && value.pollAfterSeconds === null && value.error === null && isAnalysisResponse(value.result);
  if (value.status === 'FAILED') {
    const error = value.error;
    return date(value.completedAt) && value.pollAfterSeconds === null && value.result === null && object(error) &&
      typeof error.code === 'string' && typeof error.message === 'string' &&
      (error.requestId === null || typeof error.requestId === 'string') &&
      ['NOT_STARTED', 'CONFIRMED', 'UNKNOWN'].includes(error.executionState as string);
  }
  return value.completedAt === null && value.result === null && value.error === null &&
    typeof value.pollAfterSeconds === 'number' && Number.isInteger(value.pollAfterSeconds) && value.pollAfterSeconds > 0;
}
export class JobRequestError extends AnalysisRequestError {
  constructor(code: string, message: string, public readonly temporary: boolean, public readonly retryAfterSeconds = 0, requestId?: string) {
    super({ code, message, requestId, uncertain: true, resetRequired: false });
  }
}
export async function requestJob(job: PendingJob, signal: AbortSignal): Promise<JobView> {
  const controller = new AbortController();
  const abort = () => controller.abort();
  signal.addEventListener('abort', abort, { once: true });
  if (signal.aborted) controller.abort();
  const timer = window.setTimeout(abort, 15000);
  try {
    const response = await fetch(job.analysisId ? `/api/analysis/jobs/${job.analysisId}` : '/api/analysis/jobs', {
      method: job.analysisId ? 'GET' : 'POST', cache: 'no-store', signal: controller.signal,
      headers: { Accept: 'application/json', Authorization: `Bearer ${job.accessToken}`,
        ...(!job.analysisId ? { 'Content-Type': 'application/json', 'Idempotency-Key': job.key } : {}) },
      ...(!job.analysisId ? { body: JSON.stringify(job.body) } : {}),
    });
    const payload: unknown = await response.json().catch(error => { if (controller.signal.aborted) throw error; return null; });
    const requestId = response.headers.get('X-Request-Id') ?? undefined;
    if (!response.ok) {
      const code = object(payload) && typeof payload.code === 'string' ? payload.code : 'HTTP_ERROR';
      const messages: Record<string, string> = {
        ANALYSIS_JOB_EXPIRED: 'El plazo para recuperar este análisis ha caducado.',
        ANALYSIS_JOB_NOT_FOUND: 'No se encuentra el análisis o su acceso ya no está disponible.',
        INVALID_JOB_CREDENTIAL: 'No se puede acceder a este análisis con la credencial guardada.',
        IDEMPOTENCY_CONFLICT: 'La solicitud guardada no coincide con la aceptada por el servicio.',
        ANALYSIS_CONFLICT: 'Hay un análisis en curso o el contexto de esta conversación ha quedado desactualizado.',
        CONVERSATION_REFERENCE_EXPIRED: 'La conversación ha caducado. Inicia una nueva conversación.',
      };
      const retryHeader = response.headers.get('Retry-After');
      const seconds = retryHeader && /^\d+$/.test(retryHeader) ? Number(retryHeader)
        : retryHeader ? Math.max(0, (Date.parse(retryHeader) - Date.now()) / 1000) : 0;
      throw new JobRequestError(code, messages[code] ?? 'El servicio no ha podido recuperar la solicitud.',
        response.status === 408 || response.status === 429 || response.status >= 500,
        Number.isFinite(seconds) ? Math.min(seconds, 3600) : 0, requestId);
    }
    if (!isJobView(payload) || job.analysisId && payload.analysisId !== job.analysisId)
      throw new JobRequestError('INVALID_RESPONSE', 'La respuesta no cumple el contrato del análisis. Puedes volver a consultar el mismo trabajo.', false, 0, requestId);
    return payload;
  } catch (error) {
    if (error instanceof JobRequestError) throw error;
    throw new JobRequestError(controller.signal.aborted ? 'CLIENT_TIMEOUT' : 'NETWORK_ERROR', 'Se ha perdido la conexión. Se recuperará el mismo análisis.', true);
  } finally {
    window.clearTimeout(timer);
    signal.removeEventListener('abort', abort);
  }
}
