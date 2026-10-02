package com.crosscheck.application.features.analysis.jobs;
import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.application.features.analysis.start.StartAnalysisResult;
import java.time.Instant;

/** Internal durable checkpoint; never serialize this class to an HTTP response. */
public final class AnalysisJob {
    public enum Status { QUEUED, SUBMITTING, RUNNING, RECOVERING, COMPLETED, FAILED }
    public enum Phase { CREATE_SESSION, READY, SENDING, POLL, NEXT_STAGE }
    public String id, idempotencyKey, accessHash, bodyHash, requestId, providerIdentity;
    public AiAnalysisInput input;
    public Status status;
    public Phase phase;
    public Instant createdAt, updatedAt, completedAt, expiresAt, deadline, nextPollAt, leaseUntil;
    public String sessionId, previousTurnId, turnId, leaseOwner, remoteInputHash;
    public String stage, stagePayload, conversationSessionId, stageUsage;
    public java.util.List<StageRecord> stages = new java.util.ArrayList<>();
    public record StageRecord(String stage, String sessionId, String turnId, String inputHash, Instant completedAt, String usage) {}
    public long version;
    public int pollFailures;
    public long retryAfterSeconds;
    public boolean reserved, tombstone;
    public StartAnalysisResult result;
    public JobError error;
    public boolean terminal() { return status == Status.COMPLETED || status == Status.FAILED; }
    @Override public String toString() { return "AnalysisJob[redacted]"; }
    public record JobError(String code, String message, String requestId, String executionState) {}
    public record View(String analysisId, Status status, Instant createdAt, Instant updatedAt,
            Instant completedAt, Instant expiresAt, Integer pollAfterSeconds,
            StartAnalysisResult result, JobError error) {}
    public View view() { return new View(id, status, createdAt, updatedAt, completedAt, expiresAt,
            terminal() ? null : 3, result, error); }
}
