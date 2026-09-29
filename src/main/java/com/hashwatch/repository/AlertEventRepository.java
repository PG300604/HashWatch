package com.hashwatch.repository;

import com.hashwatch.entity.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {
    List<AlertEvent> findByResolvedFalseOrderByDetectedAtDesc();
    List<AlertEvent> findTop50ByOrderByDetectedAtDesc();
    List<AlertEvent> findBySeverity(String severity);
    long countByResolvedFalse();
}
