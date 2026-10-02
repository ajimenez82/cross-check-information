package com.crosscheck.application.features.analysis.jobs;
import com.crosscheck.application.features.analysis.start.StartAnalysisHandler;
import com.crosscheck.application.error.*;
import com.crosscheck.domain.analysis.InvalidAnalysisReportException;
import java.time.*;
import java.util.*;
import static com.crosscheck.application.features.analysis.jobs.AnalysisJob.Status.*;
import static com.crosscheck.application.features.analysis.jobs.AnalysisJob.Phase.*;

/** One bounded checkpoint step, independent of browser polling. */
public final class AnalysisJobWorker {
    private final JobStore store;
    private final JobProvider provider;
    private final StartAnalysisHandler handler;
    private final JobPolicy policy;
    private final Clock clock;
    private final String owner=UUID.randomUUID().toString();
    public AnalysisJobWorker(JobStore store, JobProvider provider, StartAnalysisHandler handler, JobPolicy policy, Clock clock) {
        this.store=store; this.provider=provider; this.handler=handler; this.policy=policy; this.clock=clock;
    }
    public void tick() {
        var job=store.transact(tx -> {
            var now=clock.instant();
            tx.purgeContexts();
            var jobs=tx.jobs();
            for (var j:jobs) {
                if (!j.expiresAt.isAfter(now) && j.terminal()) {
                    if (!j.reserved && !j.createdAt.plus(policy.tombstoneRetention()).isAfter(now)) tx.delete(j.id);
                    else if (!j.tombstone) { j.input=null; j.result=null; j.error=null; j.stagePayload=null; j.tombstone=true; tx.save(j); }
                }
            }
            long reserved=jobs.stream().filter(j -> j.reserved).count();
            for (var j:jobs.stream().sorted(Comparator.comparing(x -> x.nextPollAt)).toList()) {
                if (j.tombstone && !j.reserved || j.terminal() && !j.reserved || j.nextPollAt.isAfter(now)
                        || j.leaseUntil!=null && j.leaseUntil.isAfter(now)) continue;
                if (!j.terminal() && j.status==QUEUED) {
                    if (!j.createdAt.plus(policy.queueTimeout()).isAfter(now)) { fail(j,"ANALYSIS_QUEUE_EXPIRED","NOT_STARTED",false);tx.save(j);continue; }
                    if (!j.providerIdentity.equals(provider.identity())) { fail(j,"ANALYSIS_REVISION_UNAVAILABLE","NOT_STARTED",false);tx.save(j);continue; }
                    if (reserved>=policy.concurrency()) continue;
                    if (provider.submitsDuringPrepare() && provider.startsNewSession(j)) j.remoteInputHash=provider.submissionFingerprint(j);
                    j.reserved=true; j.status=SUBMITTING; j.phase=CREATE_SESSION; j.deadline=now.plus(policy.executionTimeout());
                } else if (!j.terminal() && !j.deadline.isAfter(now)) {
                    fail(j,j.status==RECOVERING ? "ANALYSIS_SUBMISSION_UNKNOWN" : "ANALYSIS_DEADLINE_EXCEEDED",
                            provider.remoteMayBeRunning(j) ? "UNKNOWN" : "NOT_STARTED",provider.remoteMayBeRunning(j));
                    tx.save(j); continue;
                } else if (!j.terminal() && j.phase==NEXT_STAGE) {
                    if (!j.providerIdentity.equals(provider.identity())) { fail(j,"ANALYSIS_REVISION_UNAVAILABLE","NOT_STARTED",false);tx.save(j);continue; }
                    j.remoteInputHash=provider.submissionFingerprint(j); j.phase=CREATE_SESSION; j.status=SUBMITTING;
                } else if (!j.terminal() && j.phase==CREATE_SESSION) {
                    j.status=RECOVERING;
                } else if (!j.terminal() && j.phase==READY) {
                    j.remoteInputHash=provider.submissionFingerprint(j); j.phase=SENDING; j.status=SUBMITTING;
                } else if (!j.terminal() && (j.phase==SENDING || j.phase==CREATE_SESSION)) {
                    j.phase=POLL; j.status=RECOVERING;
                }
                j.leaseOwner=owner; j.leaseUntil=now.plus(policy.lease()); j.version++; j.updatedAt=now;
                tx.save(j); return j;
            }
            return null;
        });
        if (job==null) return;
        try {
            if (job.phase==CREATE_SESSION && !job.terminal() && (job.status==SUBMITTING || !provider.startsNewSession(job))) {
                var remote=provider.prepare(job);
                update(job,j -> { j.sessionId=remote.sessionId(); j.previousTurnId=remote.previousTurnId();
                    if (j.conversationSessionId==null) j.conversationSessionId=remote.sessionId();
                    if (provider.submitsDuringPrepare() && provider.startsNewSession(j)) { j.phase=POLL; j.status=RUNNING; }
                    else j.phase=READY; });
            } else if (job.phase==SENDING && job.status==SUBMITTING) {
                provider.submit(job);
                update(job,j -> { j.phase=POLL; j.status=RUNNING; });
            } else {
                var observation=provider.inspect(job);
                update(job,j -> {
                    if (j.sessionId!=null && observation.sessionId()!=null && !j.sessionId.equals(observation.sessionId())
                            || j.turnId!=null && observation.turnId()!=null && !j.turnId.equals(observation.turnId()))
                        throw new InvalidAnalysisOutputException();
                    j.pollFailures=0; j.retryAfterSeconds=0;
                    if (observation.sessionId()!=null) j.sessionId=observation.sessionId();
                    if (j.conversationSessionId==null) j.conversationSessionId=j.sessionId;
                    if (observation.turnId()!=null) j.turnId=observation.turnId();
                    if (observation.usage()!=null) j.stageUsage=observation.usage();
                    if (j.terminal()) {
                        if (observation.remoteTerminal()) { j.reserved=false; j.input=null; }
                        return;
                    }
                    if (!provider.submitsDuringPrepare() && j.phase==CREATE_SESSION && observation.sessionId()!=null && observation.turnId()==null
                            && observation.failure()==null) { j.phase=READY; j.status=SUBMITTING; return; }
                    if (observation.failure()!=null) {
                        fail(j,observation.failure(),"CONFIRMED",!observation.remoteTerminal()); return;
                    }
                    if (observation.advance()!=null) {
                        if (!observation.remoteTerminal() || observation.result()!=null) throw new InvalidAnalysisOutputException();
                        var next=observation.advance();
                        if (!Objects.equals(j.stage,next.stage())) {
                            j.stages.add(new AnalysisJob.StageRecord(j.stage,j.sessionId,j.turnId,j.remoteInputHash,clock.instant(),j.stageUsage));
                            j.stageUsage=null;
                        }
                        j.stage=next.stage();j.stagePayload=next.payload();j.status=RUNNING;
                        if (next.submit()) {j.sessionId=null;j.turnId=null;j.previousTurnId=null;j.remoteInputHash=null;j.phase=NEXT_STAGE;}
                        return;
                    }
                    if (observation.result()!=null) {
                        try {
                            if (!provider.resultSessionId(j).equals(observation.result().sessionId())) throw new InvalidAnalysisOutputException();
                            j.result=handler.complete(j.input,observation.result());
                            j.status=COMPLETED; j.completedAt=clock.instant(); j.reserved=false;
                            if (j.stage!=null) j.stages.add(new AnalysisJob.StageRecord(j.stage,j.sessionId,j.turnId,j.remoteInputHash,clock.instant(),j.stageUsage));
                        } catch (InvalidAnalysisOutputException | InvalidAnalysisReportException invalid) {
                            fail(j,"INVALID_ANALYSIS_OUTPUT","CONFIRMED",false);
                        }
                    } else if (j.turnId!=null) { j.phase=POLL; j.status=RUNNING; }
                });
            }
        } catch (JobException storage) { throw storage;
        } catch (RuntimeException failure) {
            update(job,j -> {
                if (j.terminal()) return;
                if (failure instanceof AnalysisProviderException p && p.executionState()==AnalysisProviderException.ExecutionState.NOT_STARTED
                        && (j.phase==SENDING || j.phase==CREATE_SESSION)) fail(j,"ANALYSIS_PROVIDER_FAILED","NOT_STARTED",false);
                else {
                    j.status=RECOVERING; j.pollFailures++;
                    if (failure instanceof AnalysisProviderException p) j.retryAfterSeconds=p.retryAfterSeconds();
                }
            });
        }
    }
    private void update(AnalysisJob checkpoint, java.util.function.Consumer<AnalysisJob> action) {
        store.transact(tx -> {
            var j=tx.find(checkpoint.id); var now=clock.instant();
            if (j==null || j.version!=checkpoint.version || !owner.equals(j.leaseOwner) || !j.leaseUntil.isAfter(now)) return null;
            action.accept(j);
            j.version++; j.updatedAt=now; j.leaseOwner=null; j.leaseUntil=null;
            long delay=j.terminal() ? 60 : j.phase==READY || j.phase==NEXT_STAGE ? 0 : Math.min(30,3L << Math.min(j.pollFailures,3));
            j.nextPollAt=now.plusSeconds(Math.max(delay,j.retryAfterSeconds));
            if (j.pollFailures>0) j.nextPollAt=j.nextPollAt.plusMillis(java.util.concurrent.ThreadLocalRandom.current().nextLong(1000));
            if (!j.terminal() && j.deadline!=null && j.nextPollAt.isAfter(j.deadline)) j.nextPollAt=j.deadline;
            tx.save(j); return null;
        });
    }
    private void fail(AnalysisJob j,String code,String execution,boolean reserved) {
        j.status=FAILED; j.completedAt=clock.instant();j.updatedAt=j.completedAt;j.reserved=reserved;
        j.error=new AnalysisJob.JobError(code,"No se ha podido completar el análisis. No se repetirá automáticamente.",j.requestId,execution);
        j.nextPollAt=clock.instant().plusSeconds(60);
    }
}
