package com.inventory.repository;

import com.inventory.model.Sales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SalesRepository extends JpaRepository<Sales, Long> {
    List<Sales> findBySalesDateBetweenOrderBySalesDateDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT s FROM Sales s WHERE s.salesDate BETWEEN :startOfDay AND :endOfDay ORDER BY s.salesDate DESC")
    List<Sales> findTodaySales(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    @Query("SELECT COALESCE(SUM(s.quantity * s.unitPrice), 0) FROM Sales s WHERE s.salesDate BETWEEN :startDate AND :endDate")
    Double calculateTotalSales(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
} 