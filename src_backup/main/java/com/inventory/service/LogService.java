package com.inventory.service;

import com.inventory.model.LogEntry;
import com.inventory.model.User;
import java.time.LocalDateTime;
import java.util.List;

public interface LogService {
    void logAction(String action, String details, User user, String ipAddress);
    List<LogEntry> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    List<LogEntry> getLogsByUser(String username);
    List<LogEntry> getAllLogs();
} 