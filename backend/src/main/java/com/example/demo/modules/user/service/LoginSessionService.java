package com.example.demo.modules.user.service;

import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LoginSessionService {
    private final JwtService jwt;
    private final UserService users;

    public UserResponse requireUserFromAuthentication(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw unauthorized();
        }
        UserResponse user = users.findByAccountForSession(authentication.getName());
        if (!"Active".equalsIgnoreCase(user.status())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此會員帳號已停用，請聯絡管理員");
        }
        return user;
    }

    public UserResponse requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw unauthorized();
        String token = authorization.substring(7).trim();
        Long id;
        String account;
        try {
            id = jwt.extractUserId(token);
            account = jwt.extractAccount(token);
        } catch (RuntimeException e) { throw unauthorized(); }
        UserResponse user;
        try { user = users.findById(id); }
        catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 404) throw unauthorized();
            throw e;
        }
        if (!user.account().equals(account)) throw unauthorized();
        if (!"Active".equalsIgnoreCase(user.status()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此會員帳號已停用，請聯絡管理員");
        return user;
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登入已過期或尚未登入，請重新登入");
    }
}
