package com.example.demo.modules.game.rpg.dto;

import java.util.List;

public record RpgBattleView(String battleId, String status, int turnNumber, int monsterLevel,
        RpgCombatantView player, RpgCombatantView monster,
        List<RpgSkillView> skills, List<String> logs,
        Integer rewardExp, Integer rewardGold, boolean levelUp,
        Integer previousLevel, int currentLevel, int currentExperience,
        int experienceToNextLevel, int currentGold,
        Integer recoveredHp, Integer recoveredMp, Integer experiencePenalty,
        List<RpgDropView> drops) { }
