package com.example.demo.modules.game.system.dto;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.modules.game.system.model.Game;

public class GameView {
    private Long gameId;
    private String gameCode;
    private String gameName;
    private String description;
    private String frontendPath;
    private String backendPath;
    private String imagePath;
    private boolean enabled;
    private List<GameModeView> modes = new ArrayList<GameModeView>();

    public GameView() { }

    public GameView(Game game) {
        this.gameId = game.getGameId();
        this.gameCode = game.getGameCode();
        this.gameName = game.getGameName();
        this.description = game.getDescription();
        this.frontendPath = game.getFrontendPath();
        this.backendPath = game.getBackendPath();
        this.imagePath = game.getImagePath();
        this.enabled = game.isEnabled();
    }

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
    public List<GameModeView> getModes() { return modes; }
    public void setModes(List<GameModeView> modes) { this.modes = modes; }
}
