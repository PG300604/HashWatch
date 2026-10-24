package com.hashwatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.FileStatus;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.ComparisonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * =============================================================================
 * DOMAIN: API
 * ASSIGNED TO: Riya (Sprint 3: S3-T1)
 * TARGET: src/test/java/com/hashwatch/controller/FileControllerTest.java
 * =============================================================================
 *
 * MockMvc integration tests for {@link FileController} REST endpoints:
 * - Listing all and active monitored files (GET /api/files)
 * - Single file lookup by ID (GET /api/files/{id})
 * - File registration and baseline creation (POST /api/files)
 * - Error cases: 400 Bad Request (missing/invalid path), 409 Conflict (duplicate)
 * - Soft-delete deactivation (DELETE /api/files/{id})
 */
@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WatchedFileRepository watchedFileRepository;

    @MockBean
    private ComparisonService comparisonService;

    @Test
    @DisplayName("GET /api/files returns 200 OK with list of all watched files")
    void testListFilesReturnsAllFiles() throws Exception {
        WatchedFile f1 = new WatchedFile("/etc/passwd", 2048L, LocalDateTime.now(), FileStatus.VERIFIED);
        f1.setId(1L);
        WatchedFile f2 = new WatchedFile("/etc/hosts", 512L, LocalDateTime.now(), FileStatus.TAMPERED);
        f2.setId(2L);

        when(watchedFileRepository.findAll()).thenReturn(List.of(f1, f2));

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].filePath").value("/etc/passwd"))
                .andExpect(jsonPath("$[1].filePath").value("/etc/hosts"));

        verify(watchedFileRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /api/files?activeOnly=true returns only active files")
    void testListFilesActiveOnly() throws Exception {
        WatchedFile activeFile = new WatchedFile("/etc/shadow", 1024L, LocalDateTime.now(), FileStatus.VERIFIED);
        activeFile.setId(1L);
        activeFile.setActive(true);

        when(watchedFileRepository.findByActiveTrue()).thenReturn(List.of(activeFile));

        mockMvc.perform(get("/api/files?activeOnly=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].filePath").value("/etc/shadow"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(watchedFileRepository, times(1)).findByActiveTrue();
    }

    @Test
    @DisplayName("GET /api/files/{id} returns 200 OK when file exists")
    void testGetFileByIdFound() throws Exception {
        WatchedFile file = new WatchedFile("/etc/sudoers", 1024L, LocalDateTime.now(), FileStatus.VERIFIED);
        file.setId(5L);

        when(watchedFileRepository.findById(5L)).thenReturn(Optional.of(file));

        mockMvc.perform(get("/api/files/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.filePath").value("/etc/sudoers"))
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    @DisplayName("GET /api/files/{id} returns 404 Not Found when file does not exist")
    void testGetFileByIdNotFound() throws Exception {
        when(watchedFileRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/files/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/files returns 400 Bad Request when filePath is missing")
    void testRegisterFileMissingPayload() throws Exception {
        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Missing required 'filePath' parameter in request body"));
    }

    @Test
    @DisplayName("POST /api/files returns 400 Bad Request when filePath is blank")
    void testRegisterFileBlankPath() throws Exception {
        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filePath\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("File path must not be empty or whitespace"));
    }

    @Test
    @DisplayName("POST /api/files returns 400 Bad Request when file does not exist on disk")
    void testRegisterFileNonExistentOnDisk() throws Exception {
        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filePath\": \"/non/existent/disk/path/config.yaml\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("File does not exist on disk")));
    }

    @Test
    @DisplayName("POST /api/files returns 409 Conflict when file is already being watched")
    void testRegisterFileConflictAlreadyWatched(@TempDir Path tempDir) throws Exception {
        Path existingFile = Files.createFile(tempDir.resolve("server.conf"));
        String normalizedPath = existingFile.toAbsolutePath().normalize().toString().replace('\\', '/');

        WatchedFile alreadyWatched = new WatchedFile(normalizedPath, 100L, LocalDateTime.now(), FileStatus.VERIFIED);
        alreadyWatched.setId(10L);
        alreadyWatched.setActive(true);

        when(watchedFileRepository.findByFilePath(normalizedPath)).thenReturn(Optional.of(alreadyWatched));

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("filePath", existingFile.toString()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.fileId").value(10));
    }

    @Test
    @DisplayName("POST /api/files returns 201 Created and establishes baseline on valid registration")
    void testRegisterFileSuccess(@TempDir Path tempDir) throws Exception {
        Path newDiskFile = Files.createFile(tempDir.resolve("application.yml"));
        Files.writeString(newDiskFile, "server:\n  port: 8080\n");
        String normalizedPath = newDiskFile.toAbsolutePath().normalize().toString().replace('\\', '/');

        when(watchedFileRepository.findByFilePath(normalizedPath)).thenReturn(Optional.empty());

        WatchedFile savedFile = new WatchedFile(normalizedPath, Files.size(newDiskFile), LocalDateTime.now(), FileStatus.VERIFIED);
        savedFile.setId(42L);
        savedFile.setActive(true);
        when(watchedFileRepository.save(any(WatchedFile.class))).thenReturn(savedFile);

        BaselineEntry mockBaseline = new BaselineEntry(savedFile,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "dGVzdC1zaWduYXR1cmU=",
                "key-fingerprint-123");
        mockBaseline.setId(101L);
        when(comparisonService.establishBaseline(any(WatchedFile.class))).thenReturn(mockBaseline);

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("filePath", newDiskFile.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("successfully registered")))
                .andExpect(jsonPath("$.file.id").value(42))
                .andExpect(jsonPath("$.baseline.sha256Hash").value("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));

        verify(comparisonService, times(1)).establishBaseline(any(WatchedFile.class));
    }

    @Test
    @DisplayName("DELETE /api/files/{id} returns 200 OK and deactivates file")
    void testUnwatchFileSuccess() throws Exception {
        WatchedFile file = new WatchedFile("/var/log/auth.log", 2048L, LocalDateTime.now(), FileStatus.VERIFIED);
        file.setId(7L);
        file.setActive(true);

        when(watchedFileRepository.findById(7L)).thenReturn(Optional.of(file));
        when(watchedFileRepository.save(any(WatchedFile.class))).thenReturn(file);

        mockMvc.perform(delete("/api/files/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(7))
                .andExpect(jsonPath("$.active").value(false));

        verify(watchedFileRepository, times(1)).save(file);
    }

    @Test
    @DisplayName("DELETE /api/files/{id} returns 404 Not Found when file ID does not exist")
    void testUnwatchFileNotFound() throws Exception {
        when(watchedFileRepository.findById(888L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/files/888"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
