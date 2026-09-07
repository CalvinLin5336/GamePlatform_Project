package com.example.demo.modules.game.rpg.dto;

import java.util.List;

public record RpgProfessionView(String code, String name, String description,
        String traitDescription, String extraAttributes,
        int hp, int mp, int attack, int magic, int defense, int magicDefense, int speed,
        int growthHp, int growthMp, int growthAttack, int growthMagic,
        int growthDefense, int growthMagicDefense, int growthSpeed,
        List<RpgSkillView> skills) { }
