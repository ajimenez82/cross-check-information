package com.crosscheck.bootstrap.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("crosscheck.analysis")
public record AnalysisProperties(@Min(1) int maxInputLength, @Min(512) int maxTokenLength,
                                 @NotBlank String agentRevision, @NotNull Duration conversationTtl) {}
