package com.example.demo.modules.game.rpg.dto;

import java.util.List;

public record RpgInventoryItemView(String itemCode, String itemName, String itemType,
        String description, int quantity, int maxStack, int sellPrice,
        List<RpgItemEffectView> effects) { }
