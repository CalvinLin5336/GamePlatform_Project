package com.example.demo.modules.game.rpg.dto;

import java.util.List;

public record RpgStageView(String stageCode, int stageOrder, String regionCode, String regionName,
        String regionDescription, int regionMinLevel, int regionMaxLevel,
        String stageName, int recommendedLevel, String description, String imagePath, boolean unlocked,
        int clearCount, Integer bestTurns, String monsterCode, String monsterName,
        String monsterDescription, String monsterImagePath,
        int monsterMaxHp, int rewardExp, int rewardGold, List<RpgPossibleDropView> possibleDrops) { }
