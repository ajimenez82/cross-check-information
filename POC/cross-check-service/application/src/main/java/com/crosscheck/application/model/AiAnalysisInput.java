package com.crosscheck.application.model;

import com.crosscheck.domain.analysis.AnalysisCategory;

/** sessionId is null for a new conversation; it never comes directly from the browser request. */
public record AiAnalysisInput(String text, AnalysisCategory category, String agentRevision, String sessionId, ConversationContext context) {
    public AiAnalysisInput(String text, AnalysisCategory category, String agentRevision, String sessionId) {
        this(text, category, agentRevision, sessionId, null);
    }
    public java.util.List<String> anchorTexts() {
        var texts = new java.util.ArrayList<String>();
        texts.add(text);
        if (context != null) {
            texts.add(context.previousInput());
            if (context.analysisTarget() != null) texts.add(context.analysisTarget());
            texts.addAll(context.propositions());
        }
        return texts.stream().distinct().toList();
    }
    @Override
    public String toString() {
        return "AiAnalysisInput[redacted]";
    }
}
