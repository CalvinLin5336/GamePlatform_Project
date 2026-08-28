package com.example.demo.modules.user.controller;

import com.example.demo.modules.user.dto.LoginRequest;
import com.example.demo.modules.user.dto.LoginResponse;
import com.example.demo.modules.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://10.10.1.171:5173"})
@RequestMapping("/api/auth")
public class UserPageAuthController {

    private final UserService userService;

    public UserPageAuthController(UserService userService) {
        this.userService = userService;
    }

//    @PostMapping("/login")
//    public LoginResponse login(@RequestBody LoginRequest request) {
//        return userService.login(request);
//    }
}
