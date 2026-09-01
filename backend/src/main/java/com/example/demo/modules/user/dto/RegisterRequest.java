package com.example.demo.modules.user.dto;

public record RegisterRequest(String account, String password, String username, String avatar, String description) {
}
