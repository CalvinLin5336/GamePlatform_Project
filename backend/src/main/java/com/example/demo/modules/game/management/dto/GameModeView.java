package com.example.demo.modules.game.management.dto;

import com.example.demo.modules.game.management.model.GameMode;

public class GameModeView {
    private Long modeId;
    private String modeCode;
    private String modeName;
    private int minPlayers;
    private int maxPlayers;
    private int computerPlayers;
    private boolean enabled;

    public GameModeView() { }

    public GameModeView(GameMode mode) {
        this.modeId = mode.getModeId();
        this.modeCode = mode.getModeCode();
        this.modeName = mode.getModeName();
        this.minPlayers = mode.getMinPlayers();
        this.maxPlayers = mode.getMaxPlayers();
        this.computerPlayers = mode.getComputerPlayers();
        this.enabled = mode.isEnabled();
    }

    public Long getModeId() { return modeId; }
    public void setModeId(Long modeId) { this.modeId = modeId; }
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
