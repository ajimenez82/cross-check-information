package com.crosscheck.bootstrap;

import com.crosscheck.bootstrap.configuration.OpenAiConfiguration;
import com.crosscheck.bootstrap.configuration.ReplayConfiguration;
import com.crosscheck.infrastructure.openai.ReplayPoliticalAnalysisService;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;

class ReplayConfigurationTest {
    private ApplicationContextRunner runner(String... profiles) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profiles))
                .withUserConfiguration(ReplayConfiguration.class, OpenAiConfiguration.class)
                .withPropertyValues("crosscheck.replay.scenario=PLUS_ULTRA_V19",
                        "OPENAI_API_KEY=", "crosscheck.openai.political-analysis-agent-id=");
    }

    @Test void replayNeedsNoCredentialsAndCreatesNoHttpClient() {
        runner("replay").run(context -> {
            assertNull(context.getStartupFailure());
            assertNotNull(context.getBean(ReplayPoliticalAnalysisService.class));
            assertTrue(context.getBeansOfType(HttpClient.class).isEmpty());
        });
    }

    @ParameterizedTest @ValueSource(strings = {"dev", "openai"})
    void conflictingProfilesFailAtStartup(String other) {
        runner("replay", other).run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test void missingScenarioFailsAtStartup() {
        runner("replay").withPropertyValues("crosscheck.replay.scenario=MISSING")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
