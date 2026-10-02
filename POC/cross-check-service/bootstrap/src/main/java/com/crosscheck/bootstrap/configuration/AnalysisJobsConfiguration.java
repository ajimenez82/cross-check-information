package com.crosscheck.bootstrap.configuration;
import com.crosscheck.application.contracts.*;
import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.features.analysis.start.*;
import com.crosscheck.infrastructure.jobs.JdbcJobStore;
import com.crosscheck.infrastructure.openai.OpenAiPoliticalAnalysisService;
import com.crosscheck.infrastructure.development.DevelopmentJobProvider;
import com.crosscheck.presentation.api.analysis.jobs.AnalysisJobsController;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
@Configuration(proxyBeanMethods=false)
@Profile({"dev","openai"})
@ConditionalOnProperty(name="crosscheck.analysis.jobs.enabled",havingValue="true")
@EnableConfigurationProperties(JobProperties.class)
public class AnalysisJobsConfiguration {
    @Bean(destroyMethod="close") JdbcJobStore jobStore(JobProperties properties,Clock clock) {return new JdbcJobStore(Path.of(properties.databasePath()),clock);}
    @Bean JobPolicy jobPolicy(JobProperties properties) {return properties.policy();}
    @Bean JobProvider jobProvider(AiPoliticalAnalysisService service,AnalysisPolicy policy,Clock clock,org.springframework.core.env.Environment environment) {
        if (service instanceof OpenAiPoliticalAnalysisService openai) {
            return environment.getProperty("crosscheck.openai.evidence-pipeline-enabled",Boolean.class,false)
                    ? openai.evidenceJobProvider(policy.agentRevision(),clock) : openai.jobProvider(policy.agentRevision());
        }
        return new DevelopmentJobProvider(service,policy.agentRevision());
    }
    @Bean AnalysisJobs analysisJobs(JdbcJobStore store,StartAnalysisHandler handler,ConversationReferenceCodec codec,
            JobProvider provider,JobPolicy policy,Clock clock) {return new AnalysisJobs(store,handler,codec,provider,policy,clock);}
    @Bean AnalysisJobWorker analysisJobWorker(JdbcJobStore store,JobProvider provider,StartAnalysisHandler handler,JobPolicy policy,Clock clock) {
        return new AnalysisJobWorker(store,provider,handler,policy,clock);
    }
    @Bean AnalysisJobsController analysisJobsController(AnalysisJobs jobs) {return new AnalysisJobsController(jobs);}
    @Bean(destroyMethod="close")
    @ConditionalOnProperty(name="crosscheck.analysis.jobs.worker-enabled",havingValue="true",matchIfMissing=true)
    JobScheduler jobScheduler(AnalysisJobWorker worker) {return new JobScheduler(worker);}
}
