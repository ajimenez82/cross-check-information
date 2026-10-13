package com.crosscheck.bootstrap.configuration;

import com.crosscheck.infrastructure.openai.ReplayPoliticalAnalysisService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration(proxyBeanMethods = false)
@Profile("replay")
public class ReplayConfiguration {
    @Bean
    ReplayPoliticalAnalysisService replayAnalysisService(Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("dev", "openai")))
            throw new IllegalStateException("Use replay without dev or openai.");
        return new ReplayPoliticalAnalysisService(
                environment.getRequiredProperty("crosscheck.replay.scenario"));
    }
}
