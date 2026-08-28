package com.example.demo.shared.config;

import com.example.demo.shared.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // --- 自定義查詢方法 (Query Methods) ---
    // Spring Boot 非常聰明，只要看懂你的方法名稱，就會自動幫你產生對應的 SQL 查詢！

    // 1. 透過登入帳號尋找使用者 (用在「登入」驗證)
    Optional<User> findByUsername(String username);

    // 2. 檢查資料庫是否已經存在某個帳號 (用在「註冊」時防範重複註冊)
    boolean existsByUsername(String username);

    // 3. 檢查資料庫是否已經存在某個信箱
    boolean existsByEmail(String email);
}