package com.hashwatch.scheduler;

import com.hashwatch.service.ComparisonService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MonitoringJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(MonitoringJob.class);

    private final ComparisonService comparisonService;

    public MonitoringJob(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Quartz MonitoringJob triggered: scanning files for integrity validation");
        try {
            comparisonService.runVerificationScan();
        } catch (Exception e) {
            log.error("Error occurred while executing MonitoringJob: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
}
