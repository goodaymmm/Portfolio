package com.inventory.service;

import com.inventory.model.InventoryItem;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

public interface InventoryService {
    List<InventoryItem> findAll();
    Optional<InventoryItem> findById(Long id);
    InventoryItem save(InventoryItem item);
    void deleteById(Long id);
    List<InventoryItem> searchItems(String name, String category, Double minPrice, Double maxPrice);
    List<InventoryItem> getLowStockItems();
    List<InventoryItem> getItemsByCategory(String category);
    void importFromCSV(MultipartFile file, Long userId) throws Exception;
    void exportToCSV(Writer writer) throws IOException;
} 