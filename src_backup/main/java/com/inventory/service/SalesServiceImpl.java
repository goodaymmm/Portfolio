package com.inventory.service;

import com.inventory.model.InventoryItem;
import com.inventory.model.Sales;
import com.inventory.model.User;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.SalesRepository;
import com.inventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class SalesServiceImpl implements SalesService {

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void recordSales(Sales sales) {
        salesRepository.save(sales);
    }

    @Override
    public Sales recordSale(Long itemId, int quantity, Long userId) {
        InventoryItem item = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("商品が見つかりません"));

        if (item.getQuantity() < quantity) {
            throw new RuntimeException("在庫が不足しています");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        Sales sale = new Sales();
        sale.setItem(item);
        sale.setQuantity(quantity);
        sale.setUnitPrice(item.getPrice());
        sale.setSalesDate(LocalDateTime.now());
        sale.setUser(user);

        // 在庫を減らす
        item.setQuantity(item.getQuantity() - quantity);
        inventoryRepository.save(item);

        return salesRepository.save(sale);
    }

    @Override
    public List<Sales> getTodaySales() {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return salesRepository.findTodaySales(startOfDay, endOfDay);
    }

    @Override
    public List<Sales> getWeeklySales() {
        LocalDateTime startOfWeek = LocalDateTime.now().with(LocalTime.MIN).minusDays(7);
        LocalDateTime endOfWeek = LocalDateTime.now().with(LocalTime.MAX);
        return salesRepository.findBySalesDateBetweenOrderBySalesDateDesc(startOfWeek, endOfWeek);
    }

    @Override
    public List<Sales> getMonthlySales() {
        LocalDateTime startOfMonth = LocalDateTime.now().with(LocalTime.MIN).minusDays(30);
        LocalDateTime endOfMonth = LocalDateTime.now().with(LocalTime.MAX);
        return salesRepository.findBySalesDateBetweenOrderBySalesDateDesc(startOfMonth, endOfMonth);
    }

    @Override
    public List<Sales> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return salesRepository.findBySalesDateBetweenOrderBySalesDateDesc(startDate, endDate);
    }

    @Override
    public Double calculateTotalSales(LocalDateTime startDate, LocalDateTime endDate) {
        return salesRepository.calculateTotalSales(startDate, endDate);
    }

    @Override
    public List<Sales> getAllSales() {
        return salesRepository.findAll();
    }

    @Override
    public Sales getSaleById(Long id) {
        return salesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("売上データが見つかりません"));
    }
} 