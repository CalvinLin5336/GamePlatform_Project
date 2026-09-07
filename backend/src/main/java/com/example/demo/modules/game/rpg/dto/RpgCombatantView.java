package com.example.demo.modules.game.rpg.dto;

public record RpgCombatantView(String code, String name, int currentHp, int maxHp,
        int currentMp, int maxMp, int shield, int actionPercent, String imagePath) { }
