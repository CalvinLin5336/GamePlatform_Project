package com.example.demo.modules.game.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long gameId;

    @Column(nullable = false, unique = true, length = 50)
    private String gameCode;

    @Column(nullable = false, length = 100)
    private String gameName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 200)
    private String frontendPath;

    @Column(nullable = false, length = 200)
    private String backendPath;

    @Column(length = 200)
    private String imagePath;

    @Column(nullable = false)
    private boolean enabled;

    public Game() { }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public String getGameCode() { return gameCode; }
    public void setGameCode(String gameCode) { this.gameCode = gameCode; }
    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFrontendPath() { return frontendPath; }
    public void setFrontendPath(String frontendPath) { this.frontendPath = frontendPath; }
    public String getBackendPath() { return backendPath; }
    public void setBackendPath(String backendPath) { this.backendPath = backendPath; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
