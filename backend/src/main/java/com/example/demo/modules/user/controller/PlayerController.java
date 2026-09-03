package com.example.demo.modules.user.controller;

import com.example.demo.modules.user.dto.LoginResponse;
import com.example.demo.modules.user.dto.PlayerUpdateRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.service.LoginSessionService;
import com.example.demo.modules.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController("userPlayerController")
@CrossOrigin(originPatterns = "*")
@RequestMapping("/api/user/player")
public class PlayerController {

    private final UserService userService;
    private final LoginSessionService loginSessionService;

    public PlayerController(UserService userService, LoginSessionService loginSessionService) {
        this.userService = userService;
        this.loginSessionService = loginSessionService;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return loginSessionService.requireUserFromAuthentication(authentication);
    }

    @PutMapping("/me")
    public LoginResponse updateMe(
            @RequestBody PlayerUpdateRequest request,
            Authentication authentication) {
        return userService.updatePlayer(authentication.getName(), request);
    }

    @DeleteMapping("/me")
    public void deleteMe(Authentication authentication) {
        userService.disablePlayer(authentication.getName());
    }
}
