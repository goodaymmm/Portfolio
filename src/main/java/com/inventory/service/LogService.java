package com.inventory.service;

import com.inventory.model.LogEntry;
import com.inventory.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.io.IOException;
import java.io.Writer;

public interface LogService {
    void logAction(String action, String details, User user, String ipAddress);
    List<LogEntry> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    List<LogEntry> getLogsByUser(String username);
    List<LogEntry> getAllLogs();
    void exportToCSV(Writer writer) throws IOException;
    void exportSpecificLogsToCSV(Writer writer, List<LogEntry> logs) throws IOException;
} 