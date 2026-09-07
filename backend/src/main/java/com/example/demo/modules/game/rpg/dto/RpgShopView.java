package com.example.demo.modules.game.rpg.dto;

import java.util.List;

public record RpgShopView(String shopCode, String shopName, RpgCharacterView character,
        List<RpgShopProductView> equipment, List<RpgShopProductView> items,
        List<RpgShopProductView> sellableItems, List<RpgShopProductView> sellableEquipment,
        List<RpgShopProductView> buybacks) { }
