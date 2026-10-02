package com.crosscheck.infrastructure.openai;
import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.error.*;
import com.crosscheck.application.model.AiAnalysisInput;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.JsonNode;
import static com.crosscheck.infrastructure.openai.OpenAiJson.*;

/** No automatic POST retry: uncertain submissions are reconciled using saved input and turn identity. */
final class OpenAiJobProvider implements JobProvider {
    private final OpenAiTransport transport;
    private final String agentId, revision;
    private final OpenAiPoliticalAnalysisService reader;
    OpenAiJobProvider(OpenAiTransport transport,String agentId,String revision,OpenAiPoliticalAnalysisService reader) {
        this.transport=transport;this.agentId=agentId;this.revision=revision;this.reader=reader;
    }
    public boolean submitsDuringPrepare() { return true; }
    public String identity() {return "openai:"+agentId+":"+revision+":3";}
    private long deadline(AnalysisJob job) {
        long remaining=job.deadline==null || job.terminal() ? Duration.ofSeconds(15).toNanos()
                : Math.min(Duration.ofSeconds(15).toNanos(),Duration.between(java.time.Instant.now(),job.deadline).toNanos());
        return System.nanoTime()+Math.max(0,remaining);
    }
    private JsonNode get(String path,long deadline) {return transport.request(path,null,deadline,true,true);}
    public Remote prepare(AnalysisJob job) {
        long end=deadline(job);
        if (job.sessionId==null) {
            var session=transport.request("agents/sessions",Map.of("agent_id",agentId,"environment",Map.of("type","none"),
                    "metadata",Map.of("crosscheck_job",job.id),"input",inputText(job),"stream",false),end,false,false,"crosscheck-"+job.id);
            return new Remote(id(session,"id"),null);
        }
        var session=get("agents/sessions/"+job.sessionId,end);
        if (!job.sessionId.equals(id(session,"id")) || !"idle".equals(text(session,"status")))
            throw new AnalysisProviderException(AnalysisProviderException.Reason.CONFLICT,AnalysisProviderException.ExecutionState.NOT_STARTED);
        var turns=data(get("agents/sessions/"+job.sessionId+"/turns?order=desc&limit=1",end));
        String previous=null;
        if (!turns.isEmpty()) {
            var turn=turns.get(0);
            if (!Set.of("completed","failed","cancelled").contains(text(turn,"status")))
                throw new AnalysisProviderException(AnalysisProviderException.Reason.CONFLICT,AnalysisProviderException.ExecutionState.NOT_STARTED);
            previous=id(turn,"id");
        }
        return new Remote(job.sessionId,previous);
    }
    private String inputText(AnalysisJob job) {
        var payload=new LinkedHashMap<String,Object>();
        payload.put("currentInput",job.input.text());payload.put("previousContext",job.input.context());
        payload.put("analysisRequestId",job.id);
        return MAPPER.writeValueAsString(payload);
    }
    public String submissionFingerprint(AnalysisJob job) { return hash(inputText(job)); }
    private static String hash(String text) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public void submit(AnalysisJob job) {
        transport.request("agents/sessions/"+job.sessionId+"/events",Map.of("events",List.of(Map.of(
                "type","agent.session.input.message","input",List.of(Map.of("role","user","content",
                List.of(Map.of("type","input_text","text",inputText(job)))))))),deadline(job),true,false,"crosscheck-"+job.id);
    }
    private List<JsonNode> pages(String path,long end) {
        var result=new ArrayList<JsonNode>();String cursor=null;var seen=new HashSet<String>();
        for(int page=0;page<20;page++) {
            var response=get(path+(cursor==null?"":"&after="+cursor),end);
            data(response).forEach(result::add);
            if (!response.path("has_more").isBoolean()) throw new InvalidAnalysisOutputException();
            if (!response.path("has_more").asBoolean()) return result;
            cursor=id(response,"last_id");if(!seen.add(cursor)) throw new InvalidAnalysisOutputException();
        }
        throw new InvalidAnalysisOutputException();
    }
    public Observation inspect(AnalysisJob job) {
        long end=deadline(job);String sessionId=job.sessionId;
        if (sessionId==null) {
            var matches=pages("agents/sessions?order=desc&limit=100",end).stream()
                    .filter(x -> job.id.equals(x.path("metadata").path("crosscheck_job").asString())
                            && agentId.equals(x.path("agent").path("id").asString())).toList();
            if(matches.size()!=1) return Observation.pending(null,null);
            sessionId=id(matches.getFirst(),"id");
            // Session creation already submitted the initial input; reconcile its exact turn below.
        }
        String turnId=job.turnId;
        if(turnId==null) {
            var turns=pages("agents/sessions/"+sessionId+"/turns?order=desc&limit=100",end);
            var items=pages("agents/sessions/"+sessionId+"/items?order=desc&limit=100",end);
            var candidates=new ArrayList<String>();
            for(var turn:turns) {
                String candidate=id(turn,"id");
                if(candidate.equals(job.previousTurnId) || !turn.path("subagent_id").isNull()
                        || !sessionId.equals(turn.path("session_id").asString())) continue;
                boolean matchingInput=items.stream().anyMatch(item -> candidate.equals(item.path("turn_id").asString())
                        && "user".equals(item.path("role").asString()) && item.path("content").isArray()
                        && item.path("content").size()==1 && Objects.equals(job.remoteInputHash==null && job.input!=null ? submissionFingerprint(job) : job.remoteInputHash,
                                hash(item.path("content").get(0).path("text").asString())));
                if(matchingInput) candidates.add(candidate);
            }
            if(candidates.size()!=1) return Observation.pending(sessionId,null);
            turnId=candidates.getFirst();
        }
        var turn=get("agents/sessions/"+sessionId+"/turns/"+turnId,end);
        if(!turnId.equals(id(turn,"id")) || !sessionId.equals(turn.path("session_id").asString()) || !turn.path("subagent_id").isNull())
            throw new InvalidAnalysisOutputException();
        return switch(text(turn,"status")) {
            case "completed" -> {
                if (job.terminal()) yield new Observation(sessionId,turnId,null,null,true);
                try {
                    var original=job.input;
                    var input=new AiAnalysisInput(original.text(),original.category(),original.agentRevision(),original.sessionId(),original.context());
                    yield new Observation(sessionId,turnId,reader.readReport(sessionId,turnId,end,input),null,true);
                } catch(InvalidAnalysisOutputException invalid) {yield new Observation(sessionId,turnId,null,"INVALID_ANALYSIS_OUTPUT",true);}
            }
            case "failed","cancelled" -> new Observation(sessionId,turnId,null,"ANALYSIS_PROVIDER_FAILED",true);
            case "waiting" -> new Observation(sessionId,turnId,null,"ANALYSIS_PROVIDER_ACTION_REQUIRED",false);
            case "queued","in_progress" -> Observation.pending(sessionId,turnId);
            default -> throw new InvalidAnalysisOutputException();
        };
    }
}
