package com.crosscheck.application.features.analysis.start;

import com.crosscheck.application.features.analysis.common.dto.*;
import com.crosscheck.domain.analysis.AnalysisReport;
import java.time.Instant;

final class AnalysisResultMapper {
    private AnalysisResultMapper() {}

    static AnalysisResult map(AnalysisReport report, Instant analyzedAt) {
        var claims = report.claimAnalysis();
        var verdict = claims != null && claims.claims().size() > 1 ? report.verdict()
                : FinalVerdictPolicy.resolve(report.verdict(), report.publicationPositions());
        var positions = report.publicationPositions();
        var period = positions.period();
        return new AnalysisResult(report.title(), report.context(), report.summary(), report.summarySourceIds(),
                new VerdictResult(verdict.status().name(), verdict.explanation(),
                        verdict.documentarySupport(), verdict.sourceIds()),
                report.sources().stream().map(source -> new SourceResult(source.id(), source.title(),
                        source.url().toString(), source.publisher(), source.publishedAt(),
                        source.consultedAt(), source.contribution(), source.type())).toList(),
                new PublicationPositionsResult(positions.availability().name(), positions.reason(),
                        positions.proposition(), period == null ? null
                                : new PublicationPositionsResult.PeriodResult(period.from(), period.to()),
                        positions.consultedAt(), positions.selectionCriteria(),
                        positions.units().stream().map(unit -> new PublicationPositionsResult.UnitResult(
                                unit.id(), unit.sourceIds(), unit.position().name(), unit.explanation(), trace(unit.trace()))).toList(),
                        positions.excluded().stream().map(excluded -> new PublicationPositionsResult.ExcludedResult(
                                excluded.sourceId(), excluded.reason(), trace(excluded.trace()))).toList()),
                report.limitations(), analyzedAt, report.asOf(), claims == null ? null :
                        new ClaimAnalysisResult(claims.analysisTarget(), new ClaimAnalysisResult.DecompositionResult(
                                claims.decomposition().kind().name(), claims.decomposition().basis().name(), claims.decomposition().explanation()),
                                claims.claims().stream().map(claim -> new ClaimAnalysisResult.ClaimResult(claim.id(),
                                        claim.inputExcerpt(), claim.proposition(), new VerdictResult(claim.verdict().status().name(),
                                        claim.verdict().explanation(), claim.verdict().documentarySupport(), claim.verdict().sourceIds()))).toList()));
    }

    private static PublicationTraceResult trace(com.crosscheck.domain.analysis.PublicationTrace trace) {
        if (trace == null) return null;
        var scope = trace.scope();
        return new PublicationTraceResult(trace.stanceOwner().name(), trace.stanceHolder(),
                new PublicationTraceResult.ScopeResult(scope.outcome(), scope.geography(), scope.studyPeriod(),
                        scope.policy(), scope.match().name(), scope.explanation()), trace.temporalRelation().name(),
                trace.arguments().stream().map(argument -> new PublicationTraceResult.ArgumentResult(argument.sourceId(),
                        argument.locator(), argument.paraphrase(), argument.attribution().name(), argument.relation().name())).toList());
    }
}
