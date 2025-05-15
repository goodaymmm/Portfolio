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
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "users/register";
    }

    @PostMapping("/register")
    public String registerUser(User user, Model model) {
        try {
            userService.registerUser(user);
            model.addAttribute("message", "ユーザー登録が完了しました。ログインしてください。");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "ユーザー登録に失敗しました: " + e.getMessage());
            return "users/register";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "users/create";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public String createUser(@ModelAttribute User user,
                           @RequestParam String role,
                           RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(user, role);
            redirectAttributes.addFlashAttribute("message", "User created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DEMO')")
    @GetMapping("/list")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "users/list";
    }
} 