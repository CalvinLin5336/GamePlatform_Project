package com.example.demo.modules.game.rpg.dto;

import java.util.List;

public record RpgShopProductView(long id, String productType, String productCode, String productName,
        String description, int price, int quantity, Integer purchaseLimit, int purchasedQuantity,
        String itemType, RpgEquipmentView equipment, List<RpgShopMaterialView> materials) { }
