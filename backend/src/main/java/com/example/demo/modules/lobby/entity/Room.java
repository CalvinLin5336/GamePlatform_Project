package com.example.demo.modules.lobby.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    private String id; // 使用字串格式的 UUID 作為房號

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    @Column(nullable = false)
    private Long gameId; // 記錄遊戲的 ID

    @Column(name = "mode_id", nullable = false)
    private Long modeId; // 記錄模式名稱 (例如：玩家對戰)

    @Column(name = "host_account", nullable = false)
    private String hostAccount;

    @Column(nullable = false)
    private int minPlayers; // 該模式的最低人數

    @Column(nullable = false)
    private int maxPlayers; // 該模式的最高人數

    @Column(nullable = false)
    private int computerPlayers; // 電腦數量

    @Column(nullable = false)
    private String status = "WAITING"; // 房間狀態，預設為 WAITING

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 存放加入房間的玩家名單 (另建一張 room_players 關聯表儲存)
    @ElementCollection
    @CollectionTable(name = "room_players", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "player_account")
    private List<String> players = new ArrayList<>();

    // 在存入資料庫前，自動產生 ID 與建立時間
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase(); // 產生 8 碼大寫英數作為房號
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // --- 以下為 Getter 與 Setter ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public Long getModeId() { return modeId; }
    public void setModeId(Long modeId) { this.modeId = modeId; }
    public String getHostAccount() { return hostAccount; }
    public void setHostAccount(String hostAccount) { this.hostAccount = hostAccount; }
    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public int getComputerPlayers() { return computerPlayers; }
    public void setComputerPlayers(int computerPlayers) { this.computerPlayers = computerPlayers; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> players) { this.players = players; }
}
