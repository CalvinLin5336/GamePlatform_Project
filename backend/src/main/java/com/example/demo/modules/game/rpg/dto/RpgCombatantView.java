package com.example.demo.modules.game.rpg.dto;

import java.util.List;

public record RpgCombatantView(String code, String name, int currentHp, int maxHp,
        int currentMp, int maxMp, int shield, int actionPercent, String imagePath,
        String identity, int attack, int magic, int defense, int magicDefense, int speed,
        List<String> statuses, List<String> traits) { }
