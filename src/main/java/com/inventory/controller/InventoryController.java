package com.inventory.controller;

import com.inventory.model.InventoryItem;
import com.inventory.model.User;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.InventoryService;
import com.inventory.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final LogService logService;

    @Autowired
    public InventoryController(InventoryService inventoryService, LogService logService) {
        this.inventoryService = inventoryService;
        this.logService = logService;
    }

    private static final List<String> CATEGORIES = Arrays.asList(
        "食品", "飲料", "日用品", "衣類", "電化製品", "その他"
    );

    private static final List<String> STATUSES = Arrays.asList(
        "ACTIVE", "DISCONTINUED", "OUT_OF_STOCK"
    );

    @GetMapping
    public String listItems(Model model) {
        List<InventoryItem> items = inventoryService.findAll();
        model.addAttribute("items", items);
        return "inventory/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("item", new InventoryItem());
        model.addAttribute("categories", CATEGORIES);
        return "inventory/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        InventoryItem item = inventoryService.findById(id)
                .orElseThrow(() -> new RuntimeException("商品が見つかりません: " + id));
        model.addAttribute("item", item);
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("statuses", STATUSES);
        return "inventory/form";
    }

    @PostMapping("/save")
    public String saveItem(@Valid InventoryItem item, BindingResult result, 
                           Authentication authentication, HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "inventory/form";
        }

        boolean isNewItem = (item.getId() == null);

        // ITEM_IDの処理
        if (isNewItem) {
            // 新規アイテムの場合
            if (item.getItemId() == null || item.getItemId().isEmpty()) {
                item.setItemId(generateNewItemId());
            }
        } else {
            // 既存アイテムの場合
            inventoryService.findById(item.getId()).ifPresent(existingItem -> {
                // ItemIDはそのまま保持する
                item.setItemId(existingItem.getItemId());
                // 作成日時も保持
                item.setCreatedAt(existingItem.getCreatedAt());
            });
        }

        // ステータスが設定されていない場合はACTIVEをデフォルト値に
        if (item.getStatus() == null) {
            item.setStatus(InventoryItem.ItemStatus.ACTIVE);
        }

        InventoryItem savedItem = inventoryService.save(item);

        // ログを記録
        User user = getUserFromAuthentication(authentication);
        String action = isNewItem ? "在庫追加" : "在庫編集";
        String details = isNewItem 
            ? String.format("商品「%s」(ID: %s)を追加しました", item.getName(), item.getItemId())
            : String.format("商品「%s」(ID: %s)を編集しました", item.getName(), item.getItemId());

        logService.logAction(action, details, user, request.getRemoteAddr());

        redirectAttributes.addFlashAttribute("message", "商品を保存しました");
        return "redirect:/inventory";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id, 
                             Authentication authentication, 
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            InventoryItem item = inventoryService.findById(id)
                    .orElseThrow(() -> new RuntimeException("商品が見つかりません: " + id));
            inventoryService.deleteById(id);
            
            // ログを記録
            User user = getUserFromAuthentication(authentication);
            String details = String.format("商品「%s」(ID: %s)を削除しました", item.getName(), item.getItemId());
            logService.logAction("在庫削除", details, user, request.getRemoteAddr());
            
            redirectAttributes.addFlashAttribute("message", "商品を削除しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "削除中にエラーが発生しました: " + e.getMessage());
        }
        return "redirect:/inventory";
    }

    @GetMapping("/search")
    public String searchItems(@RequestParam(required = false) String name,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) Double minPrice,
                            @RequestParam(required = false) Double maxPrice,
                            Authentication authentication,
                            HttpServletRequest request,
                            Model model) {
        // 検索ログを記録
        User user = getUserFromAuthentication(authentication);
        if (user != null) {
            logService.logAction("在庫検索", "在庫検索を実行しました", user, request.getRemoteAddr());
        }

        List<InventoryItem> items = inventoryService.searchItems(name, category, minPrice, maxPrice);
        model.addAttribute("items", items);
        return "inventory/list";
    }

    @GetMapping("/low-stock")
    public String showLowStockItems(Authentication authentication,
                                  HttpServletRequest request,
                                  Model model) {
        // 在庫不足検索ログを記録
        User user = getUserFromAuthentication(authentication);
        if (user != null) {
            logService.logAction("在庫不足検索", "在庫不足商品の検索を実行しました", user, request.getRemoteAddr());
        }

        List<InventoryItem> items = inventoryService.getLowStockItems();
        model.addAttribute("items", items);
        return "inventory/list";
    }

    @GetMapping("/category/{category}")
    public String showItemsByCategory(@PathVariable String category, 
                                    Authentication authentication,
                                    HttpServletRequest request,
                                    Model model) {
        // カテゴリー検索ログを記録
        User user = getUserFromAuthentication(authentication);
        if (user != null) {
            String details = String.format("カテゴリー「%s」で在庫検索を実行しました", category);
            logService.logAction("在庫カテゴリー検索", details, user, request.getRemoteAddr());
        }

        List<InventoryItem> items = inventoryService.getItemsByCategory(category);
        model.addAttribute("items", items);
        model.addAttribute("category", category);
        return "inventory/list";
    }

    @GetMapping("/import")
    public String showImportForm() {
        return "inventory/import";
    }

    @PostMapping("/import")
    public String importItems(@RequestParam("file") MultipartFile file,
                            Authentication authentication,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = getUserFromAuthentication(authentication);
            inventoryService.importFromCSV(file, user.getId());
            
            // ログを記録
            String details = "CSVファイルから商品をインポートしました";
            logService.logAction("在庫インポート", details, user, request.getRemoteAddr());
            
            redirectAttributes.addFlashAttribute("message", "CSVファイルから商品をインポートしました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "インポート中にエラーが発生しました: " + e.getMessage());
        }
        return "redirect:/inventory";
    }

    @GetMapping("/export")
    public void exportItems(HttpServletRequest request, Authentication authentication,
                            jakarta.servlet.http.HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=inventory.csv");
        response.setCharacterEncoding("UTF-8");
        
        try (Writer writer = response.getWriter()) {
            inventoryService.exportToCSV(writer);
            
            // ログを記録
            User user = getUserFromAuthentication(authentication);
            logService.logAction("在庫エクスポート", "在庫データをCSVでエクスポートしました", user, request.getRemoteAddr());
        }
    }

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUser();
        }
        return null;
    }

    // 商品IDの自動生成メソッド
    private String generateNewItemId() {
        List<InventoryItem> items = inventoryService.findAll();
        int maxId = 0;
        
        for (InventoryItem item : items) {
            String itemId = item.getItemId();
            if (itemId != null && itemId.startsWith("ITEM")) {
                try {
                    int id = Integer.parseInt(itemId.replace("ITEM", ""));
                    if (id > maxId) {
                        maxId = id;
                    }
                } catch (NumberFormatException e) {
                    // 数値変換できない場合はスキップ
                }
            }
        }
        
        return String.format("ITEM%03d", maxId + 1);
    }
}