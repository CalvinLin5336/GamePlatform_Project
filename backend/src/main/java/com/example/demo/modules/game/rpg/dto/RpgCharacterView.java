package com.example.demo.modules.game.rpg.dto;

public record RpgCharacterView(long characterId, String characterName,
        String professionCode, String professionName, int level,
        int experience, int experienceToNextLevel, int gold,
        int currentHp, int maxHp, int currentMp, int maxMp,
        int attack, int magic, int defense, int magicDefense, int speed,
        int baseAttack, int attackBaseBonus, int baseMagic, int baseDefense, int baseMagicDefense, int baseSpeed,
        double physicalPenetrationPercent, int physicalPenetrationFlat,
        double criticalRate, double physicalLifesteal, int skillSlotCount) { }
