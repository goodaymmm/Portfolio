package com.inventory.config;

import com.inventory.model.InventoryItem;
import com.inventory.repository.InventoryRepository;
import com.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final InventoryRepository inventoryRepository;

    @Autowired
    public DataInitializer(UserService userService, InventoryRepository inventoryRepository) {
        this.userService = userService;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // アプリケーション起動時にロールとユーザーを初期化
        userService.initializeRoles();
        userService.initializeAdminUser();
        
        // 在庫データの初期化
        if (inventoryRepository.count() == 0) {
            // サンプルデータの作成
            InventoryItem item1 = new InventoryItem();
            item1.setItemId("ITEM001");
            item1.setName("ノートパソコン");
            item1.setDescription("高性能ビジネスノートPC");
            item1.setCategory("電子機器");
            item1.setQuantity(100);
            item1.setMinQuantity(10);
            item1.setPrice(150000.0);
            item1.setStatus(InventoryItem.ItemStatus.ACTIVE);
            inventoryRepository.save(item1);

            InventoryItem item2 = new InventoryItem();
            item2.setItemId("ITEM002");
            item2.setName("デスクチェア");
            item2.setDescription("人間工学に基づいたオフィスチェア");
            item2.setCategory("オフィス家具");
            item2.setQuantity(50);
            item2.setMinQuantity(5);
            item2.setPrice(35000.0);
            item2.setStatus(InventoryItem.ItemStatus.ACTIVE);
            inventoryRepository.save(item2);

            InventoryItem item3 = new InventoryItem();
            item3.setItemId("ITEM003");
            item3.setName("プリンター");
            item3.setDescription("カラーレーザープリンター");
            item3.setCategory("電子機器");
            item3.setQuantity(30);
            item3.setMinQuantity(3);
            item3.setPrice(45000.0);
            item3.setStatus(InventoryItem.ItemStatus.ACTIVE);
            inventoryRepository.save(item3);
        }

        System.out.println("データ初期化が完了しました");
    }
} 