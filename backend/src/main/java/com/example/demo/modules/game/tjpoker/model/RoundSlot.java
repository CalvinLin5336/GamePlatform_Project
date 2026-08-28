package com.example.demo.modules.game.tjpoker.model;

public class RoundSlot {
    public static RoundSlot DISCARD = new RoundSlot("棄牌", 2, 0);
    public static RoundSlot ROUND_1 = new RoundSlot("第 1 輪", 3, 1);
    public static RoundSlot ROUND_2 = new RoundSlot("第 2 輪", 5, 2);
    public static RoundSlot ROUND_3 = new RoundSlot("第 3 輪", 5, 3);

    private String label;
    private int need;
    private int index;

    public RoundSlot(String label, int need, int index) {
        this.label = label;
        this.need = need;
        this.index = index;
    }

    public String getLabel() {
        return label;
    }

    public int getNeed() {
        return need;
    }

    public int getIndex() {
        return index;
    }

    public static RoundSlot[] values() {
        RoundSlot slots[] = { DISCARD, ROUND_1, ROUND_2, ROUND_3 };
        return slots;
    }

    public static RoundSlot fromIndex(int index) {
        RoundSlot slots[] = values();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].getIndex() == index) {
                return slots[i];
            }
        }
        return DISCARD;
    }
}
