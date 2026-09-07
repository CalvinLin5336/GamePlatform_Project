package com.example.demo.modules.game.rpg.dto;

import java.util.List;

public record RpgSkillConfigurationView(
        int slotCount,
        List<RpgSkillView> learnedSkills,
        List<RpgSkillView> equippedSkills) {
}
