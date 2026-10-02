package com.crosscheck.infrastructure.jobs;

import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.features.analysis.start.*;
import com.crosscheck.application.model.*;
import com.crosscheck.application.error.*;
import com.crosscheck.infrastructure.conversation.EncryptedConversationReferenceCodec;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StagedJobsTest {
    @TempDir Path directory;
    final AnalysisJobsTest.MutableClock clock = new AnalysisJobsTest.MutableClock();
    final FakeStages provider = new FakeStages();
    final String credential = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    JdbcJobStore store;
    AnalysisJobs jobs;
    AnalysisJobWorker worker;
    @BeforeEach void open() { reopen(); }
    void reopen() {
        if (store != null) store.close();
        store = new JdbcJobStore(directory.resolve("jobs"), clock);
        var codec = new EncryptedConversationReferenceCodec(Base64.getEncoder().encodeToString(new byte[32]), clock, 4096);
        var handler = new StartAnalysisHandler(input -> { throw new AssertionError(); }, codec,
                new AnalysisPolicy(10000, 4096, "staged-v1", Duration.ofHours(2)), clock, store);
        jobs = new AnalysisJobs(store, handler, codec, provider, JobPolicy.defaults(), clock);
        worker = new AnalysisJobWorker(store, provider, handler, JobPolicy.defaults(), clock);
    }
    @AfterEach void close() { store.close(); }
    String create(String token) {
        return jobs.create(UUID.randomUUID().toString(), credential, new StartAnalysisCommand("Question", token), "test").job().analysisId();
    }
    void step() { clock.advance(4); worker.tick(); }
    AnalysisJob saved(String id) { return store.transact(tx -> tx.find(id)); }
    void finish() { for (int index = 0; index < 10; index++) step(); }

    @Test void persistsDossierBeforeSynthesisAndRestartsWithoutDuplicateSubmissions() {
        var id = create(null); step(); step();
        assertEquals("CAPTURE", saved(id).stage);
        reopen(); step();
        assertEquals("SYNTHESIS", saved(id).stage);
        assertEquals("accepted-dossier", saved(id).stagePayload);
        assertNull(saved(id).sessionId);
        assertEquals(AnalysisJob.Phase.NEXT_STAGE, saved(id).phase);
        reopen(); finish();
        assertEquals(AnalysisJob.Status.COMPLETED, jobs.get(id, credential).status());
        assertEquals(1, provider.count("RESEARCH")); assertEquals(1, provider.count("SYNTHESIS"));
        assertEquals(3, saved(id).stages.size());
        assertEquals("sess_" + id + "_RESEARCH", saved(id).conversationSessionId);
    }
    @Test void lostSynthesisResponseIsReconciledAfterRestartWithoutRepeatingPost() {
        provider.loseSynthesis = true;
        var id = create(null); step(); step(); step(); step();
        assertEquals(AnalysisJob.Status.RECOVERING, jobs.get(id, credential).status());
        reopen(); clock.advance(15); finish();
        assertEquals(AnalysisJob.Status.COMPLETED, jobs.get(id, credential).status());
        assertEquals(1, provider.count("SYNTHESIS"));
    }
    @Test void expiryBetweenStagesDoesNotStartSynthesisOrKeepRemoteReservation() {
        var id = create(null); step(); step(); step();
        assertEquals(AnalysisJob.Phase.NEXT_STAGE, saved(id).phase);
        clock.advance(601); worker.tick();
        assertEquals(AnalysisJob.Status.FAILED, jobs.get(id, credential).status());
        assertFalse(saved(id).reserved); assertEquals(0, provider.count("SYNTHESIS"));
    }
    @Test void expiryDuringCaptureDoesNotReserveAlreadyCompletedResearch() {
        var id = create(null); step(); step();
        clock.advance(601); worker.tick();
        assertFalse(saved(id).reserved); assertEquals(0, provider.count("SYNTHESIS"));
    }
    @Test void failedCaptureDoesNotStartAnotherInference() {
        provider.failCapture = true;
        var id = create(null); finish();
        assertEquals("EVIDENCE_CAPTURE_FAILED", jobs.get(id, credential).error().code());
        assertFalse(saved(id).reserved); assertEquals(0, provider.count("SYNTHESIS"));
    }
    @Test void changedRevisionBetweenStagesFailsBeforeNewRemoteSubmission() {
        var id = create(null); step(); step(); step();
        provider.identity = "changed"; step();
        assertEquals("ANALYSIS_REVISION_UNAVAILABLE", jobs.get(id, credential).error().code());
        assertEquals(0, provider.count("SYNTHESIS"));
    }
    @Test void followUpKeepsConversationIdentityWhileUsingNewRemoteStages() {
        var first = create(null); finish();
        var token = jobs.get(first, credential).result().conversationToken();
        var next = create(token); step();
        assertEquals(saved(first).conversationSessionId, saved(next).conversationSessionId);
        assertNotEquals(saved(first).sessionId, saved(next).sessionId);
        assertThrows(JobException.class, () -> create(token));
        finish(); assertEquals(AnalysisJob.Status.COMPLETED, jobs.get(next, credential).status());
        assertEquals(2, provider.count("RESEARCH")); assertEquals(2, provider.count("SYNTHESIS"));
    }
    @Test void retentionRemovesCapturedEvidenceAsWellAsResult() {
        var id = create(null); finish();
        clock.advance(86400); worker.tick();
        assertNull(saved(id).stagePayload); assertNull(saved(id).result); assertNull(saved(id).input);
    }

    final class FakeStages implements JobProvider {
        final Map<String,Integer> submissions = new HashMap<>();
        String identity = "fake-stages";
        boolean loseSynthesis, failCapture;
        int count(String stage) { return submissions.entrySet().stream().filter(entry -> entry.getKey().endsWith(stage)).mapToInt(Map.Entry::getValue).sum(); }
        public String identity() { return identity; }
        public boolean submitsDuringPrepare() { return true; }
        public boolean startsNewSession(AnalysisJob job) { return true; }
        public String resultSessionId(AnalysisJob job) { return job.conversationSessionId; }
        public void initialize(AnalysisJob job) { job.stage = "RESEARCH"; job.sessionId = null; }
        public boolean remoteMayBeRunning(AnalysisJob job) { return !job.stage.equals("CAPTURE") && job.phase != AnalysisJob.Phase.NEXT_STAGE; }
        public String submissionFingerprint(AnalysisJob job) { return job.id + job.stage; }
        public Remote prepare(AnalysisJob job) {
            assertEquals(submissionFingerprint(job), saved(job.id).remoteInputHash);
            if (job.stage.equals("SYNTHESIS")) assertEquals("accepted-dossier", saved(job.id).stagePayload);
            submissions.merge(job.id + job.stage, 1, Integer::sum);
            if (job.stage.equals("SYNTHESIS") && loseSynthesis)
                throw new AnalysisProviderException(AnalysisProviderException.Reason.TIMEOUT, AnalysisProviderException.ExecutionState.UNKNOWN);
            return new Remote("sess_" + job.id + "_" + job.stage, null);
        }
        public void submit(AnalysisJob job) { throw new AssertionError("Duplicate submission path"); }
        public Observation inspect(AnalysisJob job) {
            if (job.stage.equals("CAPTURE")) return failCapture
                    ? new Observation(job.sessionId, job.turnId, null, "EVIDENCE_CAPTURE_FAILED", true)
                    : new Observation(job.sessionId, job.turnId, null, null, true, new Advance("SYNTHESIS", "accepted-dossier", true));
            String session = "sess_" + job.id + "_" + job.stage, turn = "turn_" + job.stage;
            if (job.stage.equals("RESEARCH")) return new Observation(session, turn, null, null, true, new Advance("CAPTURE", "research-output", false));
            return new Observation(session, turn, new AiAnalysisTurn(job.conversationSessionId, null,
                    new Clarification("¿Qué periodo?", Clarification.Reason.MISSING_PERIOD)), null, true);
        }
    }
}
