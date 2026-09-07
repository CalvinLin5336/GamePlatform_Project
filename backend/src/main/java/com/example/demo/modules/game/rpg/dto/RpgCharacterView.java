package com.example.demo.modules.game.rpg.dto;

public record RpgCharacterView(long characterId, String characterName,
        String professionCode, String professionName, int level,
        int experience, int experienceToNextLevel, int gold,
        int currentHp, int maxHp, int currentMp, int maxMp,
        int attack, int magic, int defense, int magicDefense, int speed) { }
