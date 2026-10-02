export interface AnalysisFailure {
  code: string; message: string; requestId?: string; uncertain: boolean; resetRequired: boolean;
}
export class AnalysisRequestError extends Error {
  constructor(public readonly failure: AnalysisFailure) { super(failure.message); }
}
const failureMessages: Record<string, string> = {
  INVALID_ANALYSIS_OUTPUT: 'El servicio ha devuelto un resultado que no se puede mostrar.',
  ANALYSIS_QUEUE_EXPIRED: 'La consulta ha superado el tiempo máximo en cola sin comenzar.',
  ANALYSIS_DEADLINE_EXCEEDED: 'El análisis ha superado el plazo de ejecución del servicio.',
  ANALYSIS_SUBMISSION_UNKNOWN: 'El servicio no ha podido confirmar el estado del envío.',
  ANALYSIS_REVISION_UNAVAILABLE: 'La versión del agente asociada a esta consulta ya no está disponible.',
  ANALYSIS_PROVIDER_FAILED: 'El proveedor no ha podido completar el análisis.',
};
export const failureMessage = (code: string, fallback: string) => failureMessages[code] ?? fallback;
