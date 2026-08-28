package com.example.demo.modules.user.dto;

public record UserResponse(
        Long id,
        String account,
        String username,
        String avatar,
        String description,
        String role,
        String status,
        String lastLogin
) {}
