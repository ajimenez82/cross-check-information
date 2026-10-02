package com.crosscheck.application.features.analysis.jobs;
import com.crosscheck.application.features.analysis.start.*;
import com.crosscheck.application.contracts.ConversationReferenceCodec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.*;

public final class AnalysisJobs {
    private final JobStore store;
    private final StartAnalysisHandler handler;
    private final ConversationReferenceCodec codec;
    private final JobProvider provider;
    private final JobPolicy policy;
    private final Clock clock;
    public AnalysisJobs(JobStore store, StartAnalysisHandler handler, ConversationReferenceCodec codec,
            JobProvider provider, JobPolicy policy, Clock clock) {
        this.store=store; this.handler=handler; this.codec=codec; this.provider=provider; this.policy=policy; this.clock=clock;
    }
    public record Accepted(boolean replay, AnalysisJob.View job) {}
    public Accepted create(String key, String credential, StartAnalysisCommand command, String requestId) {
        String access = credentialHash(credential);
        if (key == null || !key.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"))
            throw new JobException(400, "INVALID_IDEMPOTENCY_KEY");
        if (command == null || command.text() == null) throw new JobException(400, "INVALID_ANALYSIS_INPUT");
        String fingerprint = hash("v1:"+command.text().length()+":"+command.text()+":"
                +(command.conversationToken()==null ? -1 : command.conversationToken().length())+":"
                +Objects.toString(command.conversationToken(), ""));
        return store.transact(tx -> {
            var old=tx.byKey(key.toLowerCase(Locale.ROOT));
            if (old != null) {
                authenticate(old,access);
                if (!old.bodyHash.equals(fingerprint)) throw new JobException(409,"IDEMPOTENCY_CONFLICT");
                return new Accepted(true,old.view());
            }
            var prepared=handler.prepare(command);
            var input=prepared.input();
            var localResult=prepared.localResult();
            if (input!=null && provider.requiresContext() && input.sessionId()!=null && input.context()==null) {
                localResult=new StartAnalysisResult(null,null,new com.crosscheck.application.model.Clarification(
                        "El contexto anterior ya no está disponible. Escribe la consulta completa para iniciar un nuevo análisis.",
                        com.crosscheck.application.model.Clarification.Reason.CONTEXT_UNAVAILABLE));
            }
            if (input != null && input.sessionId()!=null) {
                if (tx.jobs().stream().anyMatch(j -> (!j.terminal() || j.reserved)
                        && (input.sessionId().equals(j.sessionId) || input.sessionId().equals(j.conversationSessionId)))) throw new JobException(409,"ANALYSIS_CONFLICT");
                String latest=tx.latestContextId(input.sessionId());
                if (latest!=null && !latest.equals(codec.decode(command.conversationToken()).contextId()))
                    throw new JobException(409,"ANALYSIS_CONFLICT");
            }
            if (tx.jobs().stream().filter(j -> !j.terminal()).count() >= policy.queueCapacity()+policy.concurrency())
                throw new JobException(429,"ANALYSIS_CAPACITY_EXCEEDED");
            var job=new AnalysisJob();
            job.id=UUID.randomUUID().toString(); job.idempotencyKey=key.toLowerCase(Locale.ROOT);
            job.accessHash=access; job.bodyHash=fingerprint; job.requestId=requestId;
            job.providerIdentity=provider.identity(); job.input=localResult==null?input:null;
            job.sessionId=input==null ? null : input.sessionId();
            job.conversationSessionId=job.sessionId;
            job.status=AnalysisJob.Status.QUEUED; job.phase=AnalysisJob.Phase.CREATE_SESSION;
            job.createdAt=clock.instant(); job.updatedAt=job.createdAt; job.nextPollAt=job.createdAt;
            job.expiresAt=job.createdAt.plus(policy.retention());
            if (localResult!=null) {
                job.status=AnalysisJob.Status.COMPLETED; job.completedAt=job.createdAt; job.result=localResult;
            }
            if (localResult==null) provider.initialize(job);
            tx.save(job); return new Accepted(false,job.view());
        });
    }
    public AnalysisJob.View get(String id,String credential) {
        String access=credentialHash(credential);
        return store.transact(tx -> { var job=tx.find(id); authenticate(job,access); return job.view(); });
    }
    private void authenticate(AnalysisJob job,String access) {
        if (job==null || !MessageDigest.isEqual(job.accessHash.getBytes(StandardCharsets.US_ASCII),access.getBytes(StandardCharsets.US_ASCII)))
            throw new JobException(404,"ANALYSIS_JOB_NOT_FOUND");
        if (!job.expiresAt.isAfter(clock.instant())) throw new JobException(410,"ANALYSIS_JOB_EXPIRED");
    }
    private static String credentialHash(String token) {
        if (token==null || !token.matches("[A-Za-z0-9_-]{43}")) throw new JobException(401,"INVALID_JOB_CREDENTIAL");
        byte[] bytes=Base64.getUrlDecoder().decode(token);
        if (bytes.length!=32 || !Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(token))
            throw new JobException(401,"INVALID_JOB_CREDENTIAL");
        return hash(token);
    }
    private static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
