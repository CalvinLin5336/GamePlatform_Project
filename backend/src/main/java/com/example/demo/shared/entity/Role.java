package com.example.demo.shared.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 角色名稱，例如 "ROLE_PLAYER", "ROLE_MODERATOR"
    // Spring Security 習慣角色名稱以 "ROLE_" 開頭
    @Column(unique = true, nullable = false, length = 50)
    private String name; 
    
    // 角色的中文描述，方便後台管理員辨識
    @Column(length = 100)
    private String description; 
    
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
}