package com.hashwatch.dto;

import java.util.List;

/**
 * Request payload DTO for registering single or multiple files into integrity monitoring.
 */
public class FileRegistrationRequest {

    private String filePath;
    private List<String> filePaths;

    public FileRegistrationRequest() {}

    public FileRegistrationRequest(String filePath) {
        this.filePath = filePath;
    }

    public FileRegistrationRequest(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }
}
