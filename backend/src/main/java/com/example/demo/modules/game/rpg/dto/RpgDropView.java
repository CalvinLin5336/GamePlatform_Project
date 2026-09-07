package com.example.demo.modules.game.rpg.dto;

public record RpgDropView(String productType, String itemCode, String itemName,
        String equipmentCode, String equipmentName, int quantity) {
    public static RpgDropView item(String code, String name, int quantity) {
        return new RpgDropView("ITEM", code, name, null, null, quantity);
    }
    public static RpgDropView equipment(String code, String name) {
        return new RpgDropView("EQUIPMENT", null, null, code, name, 1);
    }
}
