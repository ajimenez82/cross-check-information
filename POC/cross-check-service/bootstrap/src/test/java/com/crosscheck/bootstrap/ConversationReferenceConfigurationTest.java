package com.crosscheck.bootstrap;

import com.crosscheck.application.contracts.ConversationReferenceCodec;
import com.crosscheck.application.features.analysis.start.AnalysisPolicy;
import com.crosscheck.bootstrap.configuration.ConversationReferenceConfiguration;
import java.time.Clock;
import java.time.Duration;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import static org.junit.jupiter.api.Assertions.*;

class ConversationReferenceConfigurationTest {
    private ApplicationContextRunner runner(String secret) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
                .withUserConfiguration(Dependencies.class, ConversationReferenceConfiguration.class)
                .withPropertyValues("CONVERSATION_TOKEN_SECRET=" + secret);
    }

    @Test
    void missingKeyPreventsStartup() {
        runner("").run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void invalidKeyPreventsStartupWithoutLeakingValue() {
        String secret = "invalid-private-secret";
        runner(secret).run(context -> {
            assertNotNull(context.getStartupFailure());
            var text = new StringWriter();
            context.getStartupFailure().printStackTrace(new PrintWriter(text));
            assertFalse(text.toString().contains(secret));
        });
    }

    @Test
    void validKeyCreatesCodec() {
        String secret = java.util.Base64.getEncoder().encodeToString(
                new java.security.SecureRandom().generateSeed(32));
        runner(secret).run(context -> {
            assertNull(context.getStartupFailure());
            assertNotNull(context.getBean(ConversationReferenceCodec.class));
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dependencies {
        @Bean Clock clock() { return Clock.systemUTC(); }
        @Bean AnalysisPolicy policy() {
            return new AnalysisPolicy(10000, 4096, "v1", Duration.ofHours(2));
        }
    }
}
