export const verdictLabels = {
  SUPPORTED: 'Respaldada', REFUTED: 'Refutada', MISLEADING: 'Engañosa o fuera de contexto',
  INSUFFICIENT_EVIDENCE: 'Evidencia insuficiente', OPINION: 'Interpretación u opinión',
  NO_SINGLE_VERDICT: 'Sin veredicto único',
} as const;
export const positionLabels = {
  QUESTIONS: 'Cuestiona', SUPPORTS: 'Respalda', MIXED: 'Mixta', NO_EXPLICIT_POSITION: 'Sin posición explícita',
} as const;
export type Position = keyof typeof positionLabels;
export interface Source {
  id: string; title: string; url: string; publisher: string | null; publishedAt: string | null;
  consultedAt: string; contribution: string; type: string | null;
}
export interface Analysis {
  title: string; context: string; summary: string; summarySourceIds: string[];
  verdict: { status: keyof typeof verdictLabels; explanation: string; documentarySupport: string; sourceIds: string[] };
  sources: Source[];
  publicationPositions: {
    availability: 'AVAILABLE' | 'UNAVAILABLE'; reason: string | null; proposition: string | null;
    period: { from: string; to: string } | null; consultedAt: string | null; selectionCriteria: string | null;
    units: { id: string; sourceIds: string[]; position: Position; explanation: string }[];
    excluded: { sourceId: string; reason: string }[];
  };
  limitations: string[]; analyzedAt: string; asOf: string | null;
}
export interface AnalysisResponse { conversationToken: string; analysis: Analysis }
const object = (value: unknown): value is Record<string, unknown> => typeof value === 'object' && value !== null && !Array.isArray(value);
const text = (value: unknown): value is string => typeof value === 'string' && value.trim().length > 0;
const optionalText = (value: unknown) => value === null || text(value);
const texts = (value: unknown): value is string[] => Array.isArray(value) && value.every(text);
const date = (value: unknown) => text(value) && Number.isFinite(Date.parse(value));
const optionalDate = (value: unknown) => value === null || date(value);
export function safeSourceUrl(value: unknown): value is string {
  if (!text(value)) return false;
  try { const url = new URL(value); return ['http:', 'https:'].includes(url.protocol) && !url.username && !url.password; }
  catch { return false; }
}
export function isAnalysis(value: unknown): value is Analysis {
  if (!object(value) || !text(value.title) || !text(value.context) || !text(value.summary) ||
      !texts(value.summarySourceIds) || !texts(value.limitations) || !date(value.analyzedAt) || !optionalDate(value.asOf)) return false;
  const verdict = value.verdict;
  if (!object(verdict) || !text(verdict.status) || !Object.hasOwn(verdictLabels, verdict.status) ||
      !text(verdict.explanation) || !text(verdict.documentarySupport) || !texts(verdict.sourceIds)) return false;
  if (!Array.isArray(value.sources) || !value.sources.every(source => object(source) && text(source.id) &&
      text(source.title) && safeSourceUrl(source.url) && optionalText(source.publisher) && optionalDate(source.publishedAt) &&
      date(source.consultedAt) && text(source.contribution) && optionalText(source.type))) return false;
  const sourceIds = new Set(value.sources.map(source => source.id));
  if (sourceIds.size !== value.sources.length) return false;
  const references = (ids: unknown): ids is string[] => texts(ids) && new Set(ids).size === ids.length && ids.every(id => sourceIds.has(id));
  if (!references(value.summarySourceIds) || !references(verdict.sourceIds)) return false;
  const positions = value.publicationPositions;
  if (!object(positions) || !['AVAILABLE', 'UNAVAILABLE'].includes(String(positions.availability)) ||
      !optionalText(positions.reason) || !optionalText(positions.proposition) || !optionalText(positions.selectionCriteria) ||
      !optionalDate(positions.consultedAt) || !Array.isArray(positions.units) || !Array.isArray(positions.excluded)) return false;
  if (positions.period !== null && (!object(positions.period) || !date(positions.period.from) ||
      !date(positions.period.to) || Date.parse(String(positions.period.from)) > Date.parse(String(positions.period.to)))) return false;
  const unitIds = new Set<string>(); const classified = new Set<string>(); const excluded = new Set<string>();
  for (const unit of positions.units) {
    if (!object(unit) || !text(unit.id) || unitIds.has(unit.id) || !references(unit.sourceIds) || !unit.sourceIds.length ||
        !text(unit.position) || !Object.hasOwn(positionLabels, unit.position) || !text(unit.explanation)) return false;
    unitIds.add(unit.id);
    for (const id of unit.sourceIds) { if (classified.has(id)) return false; classified.add(id); }
  }
  for (const item of positions.excluded) {
    if (!object(item) || !text(item.sourceId) || !sourceIds.has(item.sourceId) || classified.has(item.sourceId) ||
        excluded.has(item.sourceId) || !text(item.reason)) return false;
    excluded.add(item.sourceId);
  }
  if (positions.availability === 'UNAVAILABLE') return positions.units.length === 0 && text(positions.reason);
  return text(positions.proposition) && text(positions.selectionCriteria) && date(positions.consultedAt) &&
    (positions.units.length > 0 || text(positions.reason));
}
export function isAnalysisResponse(value: unknown): value is AnalysisResponse {
  return object(value) && text(value.conversationToken) && isAnalysis(value.analysis);
}
