package com.inventory.service;

import com.inventory.model.InventoryItem;
import com.inventory.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public List<InventoryItem> findAll() {
        return inventoryRepository.findAll();
    }

    @Override
    public Optional<InventoryItem> findById(Long id) {
        return inventoryRepository.findById(id);
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        return inventoryRepository.save(item);
    }

    @Override
    public void deleteById(Long id) {
        inventoryRepository.deleteById(id);
    }

    @Override
    public List<InventoryItem> searchItems(String name, String category, Double minPrice, Double maxPrice) {
        if (name != null && !name.isEmpty()) {
            return inventoryRepository.findByNameContainingIgnoreCase(name);
        } else if (category != null && !category.isEmpty()) {
            return inventoryRepository.findByCategory(category);
        } else if (minPrice != null && maxPrice != null) {
            return inventoryRepository.findByPriceBetween(minPrice, maxPrice);
        } else {
            return inventoryRepository.findAll();
        }
    }

    @Override
    public List<InventoryItem> getLowStockItems() {
        return inventoryRepository.findByQuantityLessThan(10);
    }

    @Override
    public List<InventoryItem> getItemsByCategory(String category) {
        return inventoryRepository.findByCategory(category);
    }

    @Override
    public void importFromCSV(MultipartFile file, Long userId) throws Exception {
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            for (CSVRecord record : csvParser) {
                String itemId = record.get("itemId");
                String name = record.get("name");
                String description = record.get("description");
                String category = record.get("category");
                String quantityStr = record.get("quantity");
                String minQuantityStr = record.get("minQuantity");
                String priceStr = record.get("price");

                Optional<InventoryItem> existingItem = inventoryRepository.findByItemId(itemId);
                
                if (existingItem.isPresent()) {
                    InventoryItem item = existingItem.get();
                    item.setName(name);
                    if (description != null) item.setDescription(description);
                    if (category != null) item.setCategory(category);
                    if (!quantityStr.isEmpty()) {
                        item.setQuantity(Integer.parseInt(quantityStr));
                    }
                    if (!minQuantityStr.isEmpty()) {
                        item.setMinQuantity(Integer.parseInt(minQuantityStr));
                    }
                    if (!priceStr.isEmpty()) {
                        item.setPrice(Double.parseDouble(priceStr));
                    }
                    inventoryRepository.save(item);
                } else {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setItemId(itemId);
                    newItem.setName(name);
                    newItem.setDescription(description);
                    newItem.setCategory(category);
                    if (!quantityStr.isEmpty()) {
                        newItem.setQuantity(Integer.parseInt(quantityStr));
                    }
                    if (!minQuantityStr.isEmpty()) {
                        newItem.setMinQuantity(Integer.parseInt(minQuantityStr));
                    }
                    if (!priceStr.isEmpty()) {
                        newItem.setPrice(Double.parseDouble(priceStr));
                    }
                    inventoryRepository.save(newItem);
                }
            }
        }
    }

    @Override
    public void exportToCSV(Writer writer) throws IOException {
        List<InventoryItem> items = findAll();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("itemId", "name", "description", "category", "quantity", "minQuantity", "price"))) {
            for (InventoryItem item : items) {
                csvPrinter.printRecord(
                    item.getItemId(),
                    item.getName(),
                    item.getDescription(),
                    item.getCategory(),
                    item.getQuantity(),
                    item.getMinQuantity(),
                    item.getPrice()
                );
            }
        }
    }
} 