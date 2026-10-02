package com.crosscheck.application.features.analysis.jobs;
import java.time.Duration;
public record JobPolicy(int concurrency, int queueCapacity, Duration queueTimeout,
        Duration executionTimeout, Duration retention, Duration tombstoneRetention, Duration lease) {
    public JobPolicy {
        if (concurrency < 1 || concurrency > 20 || queueCapacity < 1 || queueCapacity > 1000)
            throw new IllegalArgumentException("Invalid job capacity");
        for (var d : new Duration[]{queueTimeout, executionTimeout, retention, tombstoneRetention, lease})
            if (d == null || d.isNegative() || d.isZero()) throw new IllegalArgumentException("Invalid job duration");
        if (retention.compareTo(queueTimeout.plus(executionTimeout)) <= 0
                || tombstoneRetention.compareTo(retention) <= 0 || lease.compareTo(Duration.ofSeconds(30)) <= 0)
            throw new IllegalArgumentException("Inconsistent job durations");
    }
    public static JobPolicy defaults() { return new JobPolicy(2, 20, Duration.ofMinutes(2),
            Duration.ofMinutes(10), Duration.ofHours(24), Duration.ofDays(7), Duration.ofSeconds(60)); }
}
