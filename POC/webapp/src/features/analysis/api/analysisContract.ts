export const verdictLabels = {
  SUPPORTED: 'Respaldada', REFUTED: 'Refutada', MISLEADING: 'Engañosa o fuera de contexto',
  INSUFFICIENT_EVIDENCE: 'Evidencia insuficiente', OPINION: 'Interpretación u opinión',
  NO_SINGLE_VERDICT: 'Sin veredicto único',
  SUPPORTED_BY_PUBLICATIONS: 'Respaldada por las publicaciones',
  QUESTIONED_BY_PUBLICATIONS: 'Cuestionada por las publicaciones',
} as const;
export const positionLabels = {
  QUESTIONS: 'Cuestiona', SUPPORTS: 'Respalda', MIXED: 'Mixta', NO_EXPLICIT_POSITION: 'Sin posición explícita',
} as const;
export type Position = keyof typeof positionLabels;
export interface PublicationTrace {
  stanceOwner: 'AUTHOR' | 'THIRD_PARTY' | 'NONE' | 'UNDETERMINED';
  stanceHolder: string | null;
  scope: { outcome: string; geography: string; studyPeriod: string | null; policy: string | null;
    match: 'MATCH' | 'PARTIAL' | 'OUT_OF_SCOPE' | 'UNCERTAIN'; explanation: string };
  temporalRelation: 'REQUESTED_PERIOD' | 'RETROSPECTIVE' | 'BACKGROUND' | 'UNKNOWN';
  arguments: { sourceId: string; locator: string | null; paraphrase: string;
    attribution: 'AUTHOR' | 'THIRD_PARTY' | 'DESCRIPTIVE'; relation: 'SUPPORTS' | 'QUESTIONS' | 'CONTEXT' }[];
}
export interface Source {
  id: string; title: string; url: string; publisher: string | null; publishedAt: string | null;
  consultedAt: string | null; contribution: string; type: string | null;
}
export interface Analysis {
  claimAnalysis?: {
    analysisTarget: string;
    decomposition: { kind: 'SINGLE' | 'MULTIPLE'; basis: 'SINGLE_PROPOSITION' | 'EXPLICIT_CONJUNCTION' | 'EXPLICIT_DIMENSIONS'; explanation: string };
    claims: { id: string; inputExcerpt: string; proposition: string;
      verdict: { status: 'SUPPORTED' | 'REFUTED' | 'MISLEADING' | 'INSUFFICIENT_EVIDENCE' | 'OPINION'; explanation: string; documentarySupport: string; sourceIds: string[] } }[];
  } | null;
  title: string; context: string; summary: string; summarySourceIds: string[];
  verdict: { status: keyof typeof verdictLabels; explanation: string; documentarySupport: string; sourceIds: string[] };
  sources: Source[];
  publicationPositions: {
    availability: 'AVAILABLE' | 'UNAVAILABLE'; reason: string | null; proposition: string | null;
    period: { from: string; to: string } | null; consultedAt: string | null; selectionCriteria: string | null;
    units: { id: string; sourceIds: string[]; position: Position; explanation: string; trace?: PublicationTrace | null }[];
    excluded: { sourceId: string; reason: string; trace?: PublicationTrace | null }[];
  };
  limitations: string[]; analyzedAt: string; asOf: string | null;
}
export interface Clarification { question: string; reason: 'MISSING_PERIOD' | 'MISSING_SCOPE' | 'AMBIGUOUS_REFERENCE' | 'OTHER' | 'CONTEXT_UNAVAILABLE' }
export type AnalysisResponse = { conversationToken: string; analysis: Analysis; clarification?: null }
  | { conversationToken: string | null; analysis: null; clarification: Clarification };
