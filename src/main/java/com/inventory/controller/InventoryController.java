package com.inventory.controller;

import com.inventory.model.InventoryItem;
import com.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
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

    @PostMapping("/add")
    public String addItem(@Valid @ModelAttribute("item") InventoryItem item,
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "inventory/form";
        }

        // 自動ID生成
        String newItemId = generateNewItemId();
        item.setItemId(newItemId);

        inventoryService.save(item);
        redirectAttributes.addFlashAttribute("message", "商品が追加されました");
        return "redirect:/inventory";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        InventoryItem item = inventoryService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id));
        model.addAttribute("item", item);
        model.addAttribute("categories", CATEGORIES);
        return "inventory/form";
    }

    @PostMapping("/edit/{id}")
    public String updateItem(@PathVariable Long id,
                           @Valid @ModelAttribute("item") InventoryItem item,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "inventory/form";
        }

        item.setId(id);
        inventoryService.save(item);
        redirectAttributes.addFlashAttribute("message", "商品が更新されました");
        return "redirect:/inventory";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        inventoryService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "商品が削除されました");
        return "redirect:/inventory";
    }

    @GetMapping("/search")
    public String searchItems(@RequestParam(required = false) String name,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) Double minPrice,
                            @RequestParam(required = false) Double maxPrice,
                            Model model) {
        List<InventoryItem> items = inventoryService.searchItems(name, category, minPrice, maxPrice);
        model.addAttribute("items", items);
        return "inventory/list";
    }

    @GetMapping("/low-stock")
    public String showLowStockItems(Model model) {
        List<InventoryItem> items = inventoryService.getLowStockItems();
        model.addAttribute("items", items);
        return "inventory/list";
    }

    @GetMapping("/category/{category}")
    public String showItemsByCategory(@PathVariable String category, Model model) {
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
                            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            inventoryService.importFromCSV(file, userId);
            redirectAttributes.addFlashAttribute("message", "CSVファイルから商品をインポートしました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "インポート中にエラーが発生しました: " + e.getMessage());
        }
        return "redirect:/inventory";
    }

    @GetMapping("/export")
    public void exportItems(jakarta.servlet.http.HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=inventory.csv");
        response.setCharacterEncoding("UTF-8");
        
        try (Writer writer = response.getWriter()) {
            inventoryService.exportToCSV(writer);
        }
    }

    private String generateNewItemId() {
        List<InventoryItem> items = inventoryService.findAll();
        int maxId = items.stream()
                .map(item -> {
                    try {
                        return Integer.parseInt(item.getItemId().replace("ITEM", ""));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max(Integer::compareTo)
                .orElse(0);
        return String.format("ITEM%03d", maxId + 1);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        // 実際のユーザーIDを取得するロジックを実装
        return 1L; // 仮の実装
    }
} 