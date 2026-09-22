package com.crosscheck.bootstrap.configuration;

import com.crosscheck.infrastructure.development.DevelopmentScenario;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("crosscheck.development")
public record DevelopmentProperties(@NotNull DevelopmentScenario scenario) {}
