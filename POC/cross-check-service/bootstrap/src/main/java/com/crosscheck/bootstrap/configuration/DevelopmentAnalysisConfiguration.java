package com.crosscheck.bootstrap.configuration;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.infrastructure.development.DevelopmentPoliticalAnalysisService;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration(proxyBeanMethods = false)
@Profile("dev")
@EnableConfigurationProperties(DevelopmentProperties.class)
public class DevelopmentAnalysisConfiguration {
    @Bean
    AiPoliticalAnalysisService developmentAnalysisService(DevelopmentProperties properties, Clock clock) {
        log.warn("Development analysis enabled: simulated responses only, scenario={}", properties.scenario());
        return new DevelopmentPoliticalAnalysisService(properties.scenario(), clock);
    }

}
