package com.example.demo.shared.config;

import com.example.demo.shared.config.UserRepository; // 假設你的 User Repository 在這裡
import com.example.demo.shared.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            // 1. 檢查是不是已經有資料了（避免重複建立）
            if (userRepository.count() > 0) {
                return;
            }

            // 2. 建立一個預設的系統管理員 (ADMIN)
            User admin = User.builder()
                    .username("admin")
                    .password("123456") // 實務上要 Hash，測試期先用明碼
                    .email("admin@gameplatform.com")
                    .nickname("系統管理員")
                    .role(User.Role.ADMIN)
                    .status(User.UserStatus.OFFLINE)
                    .build();

            userRepository.save(admin);

            // 3. 建立一個一般測試玩家 (PLAYER)
            User player1 = User.builder()
                    .username("player1")
                    .password("123456")
                    .email("player1@gameplatform.com")
                    .nickname("新手玩家小明")
                    .role(User.Role.PLAYER)
                    .status(User.UserStatus.ONLINE)
                    .build();

            userRepository.save(player1);

            System.out.println("🚀 【系統提示】測試假資料已成功自動植入 SQLite 資料庫！");
        };
    }
}