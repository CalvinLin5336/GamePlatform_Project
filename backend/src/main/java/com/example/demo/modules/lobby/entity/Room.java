package com.example.demo.modules.lobby.entity; // 請替換成你專案的 package 路徑

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    private String id; // 房間代號

    @Column(nullable = false)
    private String hostName; // 記錄是誰開的房間
    
    
    // 玩家名單 (自動建立關聯資料表)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_players", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "player_name")
    private List<String> players = new java.util.ArrayList<>();
    
    @Column(nullable = false)
    private String gameType; // 遊戲名稱 (例如：田忌撲克)

    @Column(nullable = false)
    private String status; // 房間狀態：WAITING (等待中), PLAYING (遊戲中), FINISHED (已結束)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 建立時間

    // 在資料存入資料庫前，自動產生 UUID 與建立時間
    @PrePersist
    protected void onCreate() {
        this.id = UUID.randomUUID().toString().substring(0, 8); // 取前8碼當作房間號碼，方便玩家輸入
        this.createdAt = LocalDateTime.now();
        this.status = "WAITING"; // 預設狀態為等待中
    }

 // 🌟 記錄玩家選擇的遊戲 ID
    @Column(nullable = false)
    private Long gameId; 

    // 🌟 記錄選擇的模式名稱 (例如: "對戰電腦")
    @Column(nullable = false)
    private String modeName;

    // 🌟 從隊員的 game_modes 表抓過來寫入的限制條件
    @Column(nullable = false)
    private int minPlayers;

    @Column(nullable = false)
    private int maxPlayers;

    @Column(nullable = false)
    private int computerPlayers;
    // --- 以下為 Getter 與 Setter ---
    
    public Long getGameId() {
		return gameId;
	}
	public void setGameId(Long gameId) {
		this.gameId = gameId;
	}
	public String getModeName() {
		return modeName;
	}
	public void setModeName(String modeName) {
		this.modeName = modeName;
	}
	public int getMinPlayers() {
		return minPlayers;
	}
	public void setMinPlayers(int minPlayers) {
		this.minPlayers = minPlayers;
	}
	public int getMaxPlayers() {
		return maxPlayers;
	}
	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}
	public int getComputerPlayers() {
		return computerPlayers;
	}
	public void setComputerPlayers(int computerPlayers) {
		this.computerPlayers = computerPlayers;
	}
	public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    
    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> players) { this.players = players; }
    
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}