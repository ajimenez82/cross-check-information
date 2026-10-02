package com.crosscheck.bootstrap.configuration;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import com.crosscheck.application.features.analysis.jobs.JobPolicy;
@ConfigurationProperties("crosscheck.analysis.jobs")
public record JobProperties(@DefaultValue("data/cross-check") String databasePath,
        @DefaultValue("2") int concurrency, @DefaultValue("20") int queueCapacity,
        @DefaultValue("PT2M") Duration queueTimeout, @DefaultValue("PT10M") Duration executionTimeout,
        @DefaultValue("PT24H") Duration retention, @DefaultValue("P7D") Duration tombstoneRetention,
        @DefaultValue("PT60S") Duration lease) {
    JobPolicy policy() {return new JobPolicy(concurrency,queueCapacity,queueTimeout,executionTimeout,retention,tombstoneRetention,lease);}
}
