package com.example.demo.modules.game.rpg.dto;

import java.util.List;

public record RpgEquipmentView(long ownedEquipmentId, String equipmentCode, String equipmentName,
        String equipmentType, String usageType, int requiredLevel, String description, int sellPrice,
        String equippedSlot, boolean professionAllowed, List<String> professions,
        List<RpgEquipmentStatView> stats) { }
