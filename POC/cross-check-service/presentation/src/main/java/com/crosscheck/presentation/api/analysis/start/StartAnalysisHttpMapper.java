package com.crosscheck.presentation.api.analysis.start;

import com.crosscheck.application.features.analysis.start.StartAnalysisCommand;
import com.crosscheck.application.features.analysis.start.StartAnalysisResult;

final class StartAnalysisHttpMapper {
    private StartAnalysisHttpMapper() {}

    static StartAnalysisCommand toCommand(StartAnalysisRequest request) {
        return new StartAnalysisCommand(request.text(), request.conversationToken());
    }

    static StartAnalysisResponse toResponse(StartAnalysisResult result) {
        return new StartAnalysisResponse(result.conversationToken(), result.analysis());
    }
}
