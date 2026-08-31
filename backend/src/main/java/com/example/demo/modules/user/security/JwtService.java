package com.example.demo.modules.user.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    // JWT 簽章金鑰
    // 正式環境不要直接寫死在程式碼裡
    private static final String SECRET_KEY =
            "gameplatform-jwt-secret-key-2026-change-this-key";

    // Token 有效時間：1 小時
    private static final long EXPIRATION_TIME =
            1000 * 60 * 60;

    private final SecretKey key;

    public JwtService() {
        this.key = Keys.hmacShaKeyFor(
                SECRET_KEY.getBytes()
        );
    }

    /**
     * 建立 JWT
     */
    public String generateToken(
            Long userId,
            String account,
            String role
    ) {

        return Jwts.builder()
                .subject(account)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + EXPIRATION_TIME)
                )
                .signWith(key)
                .compact();
    }

    /**
     * 從 JWT 取得 account
     */
    public String extractAccount(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    /**
     * 從 JWT 取得 role
     */
    public String extractRole(String token) {

        return extractAllClaims(token)
                .get("role", String.class);
    }

    /**
     * 從 JWT 取得 userId
     */
    public Long extractUserId(String token) {

        return extractAllClaims(token)
                .get("userId", Long.class);
    }

    /**
     * 驗證 JWT
     */
    public boolean isTokenValid(String token) {

        try {

            extractAllClaims(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * 解析 JWT Claims
     */
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}