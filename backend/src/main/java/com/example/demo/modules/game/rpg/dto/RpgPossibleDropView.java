package com.example.demo.modules.game.rpg.dto;

public record RpgPossibleDropView(String productType, String code, String name,
        double dropRate, int minQuantity, int maxQuantity) { }
