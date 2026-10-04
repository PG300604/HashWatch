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
}
