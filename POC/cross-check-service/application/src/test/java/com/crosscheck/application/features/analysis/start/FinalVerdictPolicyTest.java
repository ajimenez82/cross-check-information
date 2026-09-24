package com.crosscheck.application.features.analysis.start;

import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.domain.analysis.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinalVerdictPolicyTest {
    private Verdict verdict(VerdictStatus status) {
        return new Verdict(status, "Documentary explanation", "Insufficient", List.of("evidence"));
    }

    private PublicationPositions positions(int supports, int questions, int mixed, int neutral) {
        var units = new ArrayList<PublicationAssessment>();
        int[] counts = {supports, questions, mixed, neutral};
        for (var position : PublicationPosition.values()) {
            for (int index = 0; index < counts[position.ordinal()]; index++) {
                String id = position.name() + index;
                // Multiple copies still count as one classified unit.
                units.add(new PublicationAssessment(id, List.of(id, id + "-copy"), position, "Example"));
            }
        }
        return new PublicationPositions(ClassificationAvailability.AVAILABLE,
                units.isEmpty() ? "No units" : null, "Proposition", null, Instant.EPOCH,
                "Selection", units, List.of());
    }

    @Test
    void derivesOnlyStrictSupportOrQuestionPluralitiesAcrossAllSmallDistributions() {
        for (int support = 0; support <= 3; support++)
            for (int question = 0; question <= 3; question++)
                for (int mixed = 0; mixed <= 3; mixed++)
                    for (int neutral = 0; neutral <= 3; neutral++) {
                        var expected = support > question && support > mixed && support > neutral
                                ? VerdictStatus.SUPPORTED_BY_PUBLICATIONS
                                : question > support && question > mixed && question > neutral
                                ? VerdictStatus.QUESTIONED_BY_PUBLICATIONS : VerdictStatus.INSUFFICIENT_EVIDENCE;
                        var result = FinalVerdictPolicy.resolve(verdict(VerdictStatus.INSUFFICIENT_EVIDENCE),
                                positions(support, question, mixed, neutral));
                        assertEquals(expected, result.status(), support + "/" + question + "/" + mixed + "/" + neutral);
                    }
    }

    @Test
    void acceptsPluralityBelowHalfAndPreservesDocumentaryExplanationAndReferences() {
        var original = verdict(VerdictStatus.INSUFFICIENT_EVIDENCE);
        var result = FinalVerdictPolicy.resolve(original, positions(4, 3, 2, 1));
        assertEquals(VerdictStatus.SUPPORTED_BY_PUBLICATIONS, result.status());
        assertTrue(result.explanation().startsWith("La evidencia disponible no permite respaldar ni refutar la afirmación."));
        assertTrue(result.explanation().contains(original.explanation()));
        assertEquals(original.documentarySupport(), result.documentarySupport());
        assertEquals(original.sourceIds(), result.sourceIds());
    }

    @Test
    void preservesAllOtherDocumentaryVerdictsRegardlessOfPublicationMajority() {
        for (var status : List.of(VerdictStatus.SUPPORTED, VerdictStatus.REFUTED, VerdictStatus.MISLEADING,
                VerdictStatus.OPINION, VerdictStatus.NO_SINGLE_VERDICT)) {
            var original = verdict(status);
            assertSame(original, FinalVerdictPolicy.resolve(original, positions(10, 0, 0, 0)));
            assertSame(original, FinalVerdictPolicy.resolve(original, positions(0, 10, 0, 0)));
        }
    }

    @Test
    void keepsInsufficientEvidenceWhenClassificationIsUnavailable() {
        var original = verdict(VerdictStatus.INSUFFICIENT_EVIDENCE);
        var unavailable = new PublicationPositions(ClassificationAvailability.UNAVAILABLE,
                "Unavailable", null, null, null, null, List.of(), List.of());
        assertSame(original, FinalVerdictPolicy.resolve(original, unavailable));
    }

    @Test
    void rejectsProviderSuppliedDerivedStates() {
        for (var status : List.of(VerdictStatus.SUPPORTED_BY_PUBLICATIONS, VerdictStatus.QUESTIONED_BY_PUBLICATIONS)) {
            assertThrows(InvalidAnalysisOutputException.class,
                    () -> FinalVerdictPolicy.resolve(verdict(status), positions(4, 3, 2, 1)));
        }
    }
}
