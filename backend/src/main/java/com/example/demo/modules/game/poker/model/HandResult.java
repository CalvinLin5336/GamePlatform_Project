package com.example.demo.modules.game.poker.model;

public class HandResult {
    private String name;
    private long strength;

    public HandResult(String name, long strength) {
        this.name = name;
        this.strength = strength;
    }

    public String getName() {
        return name;
    }

    public long getStrength() {
        return strength;
    }

    public int compare(HandResult other) {
        if(this.strength>other.getStrength()) return 1;
        if(this.strength<other.getStrength()) return -1;
        return 0;
    }
}
