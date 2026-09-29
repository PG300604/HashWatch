package com.hashwatch.scheduler;

import com.hashwatch.service.ComparisonService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * =============================================================================
 * DOMAIN: Backend
 * ASSIGNED TO: Samarjeet (Sprint 2)
 * FOLDER / TARGET: src/main/java/com/hashwatch/scheduler/MonitoringJob.java
 * DOC TO UPDATE: docs/TRD.md (Section 1 & 2)
 * =============================================================================
 *
 * Task Description:
 * Quartz Job triggered periodically to execute file integrity verification scans.
 */
@Component
public class MonitoringJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(MonitoringJob.class);

    private final ComparisonService comparisonService;

    public MonitoringJob(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Quartz MonitoringJob firing... (Scheduled periodic execution)");
        // TODO [Sprint 2 - Backend]: Assigned to Samarjeet
        // 1. Invoke comparisonService.runVerificationScan()
        // 2. Catch and handle any transient I/O exceptions so the scheduler remains resilient
    }
}
