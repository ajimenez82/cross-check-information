package com.crosscheck.application.model;

import com.crosscheck.domain.analysis.AnalysisReport;

/** The handler verifies that the adapter has supplied both a session and a report. */
public record AiAnalysisTurn(String sessionId, AnalysisReport report, Clarification clarification) {
    public AiAnalysisTurn(String sessionId, AnalysisReport report) { this(sessionId, report, null); }
    @Override
    public String toString() {
        return "AiAnalysisTurn[redacted]";
    }
}
