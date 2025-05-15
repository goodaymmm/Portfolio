package com.inventory.controller;

import com.inventory.model.User;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.LogService;
import com.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAnyRole('ADMIN', 'DEMO')")
public class AdminUserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private LogService logService;

    @GetMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateForm(Model model, Authentication authentication, HttpServletRequest request) {
        // アクセスログを記録
        logUserPageAccess("ユーザー作成ページアクセス", authentication, request);
        
        model.addAttribute("user", new User());
        return "admin/users/create";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showNewUserForm(Model model, Authentication authentication, HttpServletRequest request) {
        // アクセスログを記録
        logUserPageAccess("新規ユーザー登録ページアクセス", authentication, request);
        
        model.addAttribute("user", new User());
        return "admin/users/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(@ModelAttribute User user,
                           @RequestParam String role,
                           Authentication authentication,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(user, role);
            
            // ユーザー作成ログを記録
            User currentUser = getUserFromAuthentication(authentication);
            String details = String.format("新しいユーザー '%s'（メール: %s、ロール: %s）を作成しました", 
                    user.getUsername(), user.getEmail(), role);
            logService.logAction("ユーザー作成", details, currentUser, request.getRemoteAddr());
            
            redirectAttributes.addFlashAttribute("message", "ユーザーを作成しました。");
            return "redirect:/admin/users/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "ユーザー作成に失敗しました: " + e.getMessage());
            return "redirect:/admin/users/create";
        }
    }

    @GetMapping("/list")
    public String listUsers(Model model, Authentication authentication, HttpServletRequest request) {
        // アクセスログを記録
        logUserPageAccess("ユーザー一覧ページアクセス", authentication, request);
        
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users/list";
    }
    
    @GetMapping("/search")
    public String searchUsers(@RequestParam(required = false) String username,
                           @RequestParam(required = false) String email,
                           Model model,
                           Authentication authentication,
                           HttpServletRequest request) {
        // アクセスログを記録
        String searchDetails = String.format("ユーザー検索（ユーザー名: %s, メール: %s）", 
                username != null ? username : "なし", 
                email != null ? email : "なし");
        logUserPageAccess(searchDetails, authentication, request);
        
        List<User> users;
        
        if ((username == null || username.isEmpty()) && (email == null || email.isEmpty())) {
            users = userService.getAllUsers();
        } else {
            users = userService.searchUsers(username, email);
        }
        
        model.addAttribute("users", users);
        return "admin/users/list";
    }
    
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable(required = false) Long id, Model model, 
                              Authentication authentication, HttpServletRequest request, 
                              RedirectAttributes redirectAttributes) {
        try {
            // IDがnullまたは無効な値の場合
            if (id == null) {
                redirectAttributes.addFlashAttribute("error", "ユーザーIDが指定されていません。");
                return "redirect:/admin/users/list";
            }

            // アクセスログを記録（ここでユーザー情報は使用しないようにする）
            logUserPageAccess("ユーザー編集ページアクセス: ID=" + id, authentication, request);
            
            // 安全なユーザー情報の取得を試みる - findByIdSafeを使用
            Optional<User> userOptional = userService.findByIdSafe(id);
            
            if (userOptional.isPresent()) {
                // ユーザーが見つかった場合はそのまま表示
                model.addAttribute("user", userOptional.get());
            } else {
                // ユーザーが見つからない場合はnullを設定
                model.addAttribute("user", null);
                model.addAttribute("error", "指定されたユーザー(ID:" + id + ")が見つかりません");
            }
            
            return "admin/users/edit";
        } catch (Exception e) {
            // 例外発生時の詳細なログ
            e.printStackTrace(); // サーバーログに詳細を出力
            
            redirectAttributes.addFlashAttribute("error", 
                "ユーザー情報の取得中にエラーが発生しました: " + e.getMessage());
            return "redirect:/admin/users/list";
        }
    }
    
    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUser(@PathVariable Long id,
                            @ModelAttribute User user,
                            @RequestParam(required = false) String currentPassword,
                            @RequestParam(required = false) String newPassword,
                            Authentication authentication,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            // findByIdSafeを使用してnull安全なアクセスに変更
            Optional<User> existingUserOptional = userService.findByIdSafe(id);
            
            if (!existingUserOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "指定されたユーザーが見つかりません。");
                return "redirect:/admin/users/list";
            }
            
            User existingUser = existingUserOptional.get();
            
            // 現在のパスワードの検証（必須）
            if (currentPassword == null || currentPassword.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "現在のパスワードを入力してください");
                return "redirect:/admin/users/" + id + "/edit";
            }
            
            if (!passwordEncoder.matches(currentPassword, existingUser.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "現在のパスワードが正しくありません");
                return "redirect:/admin/users/" + id + "/edit";
            }
            
            // ユーザー名の更新
            existingUser.setUsername(user.getUsername());
            
            // メールアドレスの更新
            existingUser.setEmail(user.getEmail());
            
            // 新しいパスワードがある場合は更新
            boolean passwordChanged = false;
            if (newPassword != null && !newPassword.isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(newPassword));
                passwordChanged = true;
            }
            
            userService.updateUser(existingUser);
            
            // ユーザー更新ログを記録
            User currentUser = getUserFromAuthentication(authentication);
            StringBuilder detailsBuilder = new StringBuilder("ユーザー '")
                .append(existingUser.getUsername())
                .append("' の情報を更新しました");
            
            if (passwordChanged) {
                detailsBuilder.append("（パスワード変更あり）");
            }
            
            logService.logAction("ユーザー更新", detailsBuilder.toString(), currentUser, request.getRemoteAddr());
            
            redirectAttributes.addFlashAttribute("message", "ユーザー情報を更新しました");
            return "redirect:/admin/users/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "ユーザー更新に失敗しました: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }
    
    // ユーザー削除機能（POST）
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUserPost(@PathVariable Long id, 
                           Authentication authentication, 
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        return deleteUserInternal(id, authentication, request, redirectAttributes);
    }
    
    // ユーザー削除機能（DELETE）
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long id, 
                           Authentication authentication, 
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        return deleteUserInternal(id, authentication, request, redirectAttributes);
    }
    
    // 共通の削除処理
    private String deleteUserInternal(Long id, Authentication authentication, 
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            // 削除対象のユーザー情報を取得 - findByIdSafeを使用
            Optional<User> targetUserOptional = userService.findByIdSafe(id);
            
            if (!targetUserOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "指定されたユーザーが見つかりません。");
                return "redirect:/admin/users/list";
            }
            
            User targetUser = targetUserOptional.get();
            String username = targetUser.getUsername();
            
            // 削除処理を実行
            userService.deleteUser(id);
            
            // 削除ログを記録
            User currentUser = getUserFromAuthentication(authentication);
            String details = "ユーザー '" + username + "' を削除しました";
            logService.logAction("ユーザー削除", details, currentUser, request.getRemoteAddr());
            
            // 成功メッセージを設定
            redirectAttributes.addFlashAttribute("message", "ユーザーを削除しました");
            return "redirect:/admin/users/list";
        } catch (Exception e) {
            // エラーメッセージを設定
            redirectAttributes.addFlashAttribute("error", "削除に失敗しました: " + e.getMessage());
            return "redirect:/admin/users/list";
        }
    }
    
    // ユーザーアクセスログ記録の共通メソッド
    private void logUserPageAccess(String action, Authentication authentication, HttpServletRequest request) {
        User user = getUserFromAuthentication(authentication);
        if (user != null) {
            logService.logAction("ページアクセス", action, user, request.getRemoteAddr());
        }
    }
    
    // 認証情報からユーザー情報を取得
    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUser();
        }
        return null;
    }
}