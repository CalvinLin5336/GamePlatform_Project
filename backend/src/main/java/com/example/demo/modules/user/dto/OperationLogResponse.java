package com.example.demo.modules.user.dto;

public record OperationLogResponse(
        Long id,
        String account,
        String action,
        Long targetId,
        String role,
        String description,
        String createdAt
) {}
