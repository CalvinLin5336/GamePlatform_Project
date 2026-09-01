package com.example.demo.modules.game.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "game_modes")
public class GameMode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long modeId;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false, length = 50)
    private String modeCode;

    @Column(nullable = false, length = 100)
    private String modeName;

    @Column(nullable = false)
    private int minPlayers;

    @Column(nullable = false)
    private int maxPlayers;

    @Column(nullable = false)
    private int computerPlayers;

    @Column(nullable = false)
    private boolean enabled;

    public GameMode() { }

    public Long getModeId() { return modeId; }
    public void setModeId(Long modeId) { this.modeId = modeId; }
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public String getModeCode() { return modeCode; }
    public void setModeCode(String modeCode) { this.modeCode = modeCode; }
    public String getModeName() { return modeName; }
    public void setModeName(String modeName) { this.modeName = modeName; }
    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public int getComputerPlayers() { return computerPlayers; }
    public void setComputerPlayers(int computerPlayers) { this.computerPlayers = computerPlayers; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
