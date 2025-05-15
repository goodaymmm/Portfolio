package com.inventory.service;

import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.repository.RoleRepository;
import com.inventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ユーザー名からユーザーを取得するメソッド
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + username));
    }
    
    // ID指定でユーザーを取得するメソッド
    public User findById(Long id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id).orElse(null);
    }
    
    // ID指定でユーザーを取得する安全なメソッド（Optional型を返す）
    public Optional<User> findByIdSafe(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return userRepository.findById(id);
    }
    
    // 全ユーザーを取得するメソッド
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    // ユーザー情報を更新するメソッド
    @Transactional
    public User updateUser(User user) {
        // ユーザーが存在するか確認
        if (!userRepository.existsById(user.getId())) {
            throw new RuntimeException("更新対象のユーザーが存在しません: ID=" + user.getId());
        }
        
        // ユーザー名の重複確認（自分以外）
        userRepository.findByUsername(user.getUsername())
            .ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new RuntimeException("このユーザー名は既に使用されています");
                }
            });
        
        // メールアドレスの重複確認（自分以外）
        userRepository.findByEmail(user.getEmail())
            .ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new RuntimeException("このメールアドレスは既に使用されています");
                }
            });
        
        return userRepository.save(user);
    }
    
    // ユーザーを削除するメソッド
    @Transactional
    public void deleteUser(Long id) {
        // admin ユーザーは削除できないようにする
        User user = findById(id);
        if (user != null && "admin".equals(user.getUsername())) {
            throw new RuntimeException("管理者ユーザーは削除できません");
        }
        
        userRepository.deleteById(id);
    }
    
    // ユーザーをロールと共に作成するメソッド
    @Transactional
    public User createUser(User user, String roleName) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("このユーザー名は既に使用されています");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("このメールアドレスは既に使用されています");    
        }
        
        // パスワードをエンコード
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 指定されたロールを設定
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("指定されたロールが見つかりません: " + roleName));
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        
        // アカウントを有効化
        user.setEnabled(true);
        
        return userRepository.save(user);
    }

    @Transactional
    public void registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("このユーザー名は既に使用されています。");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("このメールアドレスは既に使用されています。");
        }

        // パスワードをエンコード
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // デフォルトのロールを設定
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("デフォルトのロールが見つかりません。"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        
        // アカウントを有効化
        user.setEnabled(true);
        
        userRepository.save(user);
    }
    
    // 初期ロールの作成
    @Transactional
    public void initializeRoles() {
        createRoleIfNotFound("ROLE_ADMIN");
        createRoleIfNotFound("ROLE_DEMO");  // デモロールを追加
        createRoleIfNotFound("ROLE_MANAGER");
        createRoleIfNotFound("ROLE_USER");
    }
    
    // 管理者アカウントの初期化
    @Transactional
    public void initializeAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setEmail("admin@example.com");
            adminUser.setEnabled(true);
            
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("管理者ロールが見つかりません"));
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            adminUser.setRoles(roles);
            
            userRepository.save(adminUser);
        }
        
        // デモユーザーの初期化
        if (!userRepository.existsByUsername("demo")) {
            User demoUser = new User();
            demoUser.setUsername("demo");
            demoUser.setPassword(passwordEncoder.encode("demo"));
            demoUser.setEmail("demo@example.com");
            demoUser.setEnabled(true);
            
            Role demoRole = roleRepository.findByName("ROLE_DEMO")
                    .orElseThrow(() -> new RuntimeException("デモロールが見つかりません"));
            Set<Role> roles = new HashSet<>();
            roles.add(demoRole);
            demoUser.setRoles(roles);
            
            userRepository.save(demoUser);
        }
    }
    
    private void createRoleIfNotFound(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            roleRepository.save(role);
        }
    }

    // ユーザーを検索するメソッド
    public List<User> searchUsers(String username, String email) {
        List<User> allUsers = userRepository.findAll();
        
        return allUsers.stream()
            .filter(user -> (username == null || username.isEmpty() || 
                          user.getUsername().toLowerCase().contains(username.toLowerCase())))
            .filter(user -> (email == null || email.isEmpty() || 
                          user.getEmail().toLowerCase().contains(email.toLowerCase())))
            .collect(Collectors.toList());
    }
} 