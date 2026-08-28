package com.example.demo.modules.chat.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.modules.chat.dto.LoginRequest;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 允許所有前端跨域請求（方便組員測試）
public class ChatAuthController {

    @PostMapping("/auth")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        System.out.println("收到前端登入請求 - 帳號: " + username + ", 密碼: " + password);

        // 簡單的測試邏輯 (實際專案中這裡應該去查 SQLite 資料庫)
        if ("admin".equals(username) && "1234".equals(password)) {
            // 登入成功，回傳 token 或使用者名稱
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "登入成功！",
                "username", username
            ));
        } else {
            // 登入失敗
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "帳號或密碼錯誤！"
            ));
        }
    }
}