package com.example.demo.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data // Lombok 自動產生 Getter/Setter/toString/equals/hashCode
@NoArgsConstructor // 無參數建構子 (JPA 必備)
@AllArgsConstructor // 全參數建構子 (Builder 模式會用到)
@Builder // 讓你可以用 User.builder().username("...").build() 來優雅建立物件
public class User {

    // 1. 系統唯一識別碼 (Primary Key)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 2. 登入驗證資訊
    @Column(unique = true, nullable = false, length = 50)
    private String username; // 登入帳號

    @Column(nullable = false)
    private String password; // 密碼 (未來存入必須是 Hash 處理過)

    @Column(unique = true, length = 100)
    private String email;    // 聯絡/忘記密碼用信箱

    // 3. 遊戲與社交展示資訊
    @Column(nullable = false, length = 50)
    private String nickname; // 遊戲內顯示的暱稱

    @Column(name = "avatar_url")
    private String avatarUrl; // 大頭貼網址

    // 定義玩家當前狀態 (配合 WebSocket 同步)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.OFFLINE; 

 // 4. 單一身分權限 (使用 Enum，直接在資料庫存成字串)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.PLAYER; // 預設註冊都是一般玩家



    // 5. 系統稽核欄位
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

 // --- 內部列舉：玩家身分角色 ---
    public enum Role {
        PLAYER,     // 一般玩家
        MODERATOR,  // 遊戲巡察員
        ADMIN       // 系統管理員
    }

    // --- 內部列舉：玩家線上狀態 ---
    public enum UserStatus {
        ONLINE,      // 上線 (在大廳)
        OFFLINE,     // 離線
        IN_ROOM,     // 遊戲房間準備中
        PLAYING      // 正在遊戲進行中
    }
}