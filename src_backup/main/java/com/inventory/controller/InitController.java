package com.inventory.controller;

import com.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/init")
public class InitController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String initializeData() {
        userService.initializeRoles();
        userService.initializeAdminUser();
        return "Data initialized successfully";
    }
} 