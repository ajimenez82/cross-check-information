package com.crosscheck.presentation.api.analysis.start;

import com.crosscheck.application.features.analysis.start.StartAnalysisHandler;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StartAnalysisController {
    private final StartAnalysisHandler handler;
    private final boolean asyncEnabled;
    public StartAnalysisController(StartAnalysisHandler handler) { this(handler, false); }
    public StartAnalysisController(StartAnalysisHandler handler, boolean asyncEnabled) {
        this.handler=handler; this.asyncEnabled=asyncEnabled;
    }

    @PostMapping(value = "/api/analysis/start", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public StartAnalysisResponse start(@Valid @RequestBody StartAnalysisRequest request) {
        if (asyncEnabled) throw new com.crosscheck.application.features.analysis.jobs.JobException(409,"ASYNC_ANALYSIS_REQUIRED");
        return StartAnalysisHttpMapper.toResponse(handler.handle(StartAnalysisHttpMapper.toCommand(request)));
    }
}
