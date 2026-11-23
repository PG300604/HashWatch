package com.hashwatch.repository;

import com.hashwatch.entity.FileStatus;
import com.hashwatch.entity.WatchedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchedFileRepository extends JpaRepository<WatchedFile, Long> {
    Optional<WatchedFile> findByFilePath(String filePath);
    List<WatchedFile> findByActiveTrue();
    List<WatchedFile> findByStatus(FileStatus status);
    long countByActiveTrue();

    /**
     * Optimized direct count query by integrity status (e.g. TAMPERED, MISSING, VERIFIED).
     * Avoids loading large entity collections into JVM heap for dashboard KPI metric tiles.
     *
     * @param status the integrity status to count
     * @return total matching files
     */
    long countByStatus(FileStatus status);
}
