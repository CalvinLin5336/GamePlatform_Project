package com.example.demo.modules.game.management.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.service.LoginSessionService;

@Service
public class GameAdminAuthorizationService {
    private final LoginSessionService loginSessionService;

    public GameAdminAuthorizationService(LoginSessionService loginSessionService) {
        this.loginSessionService = loginSessionService;
    }

    public UserResponse requireAdmin(Authentication authentication) {
        UserResponse user = loginSessionService.requireUserFromAuthentication(authentication);
        if (!"ADMIN".equalsIgnoreCase(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }
        return user;
    }
}
