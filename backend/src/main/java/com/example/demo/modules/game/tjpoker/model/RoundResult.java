package com.example.demo.modules.game.tjpoker.model;

import java.util.List;

public class RoundResult {
    private int round;
    private List<Card> player1Cards;
    private String player1Type;
    private List<Card> player2Cards;
    private String player2Type;
    private int winner;

    public RoundResult(int round, List<Card> player1Cards, String player1Type,
            List<Card> player2Cards, String player2Type, int winner) {
        this.round = round;
        this.player1Cards = player1Cards;
        this.player1Type = player1Type;
        this.player2Cards = player2Cards;
        this.player2Type = player2Type;
        this.winner = winner;
    }

    public int getRound() { return round; }
    public List<Card> getPlayer1Cards() { return player1Cards; }
    public String getPlayer1Type() { return player1Type; }
    public List<Card> getPlayer2Cards() { return player2Cards; }
    public String getPlayer2Type() { return player2Type; }
    public int getWinner() { return winner; }
}
