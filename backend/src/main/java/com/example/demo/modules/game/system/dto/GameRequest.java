package com.example.demo.modules.game.system.dto;

public class GameRequest {
    private String gameCode;
    private String gameName;
    private String description;
    private String frontendPath;
    private String backendPath;
    private String imagePath;
    private boolean enabled;

    public GameRequest() { }

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