const object = (value: unknown): value is Record<string, unknown> => typeof value === 'object' && value !== null && !Array.isArray(value);
const text = (value: unknown): value is string => typeof value === 'string' && value.trim().length > 0;
const optionalText = (value: unknown) => value === null || text(value);
const texts = (value: unknown): value is string[] => Array.isArray(value) && value.every(text);
const date = (value: unknown) => text(value) && Number.isFinite(Date.parse(value));
const optionalDate = (value: unknown) => value === null || date(value);
function validTrace(value: unknown, allowedIds: Set<string>): boolean {
  // Older saved reports have no trace field; absence is not inferred evidence.
  if (value === undefined || value === null) return true;
  if (!object(value) || !text(value.stanceOwner) ||
      !['AUTHOR', 'THIRD_PARTY', 'NONE', 'UNDETERMINED'].includes(value.stanceOwner) ||
      !optionalText(value.stanceHolder) || !text(value.temporalRelation) ||
      !['REQUESTED_PERIOD', 'RETROSPECTIVE', 'BACKGROUND', 'UNKNOWN'].includes(value.temporalRelation)) return false;
  const scope = value.scope;
  if (!object(scope) || !text(scope.outcome) || !text(scope.geography) || !optionalText(scope.studyPeriod) ||
      !optionalText(scope.policy) || !text(scope.explanation) || !text(scope.match) ||
      !['MATCH', 'PARTIAL', 'OUT_OF_SCOPE', 'UNCERTAIN'].includes(scope.match)) return false;
  return Array.isArray(value.arguments) && value.arguments.length > 0 && value.arguments.every(argument =>
    object(argument) && text(argument.sourceId) && allowedIds.has(argument.sourceId) && optionalText(argument.locator) &&
    text(argument.paraphrase) && text(argument.attribution) && ['AUTHOR', 'THIRD_PARTY', 'DESCRIPTIVE'].includes(argument.attribution) &&
    text(argument.relation) && ['SUPPORTS', 'QUESTIONS', 'CONTEXT'].includes(argument.relation));
}
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
      optionalDate(source.consultedAt) && text(source.contribution) && optionalText(source.type))) return false;
  const sourceIds = new Set(value.sources.map(source => source.id));
  if (sourceIds.size !== value.sources.length) return false;
  const references = (ids: unknown): ids is string[] => texts(ids) && new Set(ids).size === ids.length && ids.every(id => sourceIds.has(id));
  if (!references(value.summarySourceIds) || !references(verdict.sourceIds)) return false;
  const claims = value.claimAnalysis;
  if (claims !== undefined && claims !== null) {
    if (!object(claims) || !text(claims.analysisTarget) || !object(claims.decomposition) ||
        !text(claims.decomposition.explanation) || !Array.isArray(claims.claims) ||
        claims.claims.length < 1 || claims.claims.length > 8) return false;
    const single = claims.decomposition.kind === 'SINGLE';
    if (single ? claims.claims.length !== 1 || claims.decomposition.basis !== 'SINGLE_PROPOSITION'
      : claims.decomposition.kind !== 'MULTIPLE' || claims.claims.length < 2 ||
        !['EXPLICIT_CONJUNCTION', 'EXPLICIT_DIMENSIONS'].includes(String(claims.decomposition.basis))) return false;
    const ids = new Set<string>(); const statuses = new Set<string>();
    for (const claim of claims.claims) {
      if (!object(claim) || !text(claim.id) || ids.has(claim.id) || !text(claim.inputExcerpt) || !text(claim.proposition) ||
          !object(claim.verdict) || !text(claim.verdict.status) ||
          !['SUPPORTED', 'REFUTED', 'MISLEADING', 'INSUFFICIENT_EVIDENCE', 'OPINION'].includes(claim.verdict.status) ||
          !text(claim.verdict.explanation) || !text(claim.verdict.documentarySupport) || !references(claim.verdict.sourceIds)) return false;
      ids.add(claim.id); statuses.add(claim.verdict.status);
    }
    const expected = statuses.size > 1 ? 'NO_SINGLE_VERDICT' : [...statuses][0];
    if (verdict.status !== expected && !(single && expected === 'INSUFFICIENT_EVIDENCE' &&
        ['SUPPORTED_BY_PUBLICATIONS', 'QUESTIONED_BY_PUBLICATIONS'].includes(verdict.status))) return false;
  }
  const positions = value.publicationPositions;
  if (!object(positions) || !['AVAILABLE', 'UNAVAILABLE'].includes(String(positions.availability)) ||
      !optionalText(positions.reason) || !optionalText(positions.proposition) || !optionalText(positions.selectionCriteria) ||
      !optionalDate(positions.consultedAt) || !Array.isArray(positions.units) || !Array.isArray(positions.excluded)) return false;
  if (object(claims) && object(claims.decomposition) && claims.decomposition.kind === 'MULTIPLE' &&
      (positions.availability !== 'UNAVAILABLE' || positions.units.length || positions.excluded.length)) return false;
  if (positions.period !== null && (!object(positions.period) || !date(positions.period.from) ||
      !date(positions.period.to) || Date.parse(String(positions.period.from)) > Date.parse(String(positions.period.to)))) return false;
  const unitIds = new Set<string>(); const classified = new Set<string>(); const excluded = new Set<string>();
  for (const unit of positions.units) {
    if (!object(unit) || !text(unit.id) || unitIds.has(unit.id) || !references(unit.sourceIds) || !unit.sourceIds.length ||
        !text(unit.position) || !Object.hasOwn(positionLabels, unit.position) || !text(unit.explanation)) return false;
    unitIds.add(unit.id);
    if (!validTrace(unit.trace, new Set(unit.sourceIds))) return false;
    for (const id of unit.sourceIds) { if (classified.has(id)) return false; classified.add(id); }
  }
  for (const item of positions.excluded) {
    if (!object(item) || !text(item.sourceId) || !sourceIds.has(item.sourceId) || classified.has(item.sourceId) ||
        excluded.has(item.sourceId) || !text(item.reason)) return false;
    excluded.add(item.sourceId);
    if (!validTrace(item.trace, sourceIds)) return false;
  }
  if (positions.availability === 'UNAVAILABLE') return positions.units.length === 0 && text(positions.reason);
  return text(positions.proposition) && text(positions.selectionCriteria) &&
    (positions.units.length > 0 || text(positions.reason));
}
export function isAnalysisResponse(value: unknown): value is AnalysisResponse {
  if (!object(value)) return false;
  if (isAnalysis(value.analysis)) return text(value.conversationToken) && (value.clarification === null || value.clarification === undefined);
  return value.analysis === null && isClarification(value.clarification) &&
    (value.clarification.reason === 'CONTEXT_UNAVAILABLE' ? value.conversationToken === null : text(value.conversationToken));
}
export function isClarification(value: unknown): value is Clarification {
  return object(value) && text(value.question) && value.question.length <= 2000 &&
    ['MISSING_PERIOD', 'MISSING_SCOPE', 'AMBIGUOUS_REFERENCE', 'OTHER', 'CONTEXT_UNAVAILABLE'].includes(String(value.reason));
}
