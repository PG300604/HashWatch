package com.hashwatch.scheduler;

import com.hashwatch.service.ComparisonService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * =============================================================================
 * DOMAIN: Backend / Scheduler
 * ASSIGNED TO: Samarjeet (Sprint 2: S2-T2)
 * FOLDER / TARGET: src/main/java/com/hashwatch/scheduler/MonitoringJob.java
 * DOC TO UPDATE: docs/TRD.md (Section 2.1)
 * =============================================================================
 *
 * Quartz Job triggered periodically to execute file integrity verification scans.
 *
 * Resilience & Concurrency Features:
 * 1. @DisallowConcurrentExecution ensures that if a scan takes longer than the
 *    configured interval (e.g. large file sets or slow I/O), subsequent trigger
 *    firings will wait until the active scan completes rather than running in parallel.
 * 2. High-precision execution duration logging for operational telemetry.
 * 3. Catches and logs all unhandled exceptions, wrapping them into JobExecutionException
 *    with setRefireImmediately(false) to prevent runaway failure loops.
 */
@Component
@DisallowConcurrentExecution
public class MonitoringJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(MonitoringJob.class);

    private final ComparisonService comparisonService;

    /**
     * Constructs the monitoring job with the required comparison service dependency.
     *
     * @param comparisonService the core integrity verification engine
     */
    public MonitoringJob(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    /**
     * Executes the periodic file verification scan.
     *
     * @param context the Quartz execution context
     * @throws JobExecutionException if an unrecoverable failure occurs during execution
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Quartz MonitoringJob firing: executing scheduled file integrity verification scan");
        long startTime = System.currentTimeMillis();

        try {
            // Invoke the core integrity comparison engine across all active watched files
            comparisonService.runVerificationScan();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Quartz MonitoringJob completed successfully in {} ms", duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            // Log with high severity, but catch to prevent uncaught exceptions from killing the Quartz scheduler thread
            log.error("Unhandled exception during file verification scan execution after {} ms: {}",
                    duration, e.getMessage(), e);

            // Re-wrap in JobExecutionException without refiring immediately to avoid tight failure loops
            JobExecutionException jobEx = new JobExecutionException("File integrity verification scan encountered an error", e);
            jobEx.setRefireImmediately(false);
            throw jobEx;
        }
    }
}
