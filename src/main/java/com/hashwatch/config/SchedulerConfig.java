package com.hashwatch.config;

import com.hashwatch.scheduler.MonitoringJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "hashwatch.monitor.scan-enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfig {

    @Value("${hashwatch.monitor.scan-interval-seconds:30}")
    private int scanIntervalSeconds;

    @Bean
    public JobDetail monitoringJobDetail() {
        return JobBuilder.newJob(MonitoringJob.class)
                .withIdentity("fileMonitoringJob", "integrityGroup")
                .withDescription("Scheduled file integrity verification job")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger monitoringJobTrigger(JobDetail monitoringJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(monitoringJobDetail)
                .withIdentity("fileMonitoringTrigger", "integrityGroup")
                .withDescription("Trigger for file integrity verification")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(scanIntervalSeconds)
                        .repeatForever())
                .build();
    }
}
