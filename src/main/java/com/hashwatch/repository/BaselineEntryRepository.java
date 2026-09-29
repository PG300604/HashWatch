package com.hashwatch.repository;

import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.WatchedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BaselineEntryRepository extends JpaRepository<BaselineEntry, Long> {
    Optional<BaselineEntry> findByWatchedFileAndCurrentTrue(WatchedFile watchedFile);
    List<BaselineEntry> findByCurrentTrue();
    List<BaselineEntry> findByWatchedFileOrderByCreatedAtDesc(WatchedFile watchedFile);
}
