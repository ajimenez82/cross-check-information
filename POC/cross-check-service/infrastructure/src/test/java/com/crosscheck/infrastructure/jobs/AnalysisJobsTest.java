package com.crosscheck.infrastructure.jobs;
import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.features.analysis.start.*;
import com.crosscheck.application.model.*;
import com.crosscheck.application.error.*;
import com.crosscheck.infrastructure.conversation.EncryptedConversationReferenceCodec;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.crosscheck.application.features.analysis.jobs.AnalysisJob.Status.*;

class AnalysisJobsTest {
    @TempDir Path directory;
    final MutableClock clock=new MutableClock();
    final FakeProvider remote=new FakeProvider();
    final String credential=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    final String other=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]).replaceFirst("A","B");
    final String key=UUID.randomUUID().toString();
    final StartAnalysisCommand command=new StartAnalysisCommand("¿Se recupera la oferta de vivienda?",null);
    JdbcJobStore store;
    AnalysisJobs jobs;
    AnalysisJobWorker worker;
    EncryptedConversationReferenceCodec codec;
    @BeforeEach void open() {reopen();}
    void reopen() {
        if(store!=null) store.close();
        store=new JdbcJobStore(directory.resolve("jobs"),clock);
        codec=new EncryptedConversationReferenceCodec(Base64.getEncoder().encodeToString(new byte[32]),clock,4096);
        var handler=new StartAnalysisHandler(input -> {throw new AssertionError("Synchronous provider used");},codec,
                new AnalysisPolicy(10000,4096,"test-v3",Duration.ofHours(2)),clock,store);
        jobs=new AnalysisJobs(store,handler,codec,remote,JobPolicy.defaults(),clock);
        worker=new AnalysisJobWorker(store,remote,handler,JobPolicy.defaults(),clock);
    }
    @AfterEach void close() {if(store!=null)store.close();}
    AnalysisJob.View create() {return jobs.create(key,credential,command,"request-test").job();}
    void step(long seconds) {clock.advance(seconds);worker.tick();}
    AnalysisJob.View finish(String id) {for(int i=0;i<6;i++) step(3);return jobs.get(id,credential);}
    void code(String expected,org.junit.jupiter.api.function.Executable action) {assertEquals(expected,assertThrows(JobException.class,action).code());}

    @Test void initialSubmissionIsCheckpointedAndNeverSentAgainAfterRestart() {
        remote.initialSubmission=true;
        var id=create().analysisId();
        remote.afterSubmit=()->assertEquals("input-hash",store.transact(tx->tx.find(id).remoteInputHash));
        worker.tick();assertEquals(RUNNING,jobs.get(id,credential).status());
        reopen();assertEquals(COMPLETED,finish(id).status());assertEquals(1,remote.submissions);
    }
    @Test void lostInitialSessionResponseRecoversWithoutSubmittingAnEvent() {
        remote.initialSubmission=true;remote.loseResponse=true;
        var id=create().analysisId();worker.tick();assertEquals(RECOVERING,jobs.get(id,credential).status());
        reopen();step(10);assertEquals(COMPLETED,jobs.get(id,credential).status());assertEquals(1,remote.submissions);
    }
    @Test void recoveredInitialSessionWithoutTurnDoesNotBecomeReadyToResubmit() {
        remote.initialSubmission=true;remote.loseResponse=true;remote.ready=false;
        var id=create().analysisId();worker.tick();step(10);reopen();step(10);
        assertEquals(RECOVERING,jobs.get(id,credential).status());assertEquals(1,remote.submissions);
        assertEquals(AnalysisJob.Phase.CREATE_SESSION,store.transact(tx->tx.find(id).phase));
        remote.ready=true;step(10);assertEquals(COMPLETED,jobs.get(id,credential).status());assertEquals(1,remote.submissions);
    }
    @Test void rejectedCreationFailsWithoutReservingRemoteCapacity() {
        remote.initialSubmission=true;remote.rejectPreparation=true;
        var id=create().analysisId();worker.tick();
        assertEquals(FAILED,jobs.get(id,credential).status());assertEquals("NOT_STARTED",jobs.get(id,credential).error().executionState());
        assertFalse(store.<Boolean>transact(tx->tx.find(id).reserved));assertEquals(0,remote.submissions);
    }
    @Test void replayIsDurableAndDoesNotRequireProvider() {
        var first=create();reopen();var replay=jobs.create(key,credential,command,"other-request");
        assertTrue(replay.replay());assertEquals(first.analysisId(),replay.job().analysisId());assertEquals(0,remote.submissions);
        code("IDEMPOTENCY_CONFLICT",()->jobs.create(key,credential,new StartAnalysisCommand("Different",null),"x"));
        code("ANALYSIS_JOB_NOT_FOUND",()->jobs.get(first.analysisId(),other));
        code("INVALID_JOB_CREDENTIAL",()->jobs.get(first.analysisId(),"bad"));
    }
    @Test void concurrentDuplicatesCreateOneJob() throws Exception {
        try(var executor=Executors.newFixedThreadPool(4)) {
            var tasks=new ArrayList<Future<String>>();for(int i=0;i<12;i++)tasks.add(executor.submit(()->create().analysisId()));
            var ids=new HashSet<String>();for(var task:tasks)ids.add(task.get());assertEquals(1,ids.size());
        }
        assertEquals(1,store.<Integer>transact(tx->tx.jobs().size()).intValue());
    }
    @Test void slowResultSurvivesRestartAndKeepsContextAndToken() {
        var id=create().analysisId();worker.tick();step(0);assertEquals(1,remote.submissions);
        remote.ready=false;step(106);assertEquals(RUNNING,jobs.get(id,credential).status());
        reopen();remote.ready=true;step(3);
        var completed=jobs.get(id,credential);assertEquals(COMPLETED,completed.status());
        var token=completed.result().conversationToken();var reference=codec.decode(token);
        reopen();assertEquals(token,jobs.get(id,credential).result().conversationToken());
        assertNotNull(store.get(reference.contextId(),reference.sessionId()));assertEquals(1,remote.submissions);
    }
    @Test void responseLostAfterRemoteAcceptanceNeverResends() {
        remote.loseResponse=true;var id=create().analysisId();worker.tick();step(0);
        assertEquals(RECOVERING,jobs.get(id,credential).status());reopen();step(10);
        assertEquals(COMPLETED,jobs.get(id,credential).status());assertEquals(1,remote.submissions);
    }
    @Test void crashBeforeSubmissionDoesNotGuessWhetherPostHappened() {
        var id=create().analysisId();worker.tick();
        store.transact(tx->{var j=tx.find(id);j.phase=AnalysisJob.Phase.SENDING;j.status=SUBMITTING;tx.save(j);return null;});
        reopen();step(3);assertEquals(RECOVERING,jobs.get(id,credential).status());assertEquals(0,remote.submissions);
        step(601);assertEquals(FAILED,jobs.get(id,credential).status());
        assertEquals("ANALYSIS_SUBMISSION_UNKNOWN",jobs.get(id,credential).error().code());
        assertTrue(store.<Boolean>transact(tx->tx.find(id).reserved).booleanValue());
    }
    @Test void expiredLeaseOnlyReconcilesAndDoesNotRepeatSubmission() {
        var id=create().analysisId();worker.tick();
        store.transact(tx->{var j=tx.find(id);j.phase=AnalysisJob.Phase.SENDING;j.leaseOwner="old-worker";
            j.leaseUntil=clock.instant().plusSeconds(60);tx.save(j);return null;});
        worker.tick();assertEquals(0,remote.submissions);step(61);assertEquals(RECOVERING,jobs.get(id,credential).status());
    }
    @Test void queueDeadlineDoesNotSubmit() {
        var id=create().analysisId();step(121);assertEquals("ANALYSIS_QUEUE_EXPIRED",jobs.get(id,credential).error().code());
        assertEquals(0,remote.submissions);
    }
    @Test void deadlineDoesNotReleaseUnknownRemoteWork() {
        remote.ready=false;var id=create().analysisId();worker.tick();step(0);step(601);
        assertEquals(FAILED,jobs.get(id,credential).status());assertTrue(store.<Boolean>transact(tx->tx.find(id).reserved).booleanValue());
        remote.ready=true;step(61);assertFalse(store.<Boolean>transact(tx->tx.find(id).reserved).booleanValue());
        assertEquals(FAILED,jobs.get(id,credential).status());assertEquals(1,remote.submissions);
    }
    @Test void retentionRemovesResultButKeepsDeduplicationTombstone() {
        var id=create().analysisId();assertEquals(COMPLETED,finish(id).status());step(86400);
        code("ANALYSIS_JOB_EXPIRED",()->jobs.create(key,credential,command,"x"));
        assertNull(store.transact(tx->tx.find(id).input));assertNull(store.transact(tx->tx.find(id).result));
    }
    @Test void followUpsAreSerializedAndOldContextRejected() {
        var id=create().analysisId();var result=finish(id);String token=result.result().conversationToken();
        var follow=new StartAnalysisCommand("En 2025",token);
        String next=jobs.create(UUID.randomUUID().toString(),credential,follow,"x").job().analysisId();
        code("ANALYSIS_CONFLICT",()->jobs.create(UUID.randomUUID().toString(),credential,follow,"x"));
        finish(next);code("ANALYSIS_CONFLICT",()->jobs.create(UUID.randomUUID().toString(),credential,follow,"x"));
    }
    @Test void acceptedReplayWorksAfterInputTokenExpires() {
        var first=finish(create().analysisId());var follow=new StartAnalysisCommand("En 2025",first.result().conversationToken());
        String nextKey=UUID.randomUUID().toString();var next=jobs.create(nextKey,credential,follow,"x");clock.advance(7201);
        assertEquals(next.job().analysisId(),jobs.create(nextKey,credential,follow,"x").job().analysisId());
    }
    @Test void changedRevisionFailsBeforeSendingQueuedJob() {
        var id=create().analysisId();remote.identity="different";worker.tick();
        assertEquals("ANALYSIS_REVISION_UNAVAILABLE",jobs.get(id,credential).error().code());assertEquals(0,remote.submissions);
    }
    @Test void transactionFailureRollsBackContextAndJobTogether() {
        var id=create().analysisId();
        assertThrows(IllegalStateException.class,()->store.transact(tx->{
            store.put("session",new ConversationContext("input",null,List.of(),null),clock.instant().plusSeconds(60));
            var job=tx.find(id);job.status=FAILED;tx.save(job);throw new IllegalStateException("test rollback");
        }));
        assertNull(store.transact(tx->tx.latestContextId("session")));assertEquals(QUEUED,jobs.get(id,credential).status());
    }
    @Test void closedStorageCannotAcknowledgeOrSubmit() {
        store.close();code("ANALYSIS_STORAGE_UNAVAILABLE",this::create);assertEquals(0,remote.submissions);store=null;
    }
    @Test void lostFinalCommitRecoversWithoutAnotherInference() {
        var failingStore=new FailingCompletionStore(store);
        var handler=new StartAnalysisHandler(input->{throw new AssertionError();},codec,
                new AnalysisPolicy(10000,4096,"test-v3",Duration.ofHours(2)),clock,failingStore);
        worker=new AnalysisJobWorker(failingStore,remote,handler,JobPolicy.defaults(),clock);
        var id=create().analysisId();worker.tick();step(0);
        code("ANALYSIS_STORAGE_UNAVAILABLE",()->step(3));
        assertNull(store.transact(tx->tx.latestContextId("session_"+id)));
        assertNull(jobs.get(id,credential).result());
        step(61);assertEquals(COMPLETED,jobs.get(id,credential).status());assertEquals(1,remote.submissions);
    }
    @Test void lateWorkerCannotOverwriteCompletionAfterLeaseTakeover() {
        var handler=new StartAnalysisHandler(input->{throw new AssertionError();},codec,
                new AnalysisPolicy(10000,4096,"test-v3",Duration.ofHours(2)),clock,store);
        var replacement=new AnalysisJobWorker(store,remote,handler,JobPolicy.defaults(),clock);
        var id=create().analysisId();worker.tick();
        remote.afterSubmit=()->{clock.advance(61);replacement.tick();};
        step(0);assertEquals(COMPLETED,jobs.get(id,credential).status());assertEquals(1,remote.submissions);
    }
    @Test void queueCapacityIsBoundedWithoutContactingProvider() {
        for(int i=0;i<22;i++)jobs.create(UUID.randomUUID().toString(),credential,command,"x");
        code("ANALYSIS_CAPACITY_EXCEEDED",()->jobs.create(UUID.randomUUID().toString(),credential,command,"x"));
        assertEquals(0,remote.submissions);
    }
    @Test void missingContextReturnsLocalClarificationWithoutProvider() {
        var reference=new ConversationReference("session_missing",com.crosscheck.domain.analysis.AnalysisCategory.POLITICAL_ANALYSIS,
                "test-v3",clock.instant().plusSeconds(60),UUID.randomUUID().toString());
        var result=jobs.create(key,credential,new StartAnalysisCommand("¿Y después?",codec.encode(reference)),"x").job();
        assertEquals(COMPLETED,result.status());assertNull(result.result().conversationToken());
        assertEquals(Clarification.Reason.CONTEXT_UNAVAILABLE,result.result().clarification().reason());assertEquals(0,remote.submissions);
    }
    @Test void temporaryPollingFailureRespectsRetryAfterWithoutSubmittingAgain() {
        var id=create().analysisId();worker.tick();step(0);remote.pollFailure=true;step(3);
        assertEquals(RECOVERING,jobs.get(id,credential).status());
        assertTrue(store.<Instant>transact(tx->tx.find(id).nextPollAt).isAfter(clock.instant().plusSeconds(119)));
        remote.pollFailure=false;step(30);assertEquals(RECOVERING,jobs.get(id,credential).status());
        step(92);assertEquals(COMPLETED,jobs.get(id,credential).status());assertEquals(1,remote.submissions);
    }
    @Test void expiredQuarantineKeepsOnlyRecoveryMetadataAndCanStillReleaseCapacity() {
        remote.ready=false;var id=create().analysisId();worker.tick();step(0);step(601);step(86400);
        assertTrue(store.<Boolean>transact(tx->tx.find(id).reserved).booleanValue());
        assertNull(store.transact(tx->tx.find(id).input));
        remote.ready=true;step(61);assertFalse(store.<Boolean>transact(tx->tx.find(id).reserved).booleanValue());
        code("ANALYSIS_JOB_EXPIRED",()->jobs.get(id,credential));
    }
    static final class FailingCompletionStore implements JobStore {
        final JobStore delegate;boolean fail=true;
        FailingCompletionStore(JobStore delegate){this.delegate=delegate;}
        public <T> T transact(java.util.function.Function<Transaction,T> action) {
            return delegate.transact(tx->action.apply(new Transaction(){
                public List<AnalysisJob> jobs(){return tx.jobs();}
                public AnalysisJob find(String id){return tx.find(id);}
                public AnalysisJob byKey(String key){return tx.byKey(key);}
                public void delete(String id){tx.delete(id);}
                public void purgeContexts(){tx.purgeContexts();}
                public String latestContextId(String session){return tx.latestContextId(session);}
                public void save(AnalysisJob job){tx.save(job);if(fail && job.status==COMPLETED){fail=false;throw new JobException(503,"ANALYSIS_STORAGE_UNAVAILABLE");}}
            }));
        }
        public String put(String session,ConversationContext context,Instant expires){return delegate.put(session,context,expires);}
        public ConversationContext get(String id,String session){return delegate.get(id,session);}
    }
    static final class MutableClock extends Clock {
        Instant now=Instant.parse("2026-09-29T10:00:00Z");
        void advance(long seconds) {now=now.plusSeconds(seconds);}
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return now;}
    }
    static final class FakeProvider implements JobProvider {
        int submissions;boolean ready=true,loseResponse,pollFailure,initialSubmission,rejectPreparation;Runnable afterSubmit=()->{};String identity="fake:test-v3";
        final Set<String> accepted=new HashSet<>();
        public String identity(){return identity;}
        public boolean submitsDuringPrepare(){return initialSubmission;}
        public String submissionFingerprint(AnalysisJob job){return "input-hash";}
        public Remote prepare(AnalysisJob job){if(rejectPreparation)throw new AnalysisProviderException(AnalysisProviderException.Reason.UNAVAILABLE,AnalysisProviderException.ExecutionState.NOT_STARTED);if(initialSubmission && job.input.sessionId()==null)submit(job);return new Remote(job.sessionId==null?"session_"+job.id:job.sessionId,null);}
        public void submit(AnalysisJob job){submissions++;accepted.add(job.id);afterSubmit.run();if(loseResponse)throw new AnalysisProviderException(AnalysisProviderException.Reason.TIMEOUT,AnalysisProviderException.ExecutionState.UNKNOWN);}
        public Observation inspect(AnalysisJob job){
            if(pollFailure)throw new AnalysisProviderException(AnalysisProviderException.Reason.UNAVAILABLE,AnalysisProviderException.ExecutionState.UNKNOWN,120);
            if(!ready || !accepted.contains(job.id))return Observation.pending(initialSubmission && accepted.contains(job.id)?"session_"+job.id:job.sessionId,null);
            String session=job.sessionId==null?"session_"+job.id:job.sessionId;
            return new Observation(session,"turn_"+job.id,new AiAnalysisTurn(session,null,
                    new Clarification("¿Qué periodo quieres analizar?",Clarification.Reason.MISSING_PERIOD)),null,true);
        }
    }
}
