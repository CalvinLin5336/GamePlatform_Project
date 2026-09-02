package com.example.demo.modules.game.tjpoker.model;

import java.util.ArrayList;
import java.util.List;

public class GameRoom {
    public static final String MODE_PLAYER = "PLAYER";
    public static final String MODE_COMPUTER = "COMPUTER";
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_PLAYING = "PLAYING";
    public static final String STATUS_ROUND_RESULT = "ROUND_RESULT";
    public static final String STATUS_FINISHED = "FINISHED";

    private String id;
    private String mode;
    private Game game;
    private int[][] choices = new int[2][52];
    private String[] seatTokens = new String[2];
    private boolean[] connected = new boolean[2];
    private String status = STATUS_WAITING;
    private int currentRound = 1;
    private boolean[] roundConfirmed = new boolean[2];
    private List<RoundResult> results = new ArrayList<RoundResult>();
    private Integer winner;
    private Long roundResultEndsAt;

    public GameRoom(String id, String mode) {
        this.id = id;
        this.mode = mode;
        this.game = new Game();
    }

    public String getId() { return id; }
    public String getMode() { return mode; }
    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }
    public Player[] getPlayers() { return game.getPlayer(); }
    public int[][] getChoices() { return choices; }
    public String[] getSeatTokens() { return seatTokens; }
    public boolean[] getConnected() { return connected; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    public boolean[] getRoundConfirmed() { return roundConfirmed; }
    public List<RoundResult> getResults() { return results; }
    public void setResults(List<RoundResult> results) { this.results = results; }
    public Integer getWinner() { return winner; }
    public void setWinner(Integer winner) { this.winner = winner; }
    public Long getRoundResultEndsAt() { return roundResultEndsAt; }
    public void setRoundResultEndsAt(Long roundResultEndsAt) { this.roundResultEndsAt = roundResultEndsAt; }
}
