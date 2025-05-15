package com.inventory.controller;

import com.inventory.model.LogEntry;
import com.inventory.model.User;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

@Controller
@RequestMapping("/admin/logs")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEMO')")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping
    public String listLogs(Model model, Authentication authentication, HttpServletRequest request) {
        // ページアクセスログを記録
        logUserPageAccess("操作ログ一覧ページアクセス", authentication, request);
        
        model.addAttribute("logs", logService.getAllLogs());
        return "admin/logs/list";
    }

    @GetMapping("/search")
    public String searchLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication,
            HttpServletRequest request,
            Model model) {
            
        // 検索ログを記録
        User user = getUserFromAuthentication(authentication);
        if (user != null) {
            logService.logAction("操作ログ検索", "操作ログを検索しました", user, request.getRemoteAddr());
        }
        
        model.addAttribute("logs", logService.getLogsByDateRange(startDate, endDate));
        return "admin/logs/list";
    }

    @GetMapping("/user/{username}")
    public String getUserLogs(@PathVariable String username, 
                             Authentication authentication, 
                             HttpServletRequest request,
                             Model model) {
        // ページアクセスログを記録
        logUserPageAccess("ユーザー別操作ログ表示: " + username, authentication, request);
        
        List<LogEntry> userLogs = logService.getLogsByUser(username);
        // 降順にソート
        userLogs.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        model.addAttribute("logs", userLogs);
        model.addAttribute("username", username);
        return "admin/logs/user";
    }
    
    @GetMapping("/export")
    public void exportLogs(Authentication authentication, 
                          HttpServletRequest request,
                          jakarta.servlet.http.HttpServletResponse response) throws IOException {
        // 操作ログを記録
        User user = getUserFromAuthentication(authentication);
        logService.logAction("操作ログエクスポート", "すべての操作ログをCSVでエクスポートしました", user, request.getRemoteAddr());
        
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=operation_logs.csv");
        response.setCharacterEncoding("UTF-8");
        
        try (Writer writer = response.getWriter()) {
            // 検索履歴も含むすべての操作ログをエクスポート
            logService.exportToCSV(writer);
        }
    }
    
    // ユーザー別ログエクスポート機能
    @GetMapping("/export/user/{username}")
    public void exportUserLogs(@PathVariable String username, 
                              Authentication authentication,
                              HttpServletRequest request,
                              jakarta.servlet.http.HttpServletResponse response) throws IOException {
        // 操作ログを記録
        User user = getUserFromAuthentication(authentication);
        String details = String.format("ユーザー '%s' の操作ログをCSVでエクスポートしました", username);
        logService.logAction("操作ログエクスポート", details, user, request.getRemoteAddr());
        
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=user_" + username + "_logs.csv");
        response.setCharacterEncoding("UTF-8");
        
        try (Writer writer = response.getWriter()) {
            List<LogEntry> userLogs = logService.getLogsByUser(username);
            logService.exportSpecificLogsToCSV(writer, userLogs);
        }
    }
    
    // ユーザーアクセスログ記録の共通メソッド
    private void logUserPageAccess(String action, Authentication authentication, HttpServletRequest request) {
        User user = getUserFromAuthentication(authentication);
        if (user != null) {
            logService.logAction("ページアクセス", action, user, request.getRemoteAddr());
        }
    }
    
    // 認証情報からユーザー情報を取得
    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUser();
        }
        return null;
    }
} 