package com.inventory.controller;

import com.inventory.model.LogEntry;
import com.inventory.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/logs")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping
    public String listLogs(Model model) {
        model.addAttribute("logs", logService.getAllLogs());
        return "admin/logs/list";
    }

    @GetMapping("/search")
    public String searchLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model) {
        model.addAttribute("logs", logService.getLogsByDateRange(startDate, endDate));
        return "admin/logs/list";
    }

    @GetMapping("/user/{username}")
    public String getUserLogs(@PathVariable String username, Model model) {
        model.addAttribute("logs", logService.getLogsByUser(username));
        return "admin/logs/list";
    }
} 