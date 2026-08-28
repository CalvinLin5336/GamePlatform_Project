package com.example.demo.modules.game.tjpoker.dto;

public class JoinResult {
    private String roomId;
    private String token;
    private int seat;
    private GameView game;

    public JoinResult() { }
    public JoinResult(String roomId, String token, int seat, GameView game) {
        this.roomId = roomId; this.token = token; this.seat = seat; this.game = game;
    }
    public String getRoomId() { return roomId; }
    public void setRoomId(String value) { roomId = value; }
    public String getToken() { return token; }
    public void setToken(String value) { token = value; }
    public int getSeat() { return seat; }
    public void setSeat(int value) { seat = value; }
    public GameView getGame() { return game; }
    public void setGame(GameView value) { game = value; }
}
