package com.inventory.service;

import com.inventory.model.LogEntry;
import com.inventory.model.User;
import com.inventory.repository.LogEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.stream.Collectors;

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
        // findAllの結果を取得し、タイムスタンプの降順でソートして返す
        return logEntryRepository.findAll().stream()
                .sorted(Comparator.comparing(LogEntry::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
    
    @Override
    public void exportToCSV(Writer writer) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // ヘッダー行を書き込み
        writer.write("ID,アクション,詳細,ユーザー名,タイムスタンプ,IPアドレス\n");
        
        // すべてのログエントリを取得し、降順でソート
        List<LogEntry> logs = logEntryRepository.findAll().stream()
                .sorted(Comparator.comparing(LogEntry::getTimestamp).reversed())
                .collect(Collectors.toList());
        
        // デバッグ用：ログ件数を出力
        System.out.println("エクスポート対象ログ件数: " + logs.size());
        
        if (logs.isEmpty()) {
            System.out.println("エクスポート対象ログがありません");
        }
        
        // 各ログエントリをCSV形式で書き込み
        for (LogEntry log : logs) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(log.getId()).append(",");
                sb.append(escapeCSV(log.getAction())).append(",");
                sb.append(escapeCSV(log.getDetails())).append(",");
                
                String username = (log.getUser() != null) ? log.getUser().getUsername() : "不明";
                sb.append(escapeCSV(username)).append(",");
                
                String timestamp = (log.getTimestamp() != null) ? log.getTimestamp().format(formatter) : "";
                sb.append(timestamp).append(",");
                
                String ipAddress = (log.getIpAddress() != null) ? log.getIpAddress() : "";
                sb.append(escapeCSV(ipAddress)).append("\n");
                
                writer.write(sb.toString());
                writer.flush(); // 各行ごとにフラッシュして確実に書き込む
                
                // デバッグ用
                System.out.println("エクスポート行: " + sb.toString());
            } catch (Exception e) {
                System.err.println("CSVエクスポート中にエラーが発生しました: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void exportSpecificLogsToCSV(Writer writer, List<LogEntry> logs) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // ヘッダー行を書き込み
        writer.write("ID,アクション,詳細,ユーザー名,タイムスタンプ,IPアドレス\n");
        
        // デバッグ用：ログ件数を出力
        System.out.println("特定ログエクスポート対象件数: " + logs.size());
        
        if (logs.isEmpty()) {
            System.out.println("エクスポート対象ログがありません");
        }
        
        // 各ログエントリをCSV形式で書き込み
        for (LogEntry log : logs) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(log.getId()).append(",");
                sb.append(escapeCSV(log.getAction())).append(",");
                sb.append(escapeCSV(log.getDetails())).append(",");
                
                String username = (log.getUser() != null) ? log.getUser().getUsername() : "不明";
                sb.append(escapeCSV(username)).append(",");
                
                String timestamp = (log.getTimestamp() != null) ? log.getTimestamp().format(formatter) : "";
                sb.append(timestamp).append(",");
                
                String ipAddress = (log.getIpAddress() != null) ? log.getIpAddress() : "";
                sb.append(escapeCSV(ipAddress)).append("\n");
                
                writer.write(sb.toString());
                writer.flush(); // 各行ごとにフラッシュして確実に書き込む
                
                // デバッグ用
                System.out.println("特定ログエクスポート行: " + sb.toString());
            } catch (Exception e) {
                System.err.println("特定ログCSVエクスポート中にエラーが発生しました: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // CSVエスケープ処理：ダブルクォートで囲み、内部のダブルクォートをエスケープ
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
} 