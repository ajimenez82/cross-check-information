package com.crosscheck.domain.analysis;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PublicationTraceTest {
    private PublicationTrace trace(List<PublicationTrace.Argument> arguments) {
        return new PublicationTrace(PublicationTrace.Owner.AUTHOR, null,
                new PublicationTrace.Scope("Supply", "Spain", null, null,
                        PublicationTrace.Match.MATCH, "Matching scope"),
                PublicationTrace.TemporalRelation.REQUESTED_PERIOD, arguments);
    }

    private PublicationTrace.Argument argument(String sourceId) {
        return new PublicationTrace.Argument(sourceId, null, "Argument", PublicationTrace.Attribution.AUTHOR,
                PublicationTrace.Relation.QUESTIONS);
    }

    @Test void argumentsAreDefensivelyCopied() {
        var arguments = new ArrayList<>(List.of(argument("S1")));
        var trace = trace(arguments);
        arguments.clear();
        assertEquals(1, trace.arguments().size());
        assertThrows(UnsupportedOperationException.class, () -> trace.arguments().clear());
        assertNull(trace.arguments().getFirst().locator());
    }

    @Test void rejectsEmptyTraceAndArgumentsOutsideUnit() {
        assertThrows(InvalidAnalysisReportException.class, () -> trace(List.of()));
        assertThrows(InvalidAnalysisReportException.class, () -> new PublicationAssessment("U1", List.of("S1"),
                PublicationPosition.QUESTIONS, "Explanation", trace(List.of(argument("S2")))));
    }

    @Test void legacyConstructorsRetainAbsentTrace() {
        assertNull(new PublicationAssessment("U1", List.of("S1"), PublicationPosition.QUESTIONS, "Explanation").trace());
        assertNull(new ExcludedPublication("S1", "Reason").trace());
    }
}
