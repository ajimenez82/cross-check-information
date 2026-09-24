package com.crosscheck.application.features.analysis.start;

import com.crosscheck.application.features.analysis.common.dto.*;
import com.crosscheck.domain.analysis.AnalysisReport;
import java.time.Instant;

final class AnalysisResultMapper {
    private AnalysisResultMapper() {}

    static AnalysisResult map(AnalysisReport report, Instant analyzedAt) {
        var verdict = FinalVerdictPolicy.resolve(report.verdict(), report.publicationPositions());
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
                                unit.id(), unit.sourceIds(), unit.position().name(), unit.explanation())).toList(),
                        positions.excluded().stream().map(excluded -> new PublicationPositionsResult.ExcludedResult(
                                excluded.sourceId(), excluded.reason())).toList()),
                report.limitations(), analyzedAt, report.asOf());
    }
}
