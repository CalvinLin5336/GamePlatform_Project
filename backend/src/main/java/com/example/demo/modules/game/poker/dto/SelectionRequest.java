package com.example.demo.modules.game.poker.dto;

import java.util.Map;

public class SelectionRequest {
    private Map<Integer, Integer> choices;

    public SelectionRequest() { }
    public Map<Integer, Integer> getChoices() { return choices; }
    public void setChoices(Map<Integer, Integer> choices) { this.choices = choices; }
}
