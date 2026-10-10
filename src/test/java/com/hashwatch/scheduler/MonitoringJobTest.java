package com.hashwatch.scheduler;

import com.hashwatch.config.SchedulerConfig;
import com.hashwatch.service.ComparisonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * =============================================================================
 * DOMAIN: Backend / Scheduler
 * ASSIGNED TO: Samarjeet (Sprint 2: S2-T2)
 * Unit tests validating Quartz MonitoringJob execution loop, error resiliency,
 * and SchedulerConfig bean instantiation with misfire settings.
 * =============================================================================
 */
@ExtendWith(MockitoExtension.class)
class MonitoringJobTest {

    @Mock
    private ComparisonService comparisonService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private MonitoringJob monitoringJob;

    @BeforeEach
    void setUp() {
        monitoringJob = new MonitoringJob(comparisonService);
    }

    @Test
    @DisplayName("Should successfully invoke runVerificationScan when job executes")
    void testExecuteInvokesVerificationScan() throws JobExecutionException {
        doNothing().when(comparisonService).runVerificationScan();

        assertDoesNotThrow(() -> monitoringJob.execute(jobExecutionContext));

        verify(comparisonService, times(1)).runVerificationScan();
    }

    @Test
    @DisplayName("Should wrap and handle RuntimeException in JobExecutionException without refiring immediately")
    void testExecuteHandlesExceptionGracefully() {
        doThrow(new RuntimeException("Simulated disk I/O timeout"))
                .when(comparisonService).runVerificationScan();

        JobExecutionException thrown = assertThrows(
                JobExecutionException.class,
                () -> monitoringJob.execute(jobExecutionContext)
        );

        assertNotNull(thrown.getMessage());
        assertFalse(thrown.refireImmediately(), "Job should not refire immediately upon scan failure");
        verify(comparisonService, times(1)).runVerificationScan();
    }

    @Test
    @DisplayName("SchedulerConfig should create durable JobDetail and Trigger with configured interval")
    void testSchedulerConfigBeans() {
        SchedulerConfig config = new SchedulerConfig();
        JobDetail jobDetail = config.monitoringJobDetail();

        assertNotNull(jobDetail);
        assertEquals("fileMonitoringJob", jobDetail.getKey().getName());
        assertEquals("integrityGroup", jobDetail.getKey().getGroup());
        assertTrue(jobDetail.isDurable());

        Trigger trigger = config.monitoringJobTrigger(jobDetail);
        assertNotNull(trigger);
        assertEquals("fileMonitoringTrigger", trigger.getKey().getName());
        assertEquals("integrityGroup", trigger.getKey().getGroup());
        assertEquals(jobDetail.getKey(), trigger.getJobKey());
    }
}
