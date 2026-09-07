package com.example.demo.modules.game.rpg.dto;

public record RpgProfessionView(String code, String name, String description,
        String traitDescription, int hp, int mp, int attack, int magic,
        int defense, int magicDefense, int speed) { }
