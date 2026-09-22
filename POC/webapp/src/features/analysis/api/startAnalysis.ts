import { isAnalysisResponse, type AnalysisResponse } from './analysisContract';

export interface AnalysisFailure {
  code: string; message: string; requestId?: string; uncertain: boolean; resetRequired: boolean;
}
export class AnalysisRequestError extends Error {
  constructor(public readonly failure: AnalysisFailure) { super(failure.message); }
}
const messages: Record<string, string> = {
  INVALID_ANALYSIS_INPUT: 'La consulta no es válida o supera el límite permitido.',
  INVALID_REQUEST: 'El servicio no ha aceptado el formato de la solicitud.',
  INVALID_CONVERSATION_REFERENCE: 'La referencia de conversación ya no es válida. Inicia una nueva conversación.',
  CONVERSATION_REFERENCE_EXPIRED: 'La conversación ha caducado. Inicia una nueva conversación.',
  CONVERSATION_UNAVAILABLE: 'La conversación ya no está disponible. Inicia una nueva conversación.',
  ANALYSIS_CONFLICT: 'Hay una operación en curso en esta conversación.',
  ANALYSIS_PROVIDER_UNAVAILABLE: 'El servicio de análisis no está disponible temporalmente.',
  ANALYSIS_TIMEOUT: 'El servicio ha agotado el tiempo de espera.',
  INVALID_ANALYSIS_OUTPUT: 'El servicio ha devuelto un resultado que no se puede mostrar.',
  INTERNAL_ERROR: 'El servicio no ha podido completar el análisis.',
};
export async function startAnalysis(text: string, conversationToken: string | null): Promise<AnalysisResponse> {
  const controller = new AbortController();
  const configuredTimeout = Number(import.meta.env.VITE_ANALYSIS_TIMEOUT_MS);
  const timeout = Number.isFinite(configuredTimeout) && configuredTimeout > 0 ? configuredTimeout : 120000;
  const timer = window.setTimeout(() => controller.abort(), timeout);
  try {
    const response = await fetch('/api/analysis/start', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ text, conversationToken }), signal: controller.signal, cache: 'no-store',
    });
    const payload: unknown = await response.json().catch(() => null);
    if (!response.ok) {
      const data = payload && typeof payload === 'object' ? payload as Record<string, unknown> : {};
      const code = typeof data.code === 'string' ? data.code : 'HTTP_ERROR';
      const requestId = typeof data.requestId === 'string' ? data.requestId : response.headers.get('X-Request-Id') ?? undefined;
      throw new AnalysisRequestError({
        code, requestId,
        message: messages[code] ?? (response.status === 404 ? 'La API de análisis no está disponible. Comprueba que el servicio esté iniciado con el perfil dev.' : 'No se ha podido contactar correctamente con el servicio de análisis.'),
        uncertain: data.executionState !== 'NOT_STARTED',
        resetRequired: ['INVALID_CONVERSATION_REFERENCE', 'CONVERSATION_REFERENCE_EXPIRED', 'CONVERSATION_UNAVAILABLE'].includes(code),
      });
    }
    if (!isAnalysisResponse(payload)) throw new AnalysisRequestError({
      code: 'INVALID_RESPONSE', message: 'La respuesta recibida no cumple el contrato del análisis.',
      uncertain: true, resetRequired: false,
    });
    return payload;
  } catch (error) {
    if (error instanceof AnalysisRequestError) throw error;
    throw new AnalysisRequestError({
      code: controller.signal.aborted ? 'CLIENT_TIMEOUT' : 'NETWORK_ERROR',
      message: controller.signal.aborted ? 'Se ha agotado el tiempo de espera del navegador.' : 'No se ha podido conectar con el servicio. Comprueba que esté arrancado.',
      uncertain: true, resetRequired: false,
    });
  } finally { window.clearTimeout(timer); }
}
