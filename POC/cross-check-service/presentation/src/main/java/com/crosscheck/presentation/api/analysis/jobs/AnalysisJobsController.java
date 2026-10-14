package com.crosscheck.presentation.api.analysis.jobs;
import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.features.analysis.start.StartAnalysisCommand;
import com.crosscheck.presentation.api.analysis.start.StartAnalysisRequest;
import com.crosscheck.presentation.api.error.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Tag(name="Analysis jobs")
@SecurityRequirement(name="JobCredential")
@RestController
public final class AnalysisJobsController {
    private final AnalysisJobs jobs;
    public AnalysisJobsController(AnalysisJobs jobs) {this.jobs=jobs;}
    @Operation(summary="Create or recover an analysis job",
            description="Use a new UUID v4 Idempotency-Key for a new query. Retry with the same key, body and bearer. Poll the Location URL using the same bearer.",
            responses={@ApiResponse(responseCode="202",description="Job accepted; poll using Retry-After"),
                    @ApiResponse(responseCode="200",description="Previously completed job recovered")})
    @PostMapping(value="/api/analysis/jobs",consumes=MediaType.APPLICATION_JSON_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisJob.View> create(@Parameter(required=true,description="UUID v4; reuse for retries of the same request") @RequestHeader(value="Idempotency-Key",required=false) String key,
            @Parameter(hidden=true) @RequestHeader(value="Authorization",required=false) String authorization,
            @Valid @RequestBody StartAnalysisRequest body,HttpServletRequest request) {
        var accepted=jobs.create(key,token(authorization),new StartAnalysisCommand(body.text(),body.conversationToken()),
                String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE)));
        var view=accepted.job();
        return ResponseEntity.status(accepted.replay() && view.pollAfterSeconds()==null?200:202)
                .location(URI.create("/api/analysis/jobs/"+view.analysisId()))
                .cacheControl(CacheControl.noStore()).header("Retry-After",String.valueOf(view.pollAfterSeconds()==null?0:view.pollAfterSeconds())).body(view);
    }
    @Operation(summary="Read the current job state and result",
            description="Polling only reads the persisted job. It does not start another provider request. COMPLETED may contain an analysis or a clarification; FAILED contains an error.")
    @GetMapping(value="/api/analysis/jobs/{id}",produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisJob.View> get(@PathVariable String id,
            @Parameter(hidden=true) @RequestHeader(value="Authorization",required=false) String authorization) {
        var view=jobs.get(id,token(authorization));
        var response=ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if(view.pollAfterSeconds()!=null) response.header("Retry-After",view.pollAfterSeconds().toString());
        return response.body(view);
    }
    private static String token(String header) {
        if(header==null || !header.startsWith("Bearer ")) throw new JobException(401,"INVALID_JOB_CREDENTIAL");
        return header.substring(7);
    }
}
