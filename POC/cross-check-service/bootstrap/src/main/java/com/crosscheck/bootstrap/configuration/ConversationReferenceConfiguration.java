package com.crosscheck.bootstrap.configuration;

import com.crosscheck.application.contracts.ConversationReferenceCodec;
import com.crosscheck.application.features.analysis.start.AnalysisPolicy;
import com.crosscheck.infrastructure.conversation.EncryptedConversationReferenceCodec;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
public class ConversationReferenceConfiguration {
    @Bean
    ConversationReferenceCodec conversationReferenceCodec(Environment environment, Clock clock, AnalysisPolicy policy) {
        // Read directly to avoid binding diagnostics that could print a rejected secret value.
        return new EncryptedConversationReferenceCodec(environment.getProperty("CONVERSATION_TOKEN_SECRET"),
                clock, policy.maxTokenLength());
    }
}
