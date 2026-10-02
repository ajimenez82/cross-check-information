package com.crosscheck.bootstrap.configuration;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.application.contracts.ConversationReferenceCodec;
import com.crosscheck.application.features.analysis.start.AnalysisPolicy;
import com.crosscheck.application.features.analysis.start.StartAnalysisHandler;
import com.crosscheck.presentation.api.analysis.start.StartAnalysisController;
import java.time.Clock;
import com.crosscheck.application.contracts.ConversationContexts;
import com.crosscheck.application.features.analysis.start.ConversationContextStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile({"dev", "openai"})
@EnableConfigurationProperties(AnalysisProperties.class)
public class AnalysisConfiguration {
    @Bean Clock analysisClock() { return Clock.systemUTC(); }

    @Bean
    AnalysisPolicy analysisPolicy(AnalysisProperties properties) {
        return new AnalysisPolicy(properties.maxInputLength(), properties.maxTokenLength(),
                properties.agentRevision(), properties.conversationTtl());
    }

    @Bean
    @ConditionalOnProperty(name="crosscheck.analysis.jobs.enabled",havingValue="false",matchIfMissing=true)
    ConversationContexts memoryConversationContexts(Clock clock) { return new ConversationContextStore(clock,1000); }

    @Bean
    StartAnalysisHandler startAnalysisHandler(AiPoliticalAnalysisService provider,
            ConversationReferenceCodec codec, AnalysisPolicy policy, Clock clock, ConversationContexts contexts) {
        return new StartAnalysisHandler(provider, codec, policy, clock, contexts);
    }

    @Bean
    StartAnalysisController startAnalysisController(StartAnalysisHandler handler, org.springframework.core.env.Environment environment) {
        return new StartAnalysisController(handler, environment.getProperty("crosscheck.analysis.jobs.enabled", Boolean.class, false));
    }
}
