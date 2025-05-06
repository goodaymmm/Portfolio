package com.inventory.service;

import com.inventory.model.Sales;
import java.time.LocalDateTime;
import java.util.List;

public interface SalesService {
    void recordSales(Sales sales);
    Sales recordSale(Long itemId, int quantity, Long userId);
    List<Sales> getTodaySales();
    List<Sales> getWeeklySales();
    List<Sales> getMonthlySales();
    List<Sales> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    Double calculateTotalSales(LocalDateTime startDate, LocalDateTime endDate);
    List<Sales> getAllSales();
    Sales getSaleById(Long id);
} 