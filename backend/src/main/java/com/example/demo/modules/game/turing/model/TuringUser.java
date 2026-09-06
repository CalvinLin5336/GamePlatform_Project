package com.example.demo.modules.game.turing.model;

import jakarta.persistence.*;

/**
 * Represents a registered player or administrator in the Turing Machine game.
 */
@Entity(name = "TuringUser") // 👈 關鍵：告訴 Hibernate 這個實體叫 TuringUser
@Table(name = "turing_users") // 👈 改變資料表名稱避免衝突
public class TuringUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // JPA 建議使用 Integer 取代 int

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role; // "USER" or "ADMIN"
    private Integer tokens;
    
    @Column(name = "is_blocked") // 對應資料表欄位
    private boolean blocked;

    public TuringUser() {}

    public TuringUser(Integer id, String username, String password, String role, int tokens, boolean blocked) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.tokens = tokens;
        this.blocked = blocked;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getTokens() { return tokens; }
    public void setTokens(Integer tokens) { this.tokens = tokens; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    @Override
    public String toString() {
        return username + " (" + role + ") - Tokens: " + tokens + (blocked ? " [BLOCKED]" : "");
    }
}