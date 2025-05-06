package com.inventory.controller;

import com.inventory.model.Sales;
import com.inventory.model.User;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.InventoryService;
import com.inventory.service.SalesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Controller
@RequestMapping("/sales")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class SalesController {

    @Autowired
    private SalesService salesService;

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    public String showSalesList(Model model) {
        // 今日の日付の範囲を設定
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // 今週の日付の範囲を設定
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
        if (today.getDayOfWeek().getValue() < DayOfWeek.MONDAY.getValue()) {
            startOfWeek = startOfWeek.minusWeeks(1);
            endOfWeek = endOfWeek.minusWeeks(1);
        }
        LocalDateTime startOfWeekDT = startOfWeek.atStartOfDay();
        LocalDateTime endOfWeekDT = endOfWeek.atTime(LocalTime.MAX);

        // 今月の日付の範囲を設定
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime startOfMonthDT = startOfMonth.atStartOfDay();
        LocalDateTime endOfMonthDT = endOfMonth.atTime(LocalTime.MAX);

        // 各期間の売上データを取得
        List<Sales> todaySalesList = salesService.getTodaySales();
        Double todayTotal = salesService.calculateTotalSales(startOfDay, endOfDay);
        Double weeklyTotal = salesService.calculateTotalSales(startOfWeekDT, endOfWeekDT);
        Double monthlyTotal = salesService.calculateTotalSales(startOfMonthDT, endOfMonthDT);

        model.addAttribute("sales", todaySalesList);
        model.addAttribute("todaySales", todayTotal);
        model.addAttribute("weeklySales", weeklyTotal);
        model.addAttribute("monthlySales", monthlyTotal);
        model.addAttribute("startDate", today);
        model.addAttribute("endDate", today);
        
        return "sales/list";
    }

    @GetMapping("/search")
    public String searchSales(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Model model) {
        
        // 日付が指定されていない場合は本日の日付を使用
        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = LocalDate.now();
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Sales> sales = salesService.getSalesByDateRange(startDateTime, endDateTime);
        Double totalSales = salesService.calculateTotalSales(startDateTime, endDateTime);
        
        // 今日の日付の範囲を設定
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // 今週の日付の範囲を設定
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
        if (today.getDayOfWeek().getValue() < DayOfWeek.MONDAY.getValue()) {
            startOfWeek = startOfWeek.minusWeeks(1);
            endOfWeek = endOfWeek.minusWeeks(1);
        }
        LocalDateTime startOfWeekDT = startOfWeek.atStartOfDay();
        LocalDateTime endOfWeekDT = endOfWeek.atTime(LocalTime.MAX);

        // 今月の日付の範囲を設定
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime startOfMonthDT = startOfMonth.atStartOfDay();
        LocalDateTime endOfMonthDT = endOfMonth.atTime(LocalTime.MAX);

        Double todayTotal = salesService.calculateTotalSales(startOfDay, endOfDay);
        Double weeklyTotal = salesService.calculateTotalSales(startOfWeekDT, endOfWeekDT);
        Double monthlyTotal = salesService.calculateTotalSales(startOfMonthDT, endOfMonthDT);
        
        model.addAttribute("sales", sales);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("todaySales", todayTotal);
        model.addAttribute("weeklySales", weeklyTotal);
        model.addAttribute("monthlySales", monthlyTotal);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        return "sales/list";
    }

    @GetMapping("/new")
    public String showRecordSaleForm(Model model) {
        model.addAttribute("items", inventoryService.findAll());
        return "sales/form";
    }

    @PostMapping("/record")
    public String recordSale(
            @RequestParam Long itemId,
            @RequestParam int quantity,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();
            salesService.recordSale(itemId, quantity, user.getId());
            redirectAttributes.addFlashAttribute("message", "売上を記録しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sales";
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportSalesToCsv() {
        List<Sales> sales = salesService.getAllSales();
        StringBuilder csv = new StringBuilder();
        
        // ヘッダー行
        csv.append("日時,商品名,数量,単価,合計,担当者\n");
        
        // データ行
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Sales sale : sales) {
            csv.append(String.format("%s,%s,%d,%.0f,%.0f,%s\n",
                sale.getSalesDate().format(formatter),
                sale.getItem().getName(),
                sale.getQuantity(),
                sale.getUnitPrice(),
                sale.getQuantity() * sale.getUnitPrice(),
                sale.getUser().getUsername()));
        }
        
        String filename = "sales_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.toString());
    }
} 