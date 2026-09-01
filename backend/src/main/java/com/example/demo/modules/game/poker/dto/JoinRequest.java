package com.example.demo.modules.game.poker.dto;

public class JoinRequest {
    private String roomId;
    private Long modeId;
    private Long userId;

    public JoinRequest() { }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Long getModeId() { return modeId; }
    public void setModeId(Long modeId) { this.modeId = modeId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
