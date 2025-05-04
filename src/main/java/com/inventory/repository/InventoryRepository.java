package com.inventory.repository;

import com.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByNameContainingIgnoreCase(String name);
    List<InventoryItem> findByCategory(String category);
    List<InventoryItem> findByPriceBetween(Double minPrice, Double maxPrice);
    List<InventoryItem> findByQuantityLessThan(int quantity);
    Optional<InventoryItem> findByItemId(String itemId);
} 