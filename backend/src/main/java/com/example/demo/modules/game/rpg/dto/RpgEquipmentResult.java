package com.example.demo.modules.game.rpg.dto;

import java.util.List;

public record RpgEquipmentResult(String message, RpgCharacterView character,
        List<RpgEquipmentView> equipment) { }
