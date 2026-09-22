package com.crosscheck.presentation.api.analysis.start;

import com.crosscheck.application.features.analysis.start.StartAnalysisHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StartAnalysisController {
    private final StartAnalysisHandler handler;

    @PostMapping(value = "/api/analysis/start", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public StartAnalysisResponse start(@Valid @RequestBody StartAnalysisRequest request) {
        return StartAnalysisHttpMapper.toResponse(handler.handle(StartAnalysisHttpMapper.toCommand(request)));
    }
}
