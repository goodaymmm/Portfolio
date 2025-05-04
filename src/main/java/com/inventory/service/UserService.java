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
    public User createUser(User user, String roleName) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    @Transactional
    public void initializeRoles() {
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            adminRole.setDescription("Administrator role");
            roleRepository.save(adminRole);

            Role managerRole = new Role();
            managerRole.setName("ROLE_MANAGER");
            managerRole.setDescription("Manager role");
            roleRepository.save(managerRole);

            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            userRole.setDescription("User role");
            roleRepository.save(userRole);
        }
    }

    @Transactional
    public void initializeAdminUser() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin"); // パスワードは自動的にエンコードされます
            admin.setEmail("admin@example.com");
            admin.setEnabled(true);
            createUser(admin, "ROLE_ADMIN");
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
} 