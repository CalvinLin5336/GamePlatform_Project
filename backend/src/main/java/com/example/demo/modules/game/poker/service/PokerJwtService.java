package com.example.demo.modules.game.poker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.modules.game.poker.exception.GameException;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.security.JwtService;
import com.example.demo.modules.user.service.UserService;

@Service
public class PokerJwtService {
    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    public UserResponse requireUser(String authorization) {
        if(authorization==null || !authorization.startsWith("Bearer ")) {
            throw new GameException("LOGIN_REQUIRED", "請先登入平台再進入遊戲");
        }
        String jwt=authorization.substring(7).trim();
        if(jwt.equals("") || !jwtService.isTokenValid(jwt)) {
            throw new GameException("INVALID_LOGIN", "登入資料已失效，請重新登入");
        }

        Long userId=jwtService.extractUserId(jwt);
        String account=jwtService.extractAccount(jwt);
        UserResponse user=userService.findById(userId);
        if(account==null || !account.equals(user.account())) {
            throw new GameException("INVALID_LOGIN", "登入資料與使用者資料不一致");
        }
        if(!"Active".equalsIgnoreCase(user.status())) {
            throw new GameException("USER_DISABLED", "這個使用者目前無法進入遊戲");
        }
        return user;
    }
}
