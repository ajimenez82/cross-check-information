package com.crosscheck.bootstrap;
import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.infrastructure.jobs.JdbcJobStore;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "crosscheck.analysis.jobs.enabled=true","crosscheck.analysis.jobs.worker-enabled=false"})
@ActiveProfiles("dev")
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class AnalysisJobsApiTest {
    static final Path database=Path.of(System.getProperty("java.io.tmpdir"),"crosscheck-jobs-api-"+UUID.randomUUID(),"jobs");
    static final String secret=Base64.getEncoder().encodeToString(new byte[32]);
    static final String credential=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    static final JsonMapper json=JsonMapper.builder().build();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("CONVERSATION_TOKEN_SECRET",()->secret);
        registry.add("crosscheck.analysis.jobs.database-path",()->database.toString());
    }
    @LocalServerPort int port;
    @Autowired AnalysisJobWorker worker;
    @Autowired JdbcJobStore store;
    HttpResponse<String> send(String method,String path,String key,String auth,String body) throws Exception {
        try(var client=HttpClient.newHttpClient()) {
            var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
            if(key!=null)builder.header("Idempotency-Key",key);
            if(auth!=null)builder.header("Authorization","Bearer "+auth);
            if(body==null)builder.GET();else builder.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(body));
            return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
        }
    }
    @Test void acceptsPollsAndRecoversSameResultWithoutExposingInternalState() throws Exception {
        String key=UUID.randomUUID().toString();String body="{\"text\":\"Consulta de prueba\",\"conversationToken\":null}";
        var start=send("POST","/api/analysis/jobs",key,credential,body);assertEquals(202,start.statusCode());
        assertEquals("no-store",start.headers().firstValue("Cache-Control").orElseThrow());
        var accepted=json.readTree(start.body());String id=accepted.path("analysisId").asString();
        assertEquals("QUEUED",accepted.path("status").asString());assertTrue(accepted.path("result").isNull());
        String location=start.headers().firstValue("Location").orElseThrow();
        for(int i=0;i<4;i++) {
            store.transact(tx->{var job=tx.find(id);job.nextPollAt=Instant.EPOCH;tx.save(job);return null;});worker.tick();
        }
        var completed=send("GET",location,null,credential,null);assertEquals(200,completed.statusCode());
        var result=json.readTree(completed.body());assertEquals("COMPLETED",result.path("status").asString());
        assertTrue(result.path("result").path("analysis").isObject());
        for(String field:List.of("input","accessHash","sessionId","turnId","leaseOwner","providerIdentity"))assertFalse(result.has(field));
        var replay=send("POST","/api/analysis/jobs",key,credential,body);assertEquals(200,replay.statusCode());
        assertEquals(result,json.readTree(replay.body()));
        assertEquals(409,send("POST","/api/analysis/jobs",key,credential,"{\"text\":\"Different\"}").statusCode());
        assertEquals(401,send("GET",location,null,null,null).statusCode());
        assertEquals(404,send("GET",location,null,credential.replaceFirst("A","B"),null).statusCode());
    }
    @Test void syncRouteCannotBypassAsyncReservations() throws Exception {
        var response=send("POST","/api/analysis/start",null,null,"{\"text\":\"Offline test\"}");
        assertEquals(409,response.statusCode());assertEquals("ASYNC_ANALYSIS_REQUIRED",json.readTree(response.body()).path("code").asString());
    }
    @Test void rejectsInvalidKeysBeforeAdmission() throws Exception {
        var response=send("POST","/api/analysis/jobs","bad",credential,"{\"text\":\"Offline test\"}");
        assertEquals(400,response.statusCode());assertEquals("INVALID_IDEMPOTENCY_KEY",json.readTree(response.body()).path("code").asString());
    }
}
