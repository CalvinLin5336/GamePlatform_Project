package com.example.demo.modules.user.dto;

public record DashboardResponse(
        long totalUsers,
        long activeUsers,
        long disabledUsers,
        long adminUsers,
        long todayOperations
) {}
