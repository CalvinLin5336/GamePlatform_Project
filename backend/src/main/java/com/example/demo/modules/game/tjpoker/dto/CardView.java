package com.example.demo.modules.game.tjpoker.dto;

public class CardView {
    private int id;
    private String suit;
    private int rank;
    private String name;
    private int slot;

    public CardView() { }
    public CardView(int id, String suit, int rank, String name, int slot) {
        this.id = id; this.suit = suit; this.rank = rank; this.name = name; this.slot = slot;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSuit() { return suit; }
    public void setSuit(String suit) { this.suit = suit; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
}
