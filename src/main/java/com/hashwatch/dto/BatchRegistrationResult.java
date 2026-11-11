package com.hashwatch.dto;

import com.hashwatch.entity.WatchedFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Result summary DTO for batch file registration operations.
 */
public class BatchRegistrationResult {

    private List<WatchedFile> registered = new ArrayList<>();
    private List<BatchFailureItem> failed = new ArrayList<>();
    private int totalSubmitted;
    private int successCount;
    private int failureCount;

    public BatchRegistrationResult() {}

    public void addSuccess(WatchedFile file) {
        this.registered.add(file);
        this.successCount = this.registered.size();
    }

    public void addFailure(String filePath, String reason) {
        this.failed.add(new BatchFailureItem(filePath, reason));
        this.failureCount = this.failed.size();
    }

    public List<WatchedFile> getRegistered() {
        return registered;
    }

    public void setRegistered(List<WatchedFile> registered) {
        this.registered = registered;
        this.successCount = (registered != null) ? registered.size() : 0;
    }

    public List<BatchFailureItem> getFailed() {
        return failed;
    }

    public void setFailed(List<BatchFailureItem> failed) {
        this.failed = failed;
        this.failureCount = (failed != null) ? failed.size() : 0;
    }

    public int getTotalSubmitted() {
        return totalSubmitted;
    }

    public void setTotalSubmitted(int totalSubmitted) {
        this.totalSubmitted = totalSubmitted;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public static class BatchFailureItem {
        private String filePath;
        private String reason;

        public BatchFailureItem() {}

        public BatchFailureItem(String filePath, String reason) {
            this.filePath = filePath;
            this.reason = reason;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
