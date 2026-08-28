package com.example.demo.modules.user.dto;

public record UserRequest(
        String account,
        String password,
        String username,
        String avatar,
        String description,
        String role,
        String status
) {}
