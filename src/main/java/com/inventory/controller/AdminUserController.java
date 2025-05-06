package com.inventory.controller;

import com.inventory.model.User;
import com.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/users/create";
    }

    @GetMapping("/new")
    public String showNewUserForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/users/create";
    }

    @PostMapping("/create")
    public String createUser(@ModelAttribute User user,
                           @RequestParam String role,
                           RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(user, role);
            redirectAttributes.addFlashAttribute("message", "ユーザーを作成しました。");
            return "redirect:/admin/users/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "ユーザー作成に失敗しました: " + e.getMessage());
            return "redirect:/admin/users/create";
        }
    }

    @GetMapping("/list")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users/list";
    }
} 