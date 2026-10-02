package com.crosscheck.application.features.analysis.start;

import com.crosscheck.domain.analysis.*;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClaimVerdictMappingTest {
    @Test void singleInsufficientClaimRetainsEditorialQualificationAndDocumentaryDetail() {
        var documentary = new Verdict(VerdictStatus.INSUFFICIENT_EVIDENCE, "Insufficient evidence", "Documentary limits", List.of("S1"));
        var claims = new ClaimAnalysis("Target", new ClaimAnalysis.Decomposition(ClaimAnalysis.Kind.SINGLE,
                ClaimAnalysis.Basis.SINGLE_PROPOSITION, "One claim"),
                List.of(new ClaimAnalysis.Claim("C1", "Question", "Target", documentary)));
        var positions = new PublicationPositions(ClassificationAvailability.AVAILABLE, null, "Target", null, null,
                "Selection", List.of(new PublicationAssessment("U1", List.of("S1"), PublicationPosition.SUPPORTS, "Own stance")), List.of());
        var report = new AnalysisReport("Title", "Context", "Summary", List.of("S1"), claims.aggregate(),
                List.of(new Evidence("S1", "Source", URI.create("https://example.org"), null, null, null, "Contribution", null)),
                positions, List.of(), null, claims);
        var result = AnalysisResultMapper.map(report, Instant.EPOCH);
        assertEquals("SUPPORTED_BY_PUBLICATIONS", result.verdict().status());
        assertEquals("INSUFFICIENT_EVIDENCE", result.claimAnalysis().claims().getFirst().verdict().status());
        assertEquals("Documentary limits", result.verdict().documentarySupport());
    }
}
