package com.example.demo.modules.game.rpg.dto;

public record RpgSkillView(String skillCode, String skillName, String description,
        String skillType, int mpCost, int basePower, int scalingPercent,
        int requiredLevel, boolean available) { }
