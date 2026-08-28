package com.example.demo.modules.game.tjpoker.dto;

public class JoinRequest {
    private String roomId;
    private String mode;
    private String playerName;

    public JoinRequest() { }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
}
