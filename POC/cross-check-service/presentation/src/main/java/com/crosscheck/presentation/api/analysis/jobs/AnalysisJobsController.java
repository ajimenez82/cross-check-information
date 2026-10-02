package com.crosscheck.presentation.api.analysis.jobs;
import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.features.analysis.start.StartAnalysisCommand;
import com.crosscheck.presentation.api.analysis.start.StartAnalysisRequest;
import com.crosscheck.presentation.api.error.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public final class AnalysisJobsController {
    private final AnalysisJobs jobs;
    public AnalysisJobsController(AnalysisJobs jobs) {this.jobs=jobs;}
    @PostMapping(value="/api/analysis/jobs",consumes=MediaType.APPLICATION_JSON_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisJob.View> create(@RequestHeader(value="Idempotency-Key",required=false) String key,
            @RequestHeader(value="Authorization",required=false) String authorization,
            @Valid @RequestBody StartAnalysisRequest body,HttpServletRequest request) {
        var accepted=jobs.create(key,token(authorization),new StartAnalysisCommand(body.text(),body.conversationToken()),
                String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE)));
        var view=accepted.job();
        return ResponseEntity.status(accepted.replay() && view.pollAfterSeconds()==null?200:202)
                .location(URI.create("/api/analysis/jobs/"+view.analysisId()))
                .cacheControl(CacheControl.noStore()).header("Retry-After",String.valueOf(view.pollAfterSeconds()==null?0:view.pollAfterSeconds())).body(view);
    }
    @GetMapping(value="/api/analysis/jobs/{id}",produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisJob.View> get(@PathVariable String id,
            @RequestHeader(value="Authorization",required=false) String authorization) {
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
