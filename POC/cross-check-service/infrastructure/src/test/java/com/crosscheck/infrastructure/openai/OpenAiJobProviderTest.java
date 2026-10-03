package com.crosscheck.infrastructure.openai;
import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.model.*;
import com.crosscheck.domain.analysis.AnalysisCategory;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.crosscheck.infrastructure.openai.OpenAiJson.MAPPER;
class OpenAiJobProviderTest {
    HttpServer server;
    HttpClient client;
    JobProvider provider;
    int creates,submissions,sourceReads;
    String completeOutput;
    tools.jackson.databind.JsonNode submittedAgent;
    String submittedText,submittedKey,correlation;
    boolean loseResponse,invalidOutput,missingInput;
    AnalysisJob job;
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/",exchange->{
            String path=exchange.getRequestURI().getPath();Object response;int status=200;
            var bytes=exchange.getRequestBody().readAllBytes();
            if("POST".equals(exchange.getRequestMethod()) && path.endsWith("/sessions")) {
                creates++;var request=MAPPER.readTree(bytes);assertTrue(request.has("input"));
                submittedAgent=request.path("agent");
                submittedText=request.path("input").asString();submittedKey=exchange.getRequestHeaders().getFirst("Idempotency-Key");
                correlation=request.path("metadata").path("crosscheck_job").asString();
                response=Map.of("id","sess_test");if(loseResponse)status=503;
            } else if("POST".equals(exchange.getRequestMethod())) {
                submissions++;submittedKey=exchange.getRequestHeaders().getFirst("Idempotency-Key");
                submittedText=MAPPER.readTree(bytes).path("events").get(0).path("input").get(0).path("content").get(0).path("text").asString();
                response=Map.of();if(loseResponse)status=503;
            } else if(path.endsWith("/sessions")) {
                var sessions=new ArrayList<Object>();
                var metadata=new LinkedHashMap<String,Object>();metadata.put("crosscheck_job",correlation);
                if(job.stage!=null) {
                    metadata.put("crosscheck_stage",job.stage);
                    sessions.add(Map.of("id","sess_old","agent",Map.of("id","agent_test"),"metadata",Map.of("crosscheck_job",correlation,"crosscheck_stage","RESEARCH")));
                }
                sessions.add(Map.of("id","sess_test","status","idle","agent",Map.of("id","agent_test"),"metadata",metadata));
                response=Map.of("data",sessions,"has_more",false);
            } else if(path.endsWith("/turns")) {
                response=Map.of("data",List.of(turn()),"has_more",false);
            } else if(path.endsWith("/turn_test")) {
                response=turn();
            } else if(path.endsWith("/items")) {
                var items=new ArrayList<Object>();
                if(!missingInput)items.add(Map.of("id","input_test","type","message","role","user","turn_id","turn_test",
                        "content",List.of(Map.of("type","input_text","text",submittedText))));
                String output=completeOutput!=null?completeOutput:invalidOutput?"{}":MAPPER.writeValueAsString(new LinkedHashMap<String,Object>() {{
                    put("schemaVersion","3");put("analysis",null);put("clarification",Map.of("question","¿Qué periodo?","reason","MISSING_PERIOD"));
                }});
                items.add(Map.of("id","output_test","type","message","role","assistant","turn_id","turn_test","status","completed",
                        "phase","final_answer","content",List.of(Map.of("type","output_text","text",output))));
                response=Map.of("data",items,"has_more",false);
            } else if(path.endsWith("/source")) {sourceReads++;status=403;response=Map.of();}
            else response=Map.of("id","sess_test","status","idle");
            byte[] body=MAPPER.writeValueAsBytes(response);exchange.sendResponseHeaders(status,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();client=HttpClient.newHttpClient();
        var service=new OpenAiPoliticalAnalysisService(client,URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v1/"),"offline-key","agent_test",Duration.ofSeconds(90),Duration.ofSeconds(1),2,3);
        provider=service.jobProvider("political-v9");job=new AnalysisJob();job.id=UUID.randomUUID().toString();job.phase=AnalysisJob.Phase.CREATE_SESSION;
        job.input=new AiAnalysisInput("Consulta",AnalysisCategory.POLITICAL_ANALYSIS,"political-v9",null);
    }
    Map<String,Object> turn() {
        var turn=new LinkedHashMap<String,Object>();turn.put("id","turn_test");turn.put("session_id","sess_test");turn.put("subagent_id",null);turn.put("status","completed");return turn;
    }
    @AfterEach void close(){server.stop(0);client.close();}
    void prepare(){var remote=provider.prepare(job);job.sessionId=remote.sessionId();job.previousTurnId=remote.previousTurnId();}
    @Test void startsInitialInferenceDuringCreationAndReadsExactResult() {
        prepare();assertTrue(provider.submitsDuringPrepare());assertEquals(0,submissions);assertEquals(job.id,correlation);
        assertEquals("crosscheck-"+job.id,submittedKey);assertEquals(job.id,MAPPER.readTree(submittedText).path("analysisRequestId").asString());
        var result=provider.inspect(job);assertTrue(result.remoteTerminal());assertEquals("turn_test",result.turnId());
        assertEquals(Clarification.Reason.MISSING_PERIOD,result.result().clarification().reason());assertEquals(1,creates);assertEquals(0,submissions);
        assertTrue(submittedAgent.path("instructions").asString().contains("ONE execution"));
        assertEquals("web_search",submittedAgent.path("tools").get(0).path("type").asString());
        assertTrue(submittedAgent.path("text").path("format").path("schema").path("properties").has("analysis"));
    }

    @Test void completeReportPreservesProviderSourcesWithoutDownloadingThem() throws Exception {
        tools.jackson.databind.node.ObjectNode report;
        try(var input=getClass().getResourceAsStream("/openai/claim-report-example.json")) {
            report=(tools.jackson.databind.node.ObjectNode)MAPPER.readTree(input.readAllBytes());
        }
        String text="";
        for(var claim:report.path("claimAnalysis").path("claims")) text+=claim.path("inputExcerpt").asString()+" ";
        job.input=new AiAnalysisInput(text.trim(),AnalysisCategory.POLITICAL_ANALYSIS,"political-v16",null);
        String sourceUrl="http://127.0.0.1:"+server.getAddress().getPort()+"/v1/source";
        ((tools.jackson.databind.node.ObjectNode)report.path("sources").get(0)).put("url",sourceUrl);
        var envelope=new LinkedHashMap<String,Object>();envelope.put("schemaVersion","3");envelope.put("analysis",report);envelope.put("clarification",null);
        completeOutput=MAPPER.writeValueAsString(envelope);
        prepare();var result=provider.inspect(job);
        assertNotNull(result.result());assertNull(result.failure());
        assertEquals(report.path("sources").size(),result.result().report().sources().size());
        assertEquals(sourceUrl,result.result().report().sources().getFirst().url().toString());
        assertEquals(0,sourceReads);assertEquals(1,creates);assertEquals(0,submissions);
    }
    @Test void followUpUsesEventsWithStableIdempotencyKey() {
        job.sessionId="sess_test";
        prepare();assertEquals(0,creates);assertEquals("turn_test",job.previousTurnId);
        provider.submit(job);assertEquals(1,submissions);assertEquals("crosscheck-"+job.id,submittedKey);
    }
    @Test void lostInitialPostResponseCanBeRecoveredWithoutAnotherPost() {
        loseResponse=true;assertThrows(RuntimeException.class,()->provider.prepare(job));
        assertNotNull(provider.inspect(job).result());assertEquals(1,creates);assertEquals(0,submissions);
    }
    @Test void missingInputCorrelationNeverUsesLatestTurnByGuessing() {
        prepare();missingInput=true;assertNull(provider.inspect(job).result());assertNull(provider.inspect(job).turnId());
    }
    @Test void invalidOutputIsTerminalWithoutRepairOrResubmission() {
        prepare();invalidOutput=true;var result=provider.inspect(job);
        assertEquals("INVALID_ANALYSIS_OUTPUT",result.failure());assertTrue(result.remoteTerminal());assertEquals(1,creates);assertEquals(0,submissions);
    }
    @Test void lostSessionResponseRecoversTheSubmittedTurnByMetadataAndInput() {
        provider.prepare(job);assertNull(job.sessionId);var recovered=provider.inspect(job);
        assertEquals("sess_test",recovered.sessionId());assertEquals("turn_test",recovered.turnId());assertNotNull(recovered.result());assertEquals(0,submissions);assertEquals(1,creates);
    }

    @Test void retiredLegacySubmissionReconcilesOnlyItsOwnRemoteStage() {
        job.stage="SYNTHESIS";provider.prepare(job);job.status=AnalysisJob.Status.FAILED;
        var recovered=provider.inspect(job);
        assertEquals("sess_test",recovered.sessionId());assertTrue(recovered.remoteTerminal());
        assertNull(recovered.result());assertEquals(1,creates);assertEquals(0,submissions);
    }
}
