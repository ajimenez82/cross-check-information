package com.crosscheck.bootstrap.configuration;

import com.crosscheck.infrastructure.openai.OpenAiPoliticalAnalysisService;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration(proxyBeanMethods = false)
@Profile("openai")
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiConfiguration {
    @Bean(destroyMethod = "close")
    HttpClient openAiHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Bean
    OpenAiPoliticalAnalysisService openAiAnalysisService(Environment environment,
            OpenAiProperties properties, HttpClient openAiHttpClient) {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            throw new IllegalStateException("Choose either dev or openai, not both.");
        }
        // Keep secrets outside configuration binding diagnostics and record toString methods.
        return new OpenAiPoliticalAnalysisService(openAiHttpClient,
                environment.getProperty("OPENAI_API_KEY"), properties.politicalAnalysisAgentId(), properties.timeout(),
                properties.pollInterval(), properties.maxConcurrentRequests());
    }
}
