package com.crosscheck.infrastructure.development;
import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.application.model.*;
/** Offline adapter; inspecting is deterministic and never consumes remote inference. */
public final class DevelopmentJobProvider implements JobProvider {
    private final AiPoliticalAnalysisService service;
    private final String revision;
    public DevelopmentJobProvider(AiPoliticalAnalysisService service,String revision) {this.service=service;this.revision=revision;}
    public boolean requiresContext() {return false;}
    public String identity() {return "development:"+revision;}
    public Remote prepare(AnalysisJob job) {return new Remote(job.sessionId==null?"dev_"+job.id:job.sessionId,null);}
    public void submit(AnalysisJob job) {}
    public Observation inspect(AnalysisJob job) {
        if (job.terminal()) return new Observation(job.sessionId,"dev_turn_"+job.id,null,null,true);
        var input=job.input;
        try {
        var response=service.analyze(new AiAnalysisInput(input.text(),input.category(),input.agentRevision(),input.sessionId(),input.context()));
        return new Observation(job.sessionId,"dev_turn_"+job.id,new AiAnalysisTurn(job.sessionId,response.report(),response.clarification()),null,true);
        } catch (com.crosscheck.application.error.InvalidAnalysisOutputException invalid) {
            return new Observation(job.sessionId,"dev_turn_"+job.id,null,"INVALID_ANALYSIS_OUTPUT",true);
        }
    }
}
