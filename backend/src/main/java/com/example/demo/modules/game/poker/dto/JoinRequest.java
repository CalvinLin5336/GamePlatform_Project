package com.example.demo.modules.game.poker.dto;

public class JoinRequest {
    private String roomId;
    private Long gameId;
    private Long modeId;
    private String mode;
    private Long userId;
    private Integer seat;
    private String playerName;

    public JoinRequest() { }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public Long getModeId() { return modeId; }
    public void setModeId(Long modeId) { this.modeId = modeId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getSeat() { return seat; }
    public void setSeat(Integer seat) { this.seat = seat; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
}
