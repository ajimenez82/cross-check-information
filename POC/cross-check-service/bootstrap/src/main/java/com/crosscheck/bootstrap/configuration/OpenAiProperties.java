package com.crosscheck.bootstrap.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("crosscheck.openai")
public record OpenAiProperties(String politicalAnalysisAgentId, Duration timeout, Duration pollInterval,
                               int maxConcurrentRequests,
                               @org.springframework.boot.context.properties.bind.DefaultValue("3") int reportSchemaVersion) {}
