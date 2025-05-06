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
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @Transactional
    public void createUser(User user, String roleName) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("このユーザー名は既に使用されています。");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("このメールアドレスは既に使用されています。");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("指定されたロールが見つかりません。"));
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        
        user.setEnabled(true);
        
        userRepository.save(user);
    }

    @Transactional
    public void initializeRoles() {
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            roleRepository.save(adminRole);

            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);

            Role managerRole = new Role();
            managerRole.setName("ROLE_MANAGER");
            roleRepository.save(managerRole);
        }
    }

    @Transactional
    public void initializeAdminUser() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setEmail("admin@example.com");
            admin.setEnabled(true);

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("管理者ロールが見つかりません。"));
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);

            userRepository.save(admin);
        }
    }

    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }
} 