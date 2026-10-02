package com.crosscheck.application.features.analysis.jobs;
import com.crosscheck.application.contracts.ConversationContexts;
import java.util.List;
import java.util.function.Function;
public interface JobStore extends ConversationContexts {
    interface Transaction {
        List<AnalysisJob> jobs();
        AnalysisJob find(String id);
        AnalysisJob byKey(String key);
        void save(AnalysisJob job);
        void delete(String id);
        String latestContextId(String sessionId);
        void purgeContexts();
    }
    <T> T transact(Function<Transaction, T> operation);
}
