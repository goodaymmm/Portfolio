package com.inventory.repository;

import com.inventory.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByUserId(Long userId);
    List<LogEntry> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<LogEntry> findByAction(String action);
} 