package com.example.demo.modules.game.tjpoker.dto;

import java.util.List;

public class ResultView {
    private int round;
    private List<String> player1Cards;
    private String player1Type;
    private List<String> player2Cards;
    private String player2Type;
    private int winner;

    public ResultView() { }
    public ResultView(int round, List<String> player1Cards, String player1Type, List<String> player2Cards, String player2Type, int winner) {
        this.round = round; this.player1Cards = player1Cards; this.player1Type = player1Type;
        this.player2Cards = player2Cards; this.player2Type = player2Type; this.winner = winner;
    }
    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }
    public List<String> getPlayer1Cards() { return player1Cards; }
    public void setPlayer1Cards(List<String> value) { player1Cards = value; }
    public String getPlayer1Type() { return player1Type; }
    public void setPlayer1Type(String value) { player1Type = value; }
    public List<String> getPlayer2Cards() { return player2Cards; }
    public void setPlayer2Cards(List<String> value) { player2Cards = value; }
    public String getPlayer2Type() { return player2Type; }
    public void setPlayer2Type(String value) { player2Type = value; }
    public int getWinner() { return winner; }
    public void setWinner(int winner) { this.winner = winner; }
}
