package com.crosscheck.bootstrap.configuration;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.application.contracts.ConversationReferenceCodec;
import com.crosscheck.application.features.analysis.start.AnalysisPolicy;
import com.crosscheck.application.features.analysis.start.StartAnalysisHandler;
import com.crosscheck.infrastructure.development.DevelopmentPoliticalAnalysisService;
import com.crosscheck.presentation.api.analysis.start.StartAnalysisController;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration(proxyBeanMethods = false)
@Profile("dev")
@EnableConfigurationProperties({AnalysisProperties.class, DevelopmentProperties.class})
public class DevelopmentAnalysisConfiguration {
    @Bean
    Clock analysisClock() {
        return Clock.systemUTC();
    }

    @Bean
    AnalysisPolicy analysisPolicy(AnalysisProperties properties) {
        return new AnalysisPolicy(properties.maxInputLength(), properties.maxTokenLength(),
                properties.agentRevision(), properties.conversationTtl());
    }

    @Bean
    AiPoliticalAnalysisService developmentAnalysisService(DevelopmentProperties properties, Clock clock) {
        log.warn("Development analysis enabled: simulated responses only, scenario={}", properties.scenario());
        return new DevelopmentPoliticalAnalysisService(properties.scenario(), clock);
    }

    @Bean
    StartAnalysisHandler startAnalysisHandler(AiPoliticalAnalysisService provider,
            ConversationReferenceCodec codec, AnalysisPolicy policy, Clock clock) {
        return new StartAnalysisHandler(provider, codec, policy, clock);
    }

    @Bean
    StartAnalysisController startAnalysisController(StartAnalysisHandler handler) {
        return new StartAnalysisController(handler);
    }
}
