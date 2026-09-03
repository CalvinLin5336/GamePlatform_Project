package com.example.demo.modules.user.dto;

public record PlayerUpdateRequest(
        String account,
        String username,
        String avatar,
        String description
) {}
