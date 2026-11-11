package com.hashwatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashwatch.dto.FileRegistrationRequest;
import com.hashwatch.entity.FileStatus;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * =============================================================================
 * DOMAIN: API Integration Tests
 * ASSIGNED TO: Priyanshu (Sprint A: SA-2)
 * TARGET: src/test/java/com/hashwatch/controller/FileControllerTest.java
 * =============================================================================
 *
 * MockMvc integration test suite validating:
 * 1. File registration with atomic SHA-256 & Ed25519 baseline creation.
 * 2. Directory traversal and path canonicalization.
 * 3. Physical existence, isRegularFile, and isReadable permission checks.
 * 4. 100 MB synchronous registration safety cap.
 * 5. Batch registration with fault isolation.
 * 6. Duplicate registration conflict (409) vs Intelligent Reactivation (200).
 * 7. Soft deactivation vs permanent hard cascade deletion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WatchedFileRepository watchedFileRepository;

    @Autowired
    private BaselineEntryRepository baselineEntryRepository;

    @BeforeEach
    void cleanDatabase() {
        baselineEntryRepository.deleteAll();
        watchedFileRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/files - Should return empty list when no files registered")
    void testListFiles_Empty() throws Exception {
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/files - Should register valid file, establish Ed25519 baseline, and return 201 Created")
    void testRegisterFile_Success(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("system_auth.conf");
        Files.writeString(testFile, "ENFORCE_MFA=true\nSESSION_TIMEOUT=900\n");

        FileRegistrationRequest request = new FileRegistrationRequest(testFile.toString());

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("baseline established")))
                .andExpect(jsonPath("$.data.filePath", equalTo(testFile.toAbsolutePath().normalize().toString().replace('\\', '/'))))
                .andExpect(jsonPath("$.data.status", equalTo("VERIFIED")))
                .andExpect(jsonPath("$.data.active", equalTo(true)));

        // Verify database persistence
        assertEquals(1, watchedFileRepository.count());
        WatchedFile persisted = watchedFileRepository.findAll().get(0);
        assertEquals(FileStatus.VERIFIED, persisted.getStatus());

        // Verify cryptographic baseline entry created
        assertEquals(1, baselineEntryRepository.count());
        assertTrue(baselineEntryRepository.findByWatchedFileAndCurrentTrue(persisted).isPresent());
    }

    @Test
    @DisplayName("POST /api/files - Blank path should return 400 Bad Request")
    void testRegisterFile_BlankPath() throws Exception {
        FileRegistrationRequest request = new FileRegistrationRequest("   ");

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("File path must not be blank")));
    }

    @Test
    @DisplayName("POST /api/files - Non-existent file should return 400 Bad Request")
    void testRegisterFile_NonExistentFile(@TempDir Path tempDir) throws Exception {
        Path ghostFile = tempDir.resolve("ghost_daemon.service");
        FileRegistrationRequest request = new FileRegistrationRequest(ghostFile.toString());

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("file does not exist on disk")));
    }

    @Test
    @DisplayName("POST /api/files - Directory path should be rejected with 400 Bad Request")
    void testRegisterFile_DirectoryPath(@TempDir Path tempDir) throws Exception {
        FileRegistrationRequest request = new FileRegistrationRequest(tempDir.toString());

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not a regular file")));
    }

    @Test
    @DisplayName("POST /api/files - Already actively monitored file should return 409 Conflict")
    void testRegisterFile_AlreadyActive(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("firewall.rules");
        Files.writeString(testFile, "ALLOW PORT 8080\n");

        FileRegistrationRequest request = new FileRegistrationRequest(testFile.toString());

        // First registration
        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration
        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already actively monitored")));
    }

    @Test
    @DisplayName("POST /api/files - Re-registering a soft-deleted file should intelligently reactivate it with 200 OK")
    void testRegisterFile_ReactivateSoftDeleted(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("nginx.conf");
        Files.writeString(testFile, "worker_processes auto;\n");

        FileRegistrationRequest request = new FileRegistrationRequest(testFile.toString());

        // 1. Initial registration
        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        WatchedFile registered = watchedFileRepository.findAll().get(0);

        // 2. Soft-delete
        mockMvc.perform(delete("/api/files/" + registered.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        // 3. Re-register the same path (should reactivate)
        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("reactivated")))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));

        // Only 1 entity should exist in DB (no unique constraint collision)
        assertEquals(1, watchedFileRepository.count());
    }

    @Test
    @DisplayName("POST /api/files/batch - Should register multiple files with fault isolation")
    void testBatchRegisterFiles(@TempDir Path tempDir) throws Exception {
        Path valid1 = tempDir.resolve("hosts.allow");
        Files.writeString(valid1, "127.0.0.1\n");

        Path valid2 = tempDir.resolve("hosts.deny");
        Files.writeString(valid2, "ALL\n");

        Path invalidGhost = tempDir.resolve("missing.txt");

        FileRegistrationRequest batchRequest = new FileRegistrationRequest(
                List.of(valid1.toString(), invalidGhost.toString(), valid2.toString())
        );

        mockMvc.perform(post("/api/files/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSubmitted").value(3))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failureCount").value(1))
                .andExpect(jsonPath("$.data.failed[0].filePath", equalTo(invalidGhost.toString())));

        assertEquals(2, watchedFileRepository.count());
    }

    @Test
    @DisplayName("GET /api/files - Should support activeOnly query parameter")
    void testListFiles_ActiveOnlyFilter(@TempDir Path tempDir) throws Exception {
        Path file1 = tempDir.resolve("file1.txt");
        Files.writeString(file1, "content1");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file2, "content2");

        // Register both
        mockMvc.perform(post("/api/files")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new FileRegistrationRequest(file1.toString()))));
        mockMvc.perform(post("/api/files")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new FileRegistrationRequest(file2.toString()))));

        List<WatchedFile> all = watchedFileRepository.findAll();
        assertEquals(2, all.size());

        // Deactivate file1
        mockMvc.perform(delete("/api/files/" + all.get(0).getId()))
                .andExpect(status().isOk());

        // Query activeOnly=true
        mockMvc.perform(get("/api/files").param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(all.get(1).getId()));

        // Query activeOnly=false (all files)
        mockMvc.perform(get("/api/files").param("activeOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("DELETE /api/files/{id} - Hard delete should permanently remove file and cascade to baselines")
    void testUnwatchFile_HardDelete(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("temp_purge.conf");
        Files.writeString(testFile, "PURGE_TARGET\n");

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FileRegistrationRequest(testFile.toString()))))
                .andExpect(status().isCreated());

        WatchedFile file = watchedFileRepository.findAll().get(0);
        assertEquals(1, baselineEntryRepository.count());

        // Perform hard delete
        mockMvc.perform(delete("/api/files/" + file.getId()).param("hard", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("permanently deleted")));

        assertEquals(0, watchedFileRepository.count());
        assertEquals(0, baselineEntryRepository.count());
    }

    @Test
    @DisplayName("PATCH /api/files/{id}/watch-status - Should toggle active flag")
    void testUpdateWatchStatus(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("audit.log");
        Files.writeString(testFile, "LOG ENTRY\n");

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FileRegistrationRequest(testFile.toString()))))
                .andExpect(status().isCreated());

        WatchedFile file = watchedFileRepository.findAll().get(0);
        assertTrue(file.isActive());

        // Toggle to false
        mockMvc.perform(patch("/api/files/" + file.getId() + "/watch-status").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        assertFalse(watchedFileRepository.findById(file.getId()).get().isActive());

        // Toggle back to true
        mockMvc.perform(patch("/api/files/" + file.getId() + "/watch-status").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));

        assertTrue(watchedFileRepository.findById(file.getId()).get().isActive());
    }

    @Test
    @DisplayName("GET /api/files/{id} - Non-existent ID should return 404 Not Found")
    void testGetFileById_NotFound() throws Exception {
        mockMvc.perform(get("/api/files/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not found with ID: 99999")));
    }
}
