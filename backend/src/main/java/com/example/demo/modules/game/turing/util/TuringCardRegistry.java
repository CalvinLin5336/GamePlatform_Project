package com.example.demo.modules.game.turing.util;

import java.util.List;

public class TuringCardRegistry {

    @FunctionalInterface
    public interface ConditionFunction {
        boolean test(int b, int y, int p);
    }

    public static void registerAll(List<PuzzleGenerator.ActiveCondition> allConditions, List<int[]> allCodes) {

        
        // ---- 卡片 1 ~ 4：單一顏色與固定數值比較 ----
        register(allConditions, allCodes, 1, 0, (b, y, p) -> b == 1);
        register(allConditions, allCodes, 1, 1, (b, y, p) -> b > 1);

        register(allConditions, allCodes, 2, 0, (b, y, p) -> b < 3);
        register(allConditions, allCodes, 2, 1, (b, y, p) -> b == 3);
        register(allConditions, allCodes, 2, 2, (b, y, p) -> b > 3);

        register(allConditions, allCodes, 3, 0, (b, y, p) -> y < 3);
        register(allConditions, allCodes, 3, 1, (b, y, p) -> y == 3);
        register(allConditions, allCodes, 3, 2, (b, y, p) -> y > 3);

        register(allConditions, allCodes, 4, 0, (b, y, p) -> y < 4);
        register(allConditions, allCodes, 4, 1, (b, y, p) -> y == 4);
        register(allConditions, allCodes, 4, 2, (b, y, p) -> y > 4);

        // ---- 卡片 5 ~ 7：單一顏色奇偶數 ----
        register(allConditions, allCodes, 5, 0, (b, y, p) -> b % 2 == 0);
        register(allConditions, allCodes, 5, 1, (b, y, p) -> b % 2 == 1);

        register(allConditions, allCodes, 6, 0, (b, y, p) -> y % 2 == 0);
        register(allConditions, allCodes, 6, 1, (b, y, p) -> y % 2 == 1);

        register(allConditions, allCodes, 7, 0, (b, y, p) -> p % 2 == 0);
        register(allConditions, allCodes, 7, 1, (b, y, p) -> p % 2 == 1);

        // ---- 卡片 8 ~ 10：特定數字出現的次數 ----
        register(allConditions, allCodes, 8, 0, (b, y, p) -> countNum(1, b, y, p) == 0);
        register(allConditions, allCodes, 8, 1, (b, y, p) -> countNum(1, b, y, p) == 1);
        register(allConditions, allCodes, 8, 2, (b, y, p) -> countNum(1, b, y, p) == 2);
        register(allConditions, allCodes, 8, 3, (b, y, p) -> countNum(1, b, y, p) == 3);

        register(allConditions, allCodes, 9, 0, (b, y, p) -> countNum(3, b, y, p) == 0);
        register(allConditions, allCodes, 9, 1, (b, y, p) -> countNum(3, b, y, p) == 1);
        register(allConditions, allCodes, 9, 2, (b, y, p) -> countNum(3, b, y, p) == 2);
        register(allConditions, allCodes, 9, 3, (b, y, p) -> countNum(3, b, y, p) == 3);

        register(allConditions, allCodes, 10, 0, (b, y, p) -> countNum(4, b, y, p) == 0);
        register(allConditions, allCodes, 10, 1, (b, y, p) -> countNum(4, b, y, p) == 1);
        register(allConditions, allCodes, 10, 2, (b, y, p) -> countNum(4, b, y, p) == 2);
        register(allConditions, allCodes, 10, 3, (b, y, p) -> countNum(4, b, y, p) == 3);

        // ---- 卡片 11 ~ 13：顏色兩兩比較 ----
        register(allConditions, allCodes, 11, 0, (b, y, p) -> b < y);
        register(allConditions, allCodes, 11, 1, (b, y, p) -> b == y);
        register(allConditions, allCodes, 11, 2, (b, y, p) -> b > y);

        register(allConditions, allCodes, 12, 0, (b, y, p) -> b < p);
        register(allConditions, allCodes, 12, 1, (b, y, p) -> b == p);
        register(allConditions, allCodes, 12, 2, (b, y, p) -> b > p);

        register(allConditions, allCodes, 13, 0, (b, y, p) -> y < p);
        register(allConditions, allCodes, 13, 1, (b, y, p) -> y == p);
        register(allConditions, allCodes, 13, 2, (b, y, p) -> y > p);

        // ---- 卡片 14 ~ 15：誰是極值（最小/最大） ----
        register(allConditions, allCodes, 14, 0, (b, y, p) -> b < p && b < y);
        register(allConditions, allCodes, 14, 1, (b, y, p) -> y < b && y < p);
        register(allConditions, allCodes, 14, 2, (b, y, p) -> p < y && p < b);

        register(allConditions, allCodes, 15, 0, (b, y, p) -> b > p && b > y);
        register(allConditions, allCodes, 15, 1, (b, y, p) -> y > b && y > p);
        register(allConditions, allCodes, 15, 2, (b, y, p) -> p > y && p > b);

        // ---- 卡片 16 ~ 17：奇偶數量的比較 ----
        register(allConditions, allCodes, 16, 0, (b, y, p) -> countEven(b, y, p) > countOdd(b, y, p));
        register(allConditions, allCodes, 16, 1, (b, y, p) -> countOdd(b, y, p) > countEven(b, y, p));

        register(allConditions, allCodes, 17, 0, (b, y, p) -> countEven(b, y, p) == 0);
        register(allConditions, allCodes, 17, 1, (b, y, p) -> countEven(b, y, p) == 1);
        register(allConditions, allCodes, 17, 2, (b, y, p) -> countEven(b, y, p) == 2);
        register(allConditions, allCodes, 17, 3, (b, y, p) -> countEven(b, y, p) == 3);

        // ---- 卡片 18 ~ 19：總和奇偶與兩數總和比較 ----
        register(allConditions, allCodes, 18, 0, (b, y, p) -> (b + y + p) % 2 == 0);
        register(allConditions, allCodes, 18, 1, (b, y, p) -> (b + y + p) % 2 == 1);

        register(allConditions, allCodes, 19, 0, (b, y, p) -> b + y < 6);
        register(allConditions, allCodes, 19, 1, (b, y, p) -> b + y == 6);
        register(allConditions, allCodes, 19, 2, (b, y, p) -> b + y > 6);

        // ---- 卡片 20 ~ 21：重複數字與成對狀況 ----
        register(allConditions, allCodes, 20, 0, (b, y, p) -> distinctCount(b, y, p) == 1);
        register(allConditions, allCodes, 20, 1, (b, y, p) -> distinctCount(b, y, p) == 2);
        register(allConditions, allCodes, 20, 2, (b, y, p) -> distinctCount(b, y, p) == 3);

        register(allConditions, allCodes, 21, 0, (b, y, p) -> distinctCount(b, y, p) == 3 || distinctCount(b, y, p) == 1);
        register(allConditions, allCodes, 21, 1, (b, y, p) -> distinctCount(b, y, p) == 2);

        // ---- 卡片 22 ~ 25：順序與連續性 ----
        register(allConditions, allCodes, 22, 0, (b, y, p) -> b < y && y < p);
        register(allConditions, allCodes, 22, 1, (b, y, p) -> b > y && y > p);
        register(allConditions, allCodes, 22, 2, (b, y, p) -> !((b < y && y < p) || (b > y && y > p)));

        register(allConditions, allCodes, 23, 0, (b, y, p) -> b + y + p < 6);
        register(allConditions, allCodes, 23, 1, (b, y, p) -> b + y + p == 6);
        register(allConditions, allCodes, 23, 2, (b, y, p) -> b + y + p > 6);

        register(allConditions, allCodes, 24, 0, (b, y, p) -> (y - b == 1) && (p - y == 1));
        register(allConditions, allCodes, 24, 1, (b, y, p) -> ((y - b == 1) && (p - y != 1)) || ((p - y == 1) && (y - b != 1)));
        register(allConditions, allCodes, 24, 2, (b, y, p) -> (y - b != 1) && (p - y != 1));

        register(allConditions, allCodes, 25, 0, (b, y, p) -> Math.abs(b - y) != 1 && Math.abs(y - p) != 1);
        register(allConditions, allCodes, 25, 1, (b, y, p) -> (Math.abs(b - y) == 1 && Math.abs(y - p) != 1) || (Math.abs(y - p) == 1 && Math.abs(b - y) != 1));
        register(allConditions, allCodes, 25, 2, (b, y, p) -> ((y - b == 1) && (p - y == 1)) || ((y - b == -1) && (p - y == -1)));

        // ---- 卡片 26 ~ 27：特定顏色小於固定值 ----
        register(allConditions, allCodes, 26, 0, (b, y, p) -> b < 3);
        register(allConditions, allCodes, 26, 1, (b, y, p) -> y < 3);
        register(allConditions, allCodes, 26, 2, (b, y, p) -> p < 3);

        register(allConditions, allCodes, 27, 0, (b, y, p) -> b < 4);
        register(allConditions, allCodes, 27, 1, (b, y, p) -> y < 4);
        register(allConditions, allCodes, 27, 2, (b, y, p) -> p < 4);

        // ---- 卡片 28 ~ 30：特定顏色等於固定值 ----
        register(allConditions, allCodes, 28, 0, (b, y, p) -> b == 1);
        register(allConditions, allCodes, 28, 1, (b, y, p) -> y == 1);
        register(allConditions, allCodes, 28, 2, (b, y, p) -> p == 1);

        register(allConditions, allCodes, 29, 0, (b, y, p) -> b == 3);
        register(allConditions, allCodes, 29, 1, (b, y, p) -> y == 3);
        register(allConditions, allCodes, 29, 2, (b, y, p) -> p == 3);

        register(allConditions, allCodes, 30, 0, (b, y, p) -> b == 4);
        register(allConditions, allCodes, 30, 1, (b, y, p) -> y == 4);
        register(allConditions, allCodes, 30, 2, (b, y, p) -> p == 4);

        // ---- 卡片 31 ~ 32：特定顏色大於固定值 ----
        register(allConditions, allCodes, 31, 0, (b, y, p) -> b > 1);
        register(allConditions, allCodes, 31, 1, (b, y, p) -> y > 1);
        register(allConditions, allCodes, 31, 2, (b, y, p) -> p > 1);

        register(allConditions, allCodes, 32, 0, (b, y, p) -> b > 3);
        register(allConditions, allCodes, 32, 1, (b, y, p) -> y > 3);
        register(allConditions, allCodes, 32, 2, (b, y, p) -> p > 3);

        // ---- 卡片 33：指定顏色的奇偶數 ----
        register(allConditions, allCodes, 33, 0, (b, y, p) -> b % 2 == 0);
        register(allConditions, allCodes, 33, 1, (b, y, p) -> y % 2 == 0);
        register(allConditions, allCodes, 33, 2, (b, y, p) -> p % 2 == 0);
        register(allConditions, allCodes, 33, 3, (b, y, p) -> b % 2 == 1);
        register(allConditions, allCodes, 33, 4, (b, y, p) -> y % 2 == 1);
        register(allConditions, allCodes, 33, 5, (b, y, p) -> p % 2 == 1);

        // ---- 卡片 34 ~ 35：指定顏色小於等於/大於等於其他所有顏色 ----
        register(allConditions, allCodes, 34, 0, (b, y, p) -> b <= y && b <= p);
        register(allConditions, allCodes, 34, 1, (b, y, p) -> y <= b && y <= p);
        register(allConditions, allCodes, 34, 2, (b, y, p) -> p <= b && p <= y);

        register(allConditions, allCodes, 35, 0, (b, y, p) -> b >= y && b >= p);
        register(allConditions, allCodes, 35, 1, (b, y, p) -> y >= b && y >= p);
        register(allConditions, allCodes, 35, 2, (b, y, p) -> p >= b && p >= y);

        // ---- 卡片 36：總和的倍數 ----
        register(allConditions, allCodes, 36, 0, (b, y, p) -> (b + y + p) % 3 == 0);
        register(allConditions, allCodes, 36, 1, (b, y, p) -> (b + y + p) % 4 == 0);
        register(allConditions, allCodes, 36, 2, (b, y, p) -> (b + y + p) % 5 == 0);

        // ---- 卡片 37 ~ 38：兩顏色總和等於固定值 ----
        register(allConditions, allCodes, 37, 0, (b, y, p) -> b + y == 4);
        register(allConditions, allCodes, 37, 1, (b, y, p) -> b + p == 4);
        register(allConditions, allCodes, 37, 2, (b, y, p) -> y + p == 4);

        register(allConditions, allCodes, 38, 0, (b, y, p) -> b + y == 6);
        register(allConditions, allCodes, 38, 1, (b, y, p) -> b + p == 6);
        register(allConditions, allCodes, 38, 2, (b, y, p) -> y + p == 6);

        // ---- 卡片 39 ~ 41：指定顏色與特定數值比較 ----
        register(allConditions, allCodes, 39, 0, (b, y, p) -> b == 1);
        register(allConditions, allCodes, 39, 1, (b, y, p) -> b > 1);
        register(allConditions, allCodes, 39, 2, (b, y, p) -> y == 1);
        register(allConditions, allCodes, 39, 3, (b, y, p) -> y > 1);
        register(allConditions, allCodes, 39, 4, (b, y, p) -> p == 1);
        register(allConditions, allCodes, 39, 5, (b, y, p) -> p > 1);

        register(allConditions, allCodes, 40, 0, (b, y, p) -> b < 3);
        register(allConditions, allCodes, 40, 1, (b, y, p) -> b == 3);
        register(allConditions, allCodes, 40, 2, (b, y, p) -> b > 3);
        register(allConditions, allCodes, 40, 3, (b, y, p) -> y < 3);
        register(allConditions, allCodes, 40, 4, (b, y, p) -> y == 3);
        register(allConditions, allCodes, 40, 5, (b, y, p) -> y > 3);
        register(allConditions, allCodes, 40, 6, (b, y, p) -> p < 3);
        register(allConditions, allCodes, 40, 7, (b, y, p) -> p == 3);
        register(allConditions, allCodes, 40, 8, (b, y, p) -> p > 3);

        register(allConditions, allCodes, 41, 0, (b, y, p) -> b < 4);
        register(allConditions, allCodes, 41, 1, (b, y, p) -> b == 4);
        register(allConditions, allCodes, 41, 2, (b, y, p) -> b > 4);
        register(allConditions, allCodes, 41, 3, (b, y, p) -> y < 4);
        register(allConditions, allCodes, 41, 4, (b, y, p) -> y == 4);
        register(allConditions, allCodes, 41, 5, (b, y, p) -> y > 4);
        register(allConditions, allCodes, 41, 6, (b, y, p) -> p < 4);
        register(allConditions, allCodes, 41, 7, (b, y, p) -> p == 4);
        register(allConditions, allCodes, 41, 8, (b, y, p) -> p > 4);

        // ---- 卡片 42 ~ 44：指定顏色是最大或最小 / 兩顏色互相比較 ----
        register(allConditions, allCodes, 42, 0, (b, y, p) -> b < y && b < p);
        register(allConditions, allCodes, 42, 1, (b, y, p) -> y < b && y < p);
        register(allConditions, allCodes, 42, 2, (b, y, p) -> p < b && p < y);
        register(allConditions, allCodes, 42, 3, (b, y, p) -> b > y && b > p);
        register(allConditions, allCodes, 42, 4, (b, y, p) -> y > b && y > p);
        register(allConditions, allCodes, 42, 5, (b, y, p) -> p > b && p > y);

        register(allConditions, allCodes, 43, 0, (b, y, p) -> b < y);
        register(allConditions, allCodes, 43, 1, (b, y, p) -> b == y);
        register(allConditions, allCodes, 43, 2, (b, y, p) -> b > y);
        register(allConditions, allCodes, 43, 3, (b, y, p) -> b < p);
        register(allConditions, allCodes, 43, 4, (b, y, p) -> b == p);
        register(allConditions, allCodes, 43, 5, (b, y, p) -> b > p);

        register(allConditions, allCodes, 44, 0, (b, y, p) -> y < b);
        register(allConditions, allCodes, 44, 1, (b, y, p) -> y == b);
        register(allConditions, allCodes, 44, 2, (b, y, p) -> y > b);
        register(allConditions, allCodes, 44, 3, (b, y, p) -> y < p);
        register(allConditions, allCodes, 44, 4, (b, y, p) -> y == p);
        register(allConditions, allCodes, 44, 5, (b, y, p) -> y > p);

        // ---- 卡片 45 ~ 47：指定數字出現特定次數的複合判斷 ----
        register(allConditions, allCodes, 45, 0, (b, y, p) -> countNum(1, b, y, p) == 0);
        register(allConditions, allCodes, 45, 1, (b, y, p) -> countNum(1, b, y, p) == 1);
        register(allConditions, allCodes, 45, 2, (b, y, p) -> countNum(1, b, y, p) == 2);
        register(allConditions, allCodes, 45, 3, (b, y, p) -> countNum(3, b, y, p) == 0);
        register(allConditions, allCodes, 45, 4, (b, y, p) -> countNum(3, b, y, p) == 1);
        register(allConditions, allCodes, 45, 5, (b, y, p) -> countNum(3, b, y, p) == 2);

        register(allConditions, allCodes, 46, 0, (b, y, p) -> countNum(3, b, y, p) == 0);
        register(allConditions, allCodes, 46, 1, (b, y, p) -> countNum(3, b, y, p) == 1);
        register(allConditions, allCodes, 46, 2, (b, y, p) -> countNum(3, b, y, p) == 2);
        register(allConditions, allCodes, 46, 3, (b, y, p) -> countNum(4, b, y, p) == 0);
        register(allConditions, allCodes, 46, 4, (b, y, p) -> countNum(4, b, y, p) == 1);
        register(allConditions, allCodes, 46, 5, (b, y, p) -> countNum(4, b, y, p) == 2);

        register(allConditions, allCodes, 47, 0, (b, y, p) -> countNum(1, b, y, p) == 0);
        register(allConditions, allCodes, 47, 1, (b, y, p) -> countNum(1, b, y, p) == 1);
        register(allConditions, allCodes, 47, 2, (b, y, p) -> countNum(1, b, y, p) == 2);
        register(allConditions, allCodes, 47, 3, (b, y, p) -> countNum(4, b, y, p) == 0);
        register(allConditions, allCodes, 47, 4, (b, y, p) -> countNum(4, b, y, p) == 1);
        register(allConditions, allCodes, 47, 5, (b, y, p) -> countNum(4, b, y, p) == 2);

        // ---- 卡片 48：任兩顏色互相比較的複合判斷 ----
        register(allConditions, allCodes, 48, 0, (b, y, p) -> b < y);
        register(allConditions, allCodes, 48, 1, (b, y, p) -> b == y);
        register(allConditions, allCodes, 48, 2, (b, y, p) -> b > y);
        register(allConditions, allCodes, 48, 3, (b, y, p) -> b < p);
        register(allConditions, allCodes, 48, 4, (b, y, p) -> b == p);
        register(allConditions, allCodes, 48, 5, (b, y, p) -> b > p);
        register(allConditions, allCodes, 48, 6, (b, y, p) -> y < p);
        register(allConditions, allCodes, 48, 7, (b, y, p) -> y == p);
        register(allConditions, allCodes, 48, 8, (b, y, p) -> y > p);
    }

    private static int countNum(int target, int b, int y, int p) {
        int count = 0;
        if (b == target) count++;
        if (y == target) count++;
        if (p == target) count++;
        return count;
    }

    private static int countEven(int b, int y, int p) {
        int count = 0;
        if (b % 2 == 0) count++;
        if (y % 2 == 0) count++;
        if (p % 2 == 0) count++;
        return count;
    }

    private static int countOdd(int b, int y, int p) {
        return 3 - countEven(b, y, p);
    }

    private static int distinctCount(int b, int y, int p) {
        if (b == y && y == p) return 1;
        if (b != y && y != p && b != p) return 3;
        return 2;
    }

    private static void register(
            List<PuzzleGenerator.ActiveCondition> list, 
            List<int[]> codes, 
            int cardId, int subId, 
            ConditionFunction func) {
        
        java.util.BitSet bitSet = new java.util.BitSet(125);
        for (int i = 0; i < codes.size(); i++) {
            int[] c = codes.get(i);
            if (func.test(c[0], c[1], c[2])) {
                bitSet.set(i);
            }
        }
        list.add(new PuzzleGenerator.ActiveCondition(cardId, subId, bitSet));
    }
}