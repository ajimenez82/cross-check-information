package com.crosscheck.application.features.analysis.jobs;
import com.crosscheck.application.model.AiAnalysisTurn;
/** Each method performs bounded I/O; providers declare whether creating a session also initiates inference. */
public interface JobProvider {
    String identity();
    default boolean submitsDuringPrepare() { return false; }
    default boolean requiresContext() { return true; }
    default boolean startsNewSession(AnalysisJob job) { return job.input.sessionId() == null; }
    default String resultSessionId(AnalysisJob job) { return job.sessionId; }
    default boolean remoteMayBeRunning(AnalysisJob job) { return job.phase != AnalysisJob.Phase.READY && job.phase != AnalysisJob.Phase.NEXT_STAGE; }
    default void initialize(AnalysisJob job) {}
    record Remote(String sessionId, String previousTurnId) {}
    record Advance(String stage, String payload, boolean submit) {}
    record Observation(String sessionId, String turnId, AiAnalysisTurn result, String failure, boolean remoteTerminal, Advance advance, String usage) {
        public Observation(String sessionId, String turnId, AiAnalysisTurn result, String failure, boolean remoteTerminal) {
            this(sessionId, turnId, result, failure, remoteTerminal, null, null);
        }
        public Observation(String sessionId, String turnId, AiAnalysisTurn result, String failure, boolean remoteTerminal, Advance advance) {
            this(sessionId, turnId, result, failure, remoteTerminal, advance, null);
        }
        public static Observation pending(String session, String turn) { return new Observation(session, turn, null, null, false); }
    }
    default String submissionFingerprint(AnalysisJob job) { return null; }
    Remote prepare(AnalysisJob job);
    void submit(AnalysisJob job);
    Observation inspect(AnalysisJob job);
}
