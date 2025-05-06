package com.inventory.service;

import com.inventory.model.LogEntry;
import com.inventory.model.User;
import com.inventory.repository.LogEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class LogServiceImpl implements LogService {

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Override
    public void logAction(String action, String details, User user, String ipAddress) {
        LogEntry logEntry = new LogEntry();
        logEntry.setAction(action);
        logEntry.setDetails(details);
        logEntry.setUser(user);
        logEntry.setIpAddress(ipAddress);
        logEntryRepository.save(logEntry);
    }

    @Override
    public List<LogEntry> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return logEntryRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate);
    }

    @Override
    public List<LogEntry> getLogsByUser(String username) {
        return logEntryRepository.findByUserUsernameOrderByTimestampDesc(username);
    }

    @Override
    public List<LogEntry> getAllLogs() {
        return logEntryRepository.findAll();
    }
} 