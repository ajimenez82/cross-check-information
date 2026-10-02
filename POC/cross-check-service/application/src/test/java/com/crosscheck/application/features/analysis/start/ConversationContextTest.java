package com.crosscheck.application.features.analysis.start;

import com.crosscheck.application.contracts.*;
import com.crosscheck.application.model.*;
import com.crosscheck.domain.analysis.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConversationContextTest {
    private static final Instant NOW = Instant.parse("2026-09-28T12:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AnalysisPolicy policy = new AnalysisPolicy(10000, 4096, "test-v3", Duration.ofHours(2));
    private static class Codec implements ConversationReferenceCodec {
        final Map<String, ConversationReference> references = new HashMap<>();
        public String encode(ConversationReference reference) {
            String token = UUID.randomUUID().toString(); references.put(token, reference); return token;
        }
        public ConversationReference decode(String token) { return references.get(token); }
    }
    private AnalysisReport report(String target, String anchor) {
        var verdict = new Verdict(VerdictStatus.INSUFFICIENT_EVIDENCE, "Insufficient", "Unknown", List.of());
        var claims = new ClaimAnalysis(target, new ClaimAnalysis.Decomposition(ClaimAnalysis.Kind.SINGLE,
                ClaimAnalysis.Basis.SINGLE_PROPOSITION, "One proposition"),
                List.of(new ClaimAnalysis.Claim("C1", anchor, target, verdict)));
        return new AnalysisReport("Test", "Test", "Test", List.of(), claims.aggregate(), List.of(),
                new PublicationPositions(ClassificationAvailability.UNAVAILABLE, "No sample", null, null, null,
                        null, List.of(), List.of()), List.of(), null, claims);
    }
    @Test void explicitGeographyFollowUpRetainsPriorTargetAndPeriod() {
        var codec = new Codec();
        var calls = new AtomicInteger();
        String target = "Efecto sobre la oferta en España durante 2023–2025";
        var handler = new StartAnalysisHandler(input -> {
            if (calls.getAndIncrement() == 0) return new AiAnalysisTurn("session", report(target, input.text()));
            assertEquals(target, input.context().analysisTarget());
            assertEquals(List.of(target), input.context().propositions());
            assertEquals("¿Y en Cataluña?", input.text());
            return new AiAnalysisTurn("session", report("Efecto sobre la oferta en Cataluña durante 2023–2025", input.text()));
        }, codec, policy, clock);
        var first = handler.handle(new StartAnalysisCommand(target, null));
        var next = handler.handle(new StartAnalysisCommand("¿Y en Cataluña?", first.conversationToken()));
        assertTrue(next.analysis().claimAnalysis().analysisTarget().contains("Cataluña durante 2023–2025"));
        assertEquals(2, calls.get());
    }
    @Test void clarificationPreservesTargetAndDoesNotCreateVerdict() {
        var codec = new Codec();
        var calls = new AtomicInteger();
        var handler = new StartAnalysisHandler(input -> {
            int index = calls.getAndIncrement();
            if (index == 1) return new AiAnalysisTurn("session", null,
                    new Clarification("¿Qué periodo posterior quieres analizar?", Clarification.Reason.MISSING_PERIOD));
            if (index == 2) {
                assertEquals("Initial target", input.context().analysisTarget());
                assertTrue(input.context().previousInput().contains("¿Y después?"));
                assertNotNull(input.context().pendingQuestion());
                assertEquals("2026–2027", input.text());
            }
            return new AiAnalysisTurn("session", report(index == 2 ? "New period 2026–2027" : "Initial target", input.text()));
        }, codec, policy, clock);
        var first = handler.handle(new StartAnalysisCommand("Initial target", null));
        var question = handler.handle(new StartAnalysisCommand("¿Y después?", first.conversationToken()));
        assertNull(question.analysis());
        assertEquals(Clarification.Reason.MISSING_PERIOD, question.clarification().reason());
        var next = handler.handle(new StartAnalysisCommand("2026–2027", question.conversationToken()));
        assertNull(next.clarification());
        assertEquals("New period 2026–2027", next.analysis().claimAnalysis().analysisTarget());
    }
    @Test void restartRequestsFullInputWithoutCallingProvider() {
        var codec = new Codec();
        var firstHandler = new StartAnalysisHandler(input -> new AiAnalysisTurn("session", report("Target", input.text())), codec, policy, clock);
        var first = firstHandler.handle(new StartAnalysisCommand("Target", null));
        var restarted = new StartAnalysisHandler(input -> { fail("Must not call provider"); return null; }, codec, policy, clock);
        var result = restarted.handle(new StartAnalysisCommand("Follow-up", first.conversationToken()));
        assertNull(result.analysis()); assertNull(result.conversationToken());
        assertEquals(Clarification.Reason.CONTEXT_UNAVAILABLE, result.clarification().reason());
    }
    @Test void snapshotsAreBoundedExpiredAndSessionBound() {
        var store = new ConversationContextStore(clock, 1);
        var context = new ConversationContext("Input", "Target", List.of("Target"), null);
        var first = store.put("session", context, NOW.plusSeconds(60));
        assertNull(store.get(first, "another-session"));
        var second = store.put("session", context, NOW.plusSeconds(60));
        assertNull(store.get(first, "session")); assertEquals(context, store.get(second, "session"));
        var expired = store.put("session", context, NOW);
        assertNull(store.get(expired, "session"));
    }
    @Test void topicChangeReplacesContextInsteadOfAppendingOldClaims() {
        var codec = new Codec();
        var store = new ConversationContextStore(clock, 10);
        var handler = new StartAnalysisHandler(input -> new AiAnalysisTurn("session", report(input.text(), input.text())), codec, policy, clock, store);
        var first = handler.handle(new StartAnalysisCommand("Housing", null));
        var next = handler.handle(new StartAnalysisCommand("Transport", first.conversationToken()));
        var ref = codec.decode(next.conversationToken());
        var context = store.get(ref.contextId(), ref.sessionId());
        assertEquals(List.of("Transport"), context.propositions());
        assertEquals("Transport", context.previousInput());
    }
}
