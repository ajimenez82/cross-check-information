package com.crosscheck.bootstrap.configuration;
import com.crosscheck.application.features.analysis.jobs.AnalysisJobWorker;
import java.util.concurrent.*;
import org.slf4j.LoggerFactory;
/** Graceful shutdown never interrupts an embedded database transaction. */
final class JobScheduler implements AutoCloseable {
    private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor();
    JobScheduler(AnalysisJobWorker worker) {
        executor.scheduleWithFixedDelay(() -> {
            try {worker.tick();}
            catch(RuntimeException failure) {LoggerFactory.getLogger(JobScheduler.class).warn("Analysis job checkpoint failed: type={}",failure.getClass().getSimpleName());}
        },0,1,TimeUnit.SECONDS);
    }
    @Override public void close() {
        executor.shutdown();
        try {if(!executor.awaitTermination(60,TimeUnit.SECONDS)) throw new IllegalStateException("Job scheduler did not stop");}
        catch(InterruptedException interrupted) {Thread.currentThread().interrupt();throw new IllegalStateException("Interrupted during scheduler shutdown");}
    }
}
