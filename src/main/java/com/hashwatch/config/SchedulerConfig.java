package com.hashwatch.config;

import com.hashwatch.scheduler.MonitoringJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * =============================================================================
 * DOMAIN: Backend / Scheduler
 * ASSIGNED TO: Samarjeet (Sprint 2: S2-T2)
 * FOLDER / TARGET: src/main/java/com/hashwatch/config/SchedulerConfig.java
 * DOC TO UPDATE: docs/TRD.md (Section 2.1)
 * =============================================================================
 *
 * Spring configuration class that initializes Quartz Scheduler components for
 * automated periodic file integrity verification scans.
 *
 * Features:
 * 1. Conditional activation based on 'hashwatch.monitor.scan-enabled' property.
 * 2. Configurable scan interval (default: 30 seconds).
 * 3. Durable JobDetail definition bound to the 'integrityGroup'.
 * 4. Resilient Trigger with misfire handling policy preventing execution bursts.
 */
@Configuration
@ConditionalOnProperty(name = "hashwatch.monitor.scan-enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfig {

    /**
     * Periodic scan interval in seconds configured via application.properties.
     * Defaults to 30 seconds if not specified.
     */
    @Value("${hashwatch.monitor.scan-interval-seconds:30}")
    private int scanIntervalSeconds;

    /**
     * Defines the durable Quartz JobDetail metadata for the file integrity monitoring job.
     *
     * @return configured JobDetail instance
     */
    @Bean
    public JobDetail monitoringJobDetail() {
        return JobBuilder.newJob(MonitoringJob.class)
                .withIdentity("fileMonitoringJob", "integrityGroup")
                .withDescription("Scheduled file integrity verification job")
                // Store durably so the job definition remains registered even without active triggers
                .storeDurably()
                // Disable automatic recovery re-execution on crash to avoid redundant scan storms
                .requestRecovery(false)
                .build();
    }

    /**
     * Defines the periodic Trigger that fires the monitoring JobDetail at fixed intervals.
     *
     * @param monitoringJobDetail the JobDetail bean to associate with this trigger
     * @return configured Trigger instance
     */
    @Bean
    public Trigger monitoringJobTrigger(JobDetail monitoringJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(monitoringJobDetail)
                .withIdentity("fileMonitoringTrigger", "integrityGroup")
                .withDescription("Trigger for periodic file integrity verification")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(scanIntervalSeconds)
                        .repeatForever()
                        // If system sleeps or execution lags, advance to next fire time instead of backlogged burst
                        .withMisfireHandlingInstructionNextWithExistingCount())
                .build();
    }
}
