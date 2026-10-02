package com.crosscheck.bootstrap;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.bootstrap.configuration.OpenAiConfiguration;
import com.crosscheck.infrastructure.openai.OpenAiPoliticalAnalysisService;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiConfigurationTest {
    private ApplicationContextRunner runner(String... profiles) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profiles))
                .withUserConfiguration(OpenAiConfiguration.class)
                .withPropertyValues("OPENAI_API_KEY=test-key-not-a-credential", "crosscheck.openai.political-analysis-agent-id=agent_test",
                        "crosscheck.openai.timeout=PT90S", "crosscheck.openai.poll-interval=PT1S",
                        "crosscheck.openai.max-concurrent-requests=2");
    }

    @Test void configuredProfileCreatesAdapterWithoutNetwork() {
        runner("openai").run(context -> {
            assertNull(context.getStartupFailure());
            assertInstanceOf(OpenAiPoliticalAnalysisService.class, context.getBean(AiPoliticalAnalysisService.class));
        });
    }

    @Test void devDoesNotRequireOpenAiCredentials() {
        runner("dev").withPropertyValues("OPENAI_API_KEY=", "crosscheck.openai.political-analysis-agent-id=").run(context -> {
            assertNull(context.getStartupFailure());
            assertFalse(context.containsBean("openAiAnalysisService"));
        });
    }

    @Test void rejectsBothProviderProfiles() {
        runner("dev", "openai").run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test void evidencePipelineRequiresDurableAsyncJobs() {
        runner("openai").withPropertyValues("crosscheck.openai.evidence-pipeline-enabled=true", "crosscheck.analysis.jobs.enabled=false")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPENAI_API_KEY=", "crosscheck.openai.political-analysis-agent-id=", "crosscheck.openai.timeout=PT0S",
            "crosscheck.openai.timeout=PT6M", "crosscheck.openai.poll-interval=PT0S",
            "crosscheck.openai.poll-interval=PT90S", "crosscheck.openai.max-concurrent-requests=0",
            "crosscheck.openai.report-schema-version=0", "crosscheck.openai.report-schema-version=4"})
    void rejectsIncompleteOrInvalidConfiguration(String property) {
        runner("openai").withPropertyValues(property).run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test void secretIsNotIncludedInStartupFailure() {
        String secret = "private invalid key";
        runner("openai").withPropertyValues("OPENAI_API_KEY=" + secret).run(context -> {
            assertNotNull(context.getStartupFailure());
            var output = new StringWriter();
            context.getStartupFailure().printStackTrace(new PrintWriter(output));
            assertFalse(output.toString().contains(secret));
        });
    }
}
