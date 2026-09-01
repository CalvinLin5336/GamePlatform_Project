package com.example.demo.modules.game.poker.dto;

public class PreviewView {
    private int count;
    private int need;
    private String type;

    public PreviewView() { }
    public PreviewView(int count, int need, String type) { this.count = count; this.need = need; this.type = type; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public int getNeed() { return need; }
    public void setNeed(int need) { this.need = need; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
