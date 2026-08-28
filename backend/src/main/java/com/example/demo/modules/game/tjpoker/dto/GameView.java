package com.example.demo.modules.game.tjpoker.dto;

import java.util.List;
import java.util.Map;

public class GameView {
    private String roomId;
    private String mode;
    private String status;
    private int currentRound;
    private int seat;
    private boolean player1Connected;
    private boolean player2Connected;
    private boolean player1Confirmed;
    private boolean player2Confirmed;
    private List<CardView> hand;
    private Map<Integer, PreviewView> preview;
    private List<ResultView> results;
    private Integer winner;
    private String player1Name;
    private String player2Name;

    public GameView() { }
    public GameView(String roomId, String mode, String status, int currentRound, int seat,
            boolean player1Connected, boolean player2Connected, boolean player1Confirmed,
            boolean player2Confirmed, List<CardView> hand, Map<Integer, PreviewView> preview,
            List<ResultView> results, Integer winner, String player1Name, String player2Name) {
        this.roomId = roomId; this.mode = mode; this.status = status; this.currentRound = currentRound;
        this.seat = seat; this.player1Connected = player1Connected; this.player2Connected = player2Connected;
        this.player1Confirmed = player1Confirmed; this.player2Confirmed = player2Confirmed;
        this.hand = hand; this.preview = preview; this.results = results; this.winner = winner;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
    }
    public String getRoomId() { return roomId; }
    public void setRoomId(String value) { roomId = value; }
    public String getMode() { return mode; }
    public void setMode(String value) { mode = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int value) { currentRound = value; }
    public int getSeat() { return seat; }
    public void setSeat(int value) { seat = value; }
    public boolean isPlayer1Connected() { return player1Connected; }
    public void setPlayer1Connected(boolean value) { player1Connected = value; }
    public boolean isPlayer2Connected() { return player2Connected; }
    public void setPlayer2Connected(boolean value) { player2Connected = value; }
    public boolean isPlayer1Confirmed() { return player1Confirmed; }
    public void setPlayer1Confirmed(boolean value) { player1Confirmed = value; }
    public boolean isPlayer2Confirmed() { return player2Confirmed; }
    public void setPlayer2Confirmed(boolean value) { player2Confirmed = value; }
    public List<CardView> getHand() { return hand; }
    public void setHand(List<CardView> value) { hand = value; }
    public Map<Integer, PreviewView> getPreview() { return preview; }
    public void setPreview(Map<Integer, PreviewView> value) { preview = value; }
    public List<ResultView> getResults() { return results; }
    public void setResults(List<ResultView> value) { results = value; }
    public Integer getWinner() { return winner; }
    public void setWinner(Integer value) { winner = value; }
    public String getPlayer1Name() { return player1Name; }
    public void setPlayer1Name(String value) { player1Name = value; }
    public String getPlayer2Name() { return player2Name; }
    public void setPlayer2Name(String value) { player2Name = value; }
}
