package com.example.demo.modules.game.turing.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class VerifierCard {
    private Integer cardId; // 💡 改用 Integer 避免序列化時的 null 對應例外
    private String title;
    private String description;

    public VerifierCard() {} // 建議補上無參數建構子

    public VerifierCard(Integer cardId, String title, String description) {
        this.cardId = cardId;
        this.title = title;
        this.description = description;
    }

    public Integer getCardId() {
        return cardId;
    }

    public void setCardId(Integer cardId) {
        this.cardId = cardId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // 其餘 verify 與輔助方法保持不變...

    /**
     * Evaluates whether the proposed code passes the criteria check in the exact same way
     * as the secret code does. In Turing Machine, a verifier card contains several sub-conditions.
     * The secret code matches exactly one sub-condition.
     * A proposal code passes this verifier's test if and only if it matches that SAME sub-condition.
     */
    public boolean verify(Code proposal, Code secret) {
        switch (cardId) {
            case 1: // Compare Blue to 1
                return (proposal.getBlue() == 1) == (secret.getBlue() == 1);

            case 2: // Compare Blue to 3
                int pSub = proposal.getBlue() < 3 ? -1 : (proposal.getBlue() == 3 ? 0 : 1);
                int sSub = secret.getBlue() < 3 ? -1 : (secret.getBlue() == 3 ? 0 : 1);
                return pSub == sSub;

            case 3: // Compare Yellow to 3
                int pSubY = proposal.getYellow() < 3 ? -1 : (proposal.getYellow() == 3 ? 0 : 1);
                int sSubY = secret.getYellow() < 3 ? -1 : (secret.getYellow() == 3 ? 0 : 1);
                return pSubY == sSubY;

            case 4: // Compare Yellow to 4 (修正：原本誤寫為 Purple)
                int pSubY4 = proposal.getYellow() < 4 ? -1 : (proposal.getYellow() == 4 ? 0 : 1);
                int sSubY4 = secret.getYellow() < 4 ? -1 : (secret.getYellow() == 4 ? 0 : 1);
                return pSubY4 == sSubY4;

            case 5: // Compare Blue is even or odd
                return (proposal.getBlue() % 2 == 0) == (secret.getBlue() % 2 == 0);

            case 6: // Compare Yellow is even or odd
                return (proposal.getYellow() % 2 == 0) == (secret.getYellow() % 2 == 0);

            case 7: // Compare Purple is even or odd
                return (proposal.getPurple() % 2 == 0) == (secret.getPurple() % 2 == 0);

            case 8: // Compare count of value 1 in code
                return countValue(proposal, 1) == countValue(secret, 1);

            case 9: // Compare count of value 3 in code
                return countValue(proposal, 3) == countValue(secret, 3);

            case 10: // Compare count of value 4 in code
                return countValue(proposal, 4) == countValue(secret, 4);

            case 11: // Compare Blue with Yellow
                int pCompBY = Integer.compare(proposal.getBlue(), proposal.getYellow());
                int sCompBY = Integer.compare(secret.getBlue(), secret.getYellow());
                return pCompBY == sCompBY;

            case 12: // Compare Blue with Purple
                int pCompBP = Integer.compare(proposal.getBlue(), proposal.getPurple());
                int sCompBP = Integer.compare(secret.getBlue(), secret.getPurple());
                return pCompBP == sCompBP;

            case 13: // Compare Yellow with Purple
                int pCompYP = Integer.compare(proposal.getYellow(), proposal.getPurple());
                int sCompYP = Integer.compare(secret.getYellow(), secret.getPurple());
                return pCompYP == sCompYP;

            case 14: // Compare which digit is smallest
                return getSmallestCategory(proposal) == getSmallestCategory(secret);

            case 15: // Compare which digit is largest
                return getLargestCategory(proposal) == getLargestCategory(secret);

            case 16: // Compare count of even digits
                return countEvens(proposal) == countEvens(secret);

            case 17: // Compare count of odd digits
                return countOdds(proposal) == countOdds(secret);

            case 18: // Is the sum of digits even or odd
                return ((proposal.getBlue() + proposal.getYellow() + proposal.getPurple()) % 2 == 0) ==
                       ((secret.getBlue() + secret.getYellow() + secret.getPurple()) % 2 == 0);

            case 19: // Blue + Yellow compared to 6
                int pBY = proposal.getBlue() + proposal.getYellow();
                int sBY = secret.getBlue() + secret.getYellow();
                int pSubBY = pBY < 6 ? -1 : (pBY == 6 ? 0 : 1);
                int sSubBY = sBY < 6 ? -1 : (sBY == 6 ? 0 : 1);
                return pSubBY == sSubBY;

            case 20: // Repetitions of digits
                return getRepeatCategory(proposal) == getRepeatCategory(secret);

            case 21: // Exactly one pair/repetition
                return (getRepeatCategory(proposal) > 1) == (getRepeatCategory(secret) > 1);

            case 22: // Strict order
                return getOrderCategory(proposal) == getOrderCategory(secret);

            case 23: // Sum of all three digits compared to 6
                int pSum23 = proposal.getBlue() + proposal.getYellow() + proposal.getPurple();
                int sSum23 = secret.getBlue() + secret.getYellow() + secret.getPurple();
                int pSub23 = pSum23 < 6 ? -1 : (pSum23 == 6 ? 0 : 1);
                int sSub23 = sSum23 < 6 ? -1 : (sSum23 == 6 ? 0 : 1);
                return pSub23 == sSub23;

            case 24: // 密碼中包含幾個「遞增的連續數字」 (4欄橫排)
                return getAscendingConsecutiveCount(proposal) == getAscendingConsecutiveCount(secret);

            case 25: // 密碼中包含幾個「遞增或遞減的連續數字」 (4欄橫排)
                return getConsecutiveCount(proposal) == getConsecutiveCount(secret);

            case 26: // 其中一個特定顏色數字小於 3
                return checkSpecificColorLogic(proposal, secret, p -> p < 3);

            case 27: // 其中一個特定顏色數字小於 4
                return checkSpecificColorLogic(proposal, secret, p -> p < 4);

            case 28: // 其中一個特定顏色數字等於 1
                return checkSpecificColorLogic(proposal, secret, p -> p == 1);

            case 29: // 其中一個特定顏色數字等於 3
                return checkSpecificColorLogic(proposal, secret, p -> p == 3);

            case 30: // 其中一個特定顏色數字等於 4
                return checkSpecificColorLogic(proposal, secret, p -> p == 4);

            case 31: // 其中一個特定顏色數字大於 1
                return checkSpecificColorLogic(proposal, secret, p -> p > 1);

            case 32: // 其中一個特定顏色數字大於 3
                return checkSpecificColorLogic(proposal, secret, p -> p > 3);

            case 33: // 其中一個特定顏色數字為偶數或奇數 (6格版型)
                return getSpecificParityCategory(secret) == getSpecificTargetParityCategory(proposal, secret);

            case 34: // 哪一個顏色的數字小於或等於其他數字 (3欄橫排)
                return getNonUniqueSmallestCategory(proposal) == getNonUniqueSmallestCategory(secret);

            case 35: // 哪一個顏色的數字大於或等於其他數字 (3欄橫排)
                return getNonUniqueLargestCategory(proposal) == getNonUniqueLargestCategory(secret);

            case 36: // 密碼的總和為3、4或5的倍數 (6格版型)
                return getMultipleCategory(proposal) == getMultipleCategory(secret);

            case 37: // 其中兩個特定顏色數字總和為 4 (3欄橫排)
                return getSpecificSumEqualsCategory(secret, 4) == getSpecificSumEqualsCategory(proposal, 4);

            case 38: // 其中兩個特定顏色數字總和為 6 (3欄橫排)
                return getSpecificSumEqualsCategory(secret, 6) == getSpecificSumEqualsCategory(proposal, 6);

            case 39: // 🏆 完美重構：其中一個特定顏色與數字 1 的關係 (對齊圖面 39)
                // 1. 先用 secret 找出是 6 條線索中的哪一條活著
                if (secret.getBlue() == 1)      return proposal.getBlue() == 1;
                if (secret.getBlue() > 1)       return proposal.getBlue() > 1;
                if (secret.getYellow() == 1)    return proposal.getYellow() == 1;
                if (secret.getYellow() > 1)     return proposal.getYellow() > 1;
                if (secret.getPurple() == 1)    return proposal.getPurple() == 1;
                if (secret.getPurple() > 1)     return proposal.getPurple() > 1;
                return false;

            case 40: // 🏆 完美重構：其中一個特定顏色數字與 3 的關係 (對齊圖面 40 的 9 格線索)
                // 1. 先拿 secret 探測這場賽局是哪一個顏色的哪種關係活著
                if (secret.getBlue() < 3)       return proposal.getBlue() < 3;
                if (secret.getBlue() == 3)      return proposal.getBlue() == 3;
                if (secret.getBlue() > 3)       return proposal.getBlue() > 3;
                
                if (secret.getYellow() < 3)     return proposal.getYellow() < 3;
                if (secret.getYellow() == 3)    return proposal.getYellow() == 3;
                if (secret.getYellow() > 3)     return proposal.getYellow() > 3;
                
                if (secret.getPurple() < 3)     return proposal.getPurple() < 3;
                if (secret.getPurple() == 3)    return proposal.getPurple() == 3;
                if (secret.getPurple() > 3)     return proposal.getPurple() > 3;
                return false;

            	case 41: // 🏆 完美重構：其中一個特定顏色數字與 4 的關係 (對齊圖面 41 的 9 格線索)
                // 1. 先拿 secret 探測這場賽局是哪一個顏色的哪種關係活著
                if (secret.getBlue() < 4)       return proposal.getBlue() < 4;
                if (secret.getBlue() == 4)      return proposal.getBlue() == 4;
                if (secret.getBlue() > 4)       return proposal.getBlue() > 4;
                
                if (secret.getYellow() < 4)     return proposal.getYellow() < 4;
                if (secret.getYellow() == 4)    return proposal.getYellow() == 4;
                if (secret.getYellow() > 4)     return proposal.getYellow() > 4;
                
                if (secret.getPurple() < 4)     return proposal.getPurple() < 4;
                if (secret.getPurple() == 4)    return proposal.getPurple() == 4;
                if (secret.getPurple() > 4)     return proposal.getPurple() > 4;
                return false;

            case 42: // 哪個顏色的數字是最小或最大的 (6格版型)
                return getMinMaxCategory(proposal) == getMinMaxCategory(secret);

            case 43: // 藍色數字與另一個特定顏色數字的關係 (9格版型)
                return getBlueToSpecificColorCategory(secret) == getBlueToSpecificColorCategory(proposal);

            case 44: // 黃色數字與另一個特定顏色數字的關係 (9格版型)
                return getYellowToSpecificColorCategory(secret) == getYellowToSpecificColorCategory(proposal);

            case 45: // 密碼中 1 或 3 的數量 (6格版型)
                return getCountOfOneOrThreeCategory(secret) == getCountOfOneOrThreeCategory(proposal);

            case 46: // 密碼中 3 或 4 的數量 (6格版型)
                return getCountOfThreeOrFourCategory(secret) == getCountOfThreeOrFourCategory(proposal);

            case 47: // 密碼中 1 或 4 的數量 (6格版型)
                return getCountOfOneOrFourCategory(secret) == getCountOfOneOrFourCategory(proposal);

            case 48: // 其中一個特定顏色數字與另一個特定顏色數字的關係 (9格版型)
                return getTwoColorsRelationCategory(secret) == getTwoColorsRelationCategory(proposal);

            default:
                System.out.println("⚠️ Warning: VerifierCard #" + cardId + " is using standard comparison fallback.");
                return proposal.equals(secret);
        }
    }

    // --- 內部基礎統計與解析輔助方法 ---

    private int countOdds(Code code) {
        int count = 0;
        if (code.getBlue() % 2 != 0) count++;
        if (code.getYellow() % 2 != 0) count++;
        if (code.getPurple() % 2 != 0) count++;
        return count;
    }

    private int getRepeatCategory(Code code) {
        int b = code.getBlue();
        int y = code.getYellow();
        int p = code.getPurple();
        if (b == y && y == p) return 3; // Triplet
        if (b == y || y == p || b == p) return 2; // Pair
        return 1; // Distinct
    }

    private int getOrderCategory(Code code) {
        int b = code.getBlue();
        int y = code.getYellow();
        int p = code.getPurple();
        if (b < y && y < p) return 1; // Strictly ascending
        if (b > y && y > p) return 2; // Strictly descending
        return 0; // No strict order
    }

    private int countValue(Code code, int value) {
        int count = 0;
        if (code.getBlue() == value) count++;
        if (code.getYellow() == value) count++;
        if (code.getPurple() == value) count++;
        return count;
    }

    private int countEvens(Code code) {
        int count = 0;
        if (code.getBlue() % 2 == 0) count++;
        if (code.getYellow() % 2 == 0) count++;
        if (code.getPurple() % 2 == 0) count++;
        return count;
    }

    private int getSmallestCategory(Code code) {
        int b = code.getBlue();
        int y = code.getYellow();
        int p = code.getPurple();
        if (b < y && b < p) return 1; // Blue is strictly smallest
        if (y < b && y < p) return 2; // Yellow is strictly smallest
        if (p < b && p < y) return 3; // Purple is strictly smallest
        return 0; // Tied smallest
    }

    private int getLargestCategory(Code code) {
        int b = code.getBlue();
        int y = code.getYellow();
        int p = code.getPurple();
        if (b > y && b > p) return 1; // Blue is strictly largest
        if (y > b && y > p) return 2; // Yellow is strictly largest
        if (p > b && p > y) return 3; // Purple is strictly largest
        return 0; // Tied largest
    }

    // --- 高階幾何與特定對比演算擴充輔助方法 ---

    private int getAscendingConsecutiveCount(Code code) {
        int b = code.getBlue(), y = code.getYellow(), p = code.getPurple();
        if (y == b + 1 && p == y + 1) return 3;
        if (y == b + 1 || p == y + 1) return 2;
        return 0;
    }

    private int getConsecutiveCount(Code code) {
        int b = code.getBlue(), y = code.getYellow(), p = code.getPurple();
        if ((y == b + 1 && p == y + 1) || (y == b - 1 && p == y - 1)) return 3;
        if (Math.abs(y - b) == 1 || Math.abs(p - y) == 1) return 2;
        return 0;
    }

    private boolean checkSpecificColorLogic(Code proposal, Code secret, java.util.function.Predicate<Integer> condition) {
        // 1. 探測答案中是哪一個顏色符合條件（藍=1, 黃=2, 紫=3）
        int targetColor = 0;
        if (condition.test(secret.getBlue())) {
            targetColor = 1;
        } else if (condition.test(secret.getYellow())) {
            targetColor = 2;
        } else if (condition.test(secret.getPurple())) {
            targetColor = 3;
        }
        
        // 如果連答案本身都沒有任何顏色符合，直接回傳 false
        if (targetColor == 0) return false;
        
        // 2. 根據被鎖定的特定顏色，直接比對玩家提案的該顏色數值
        if (targetColor == 1) return condition.test(proposal.getBlue());
        if (targetColor == 2) return condition.test(proposal.getYellow());
        return condition.test(proposal.getPurple());
    }

    private int getSpecificTargetParityCategory(Code code, Code secret) {
        boolean bEven = (secret.getBlue() % 2 == 0);
        boolean yEven = (secret.getYellow() % 2 == 0);
        boolean pEven = (secret.getPurple() % 2 == 0);
        if (bEven && !yEven && !pEven) return (code.getBlue() % 2 == 0) ? 1 : 2;
        if (!bEven && yEven && !pEven) return (code.getYellow() % 2 == 0) ? 3 : 4;
        if (!bEven && !yEven && pEven) return (code.getPurple() % 2 == 0) ? 5 : 6;
        if (!bEven && yEven && pEven) return (code.getBlue() % 2 != 0) ? 2 : 1;
        if (bEven && !yEven && pEven) return (code.getYellow() % 2 != 0) ? 4 : 3;
        if (bEven && yEven && !pEven) return (code.getPurple() % 2 != 0) ? 6 : 5;
        return 0;
    }

    private int getSpecificParityCategory(Code secret) {
        return getSpecificTargetParityCategory(secret, secret);
    }

    private int getNonUniqueSmallestCategory(Code code) {
        int b = code.getBlue(), y = code.getYellow(), p = code.getPurple();
        if (b <= y && b <= p) return 1;
        if (y <= b && y <= p) return 2;
        return 3;
    }

    private int getNonUniqueLargestCategory(Code code) {
        int b = code.getBlue(), y = code.getYellow(), p = code.getPurple();
        if (b >= y && b >= p) return 1;
        if (y >= b && y >= p) return 2;
        return 3;
    }

    private int getMultipleCategory(Code code) {
        int sum = code.getBlue() + code.getYellow() + code.getPurple();
        if (sum % 3 == 0) return 1;
        if (sum % 4 == 0) return 2;
        if (sum % 5 == 0) return 3;
        return 0;
    }

    private int getSpecificTargetSumCategory(Code code, int target) {
        boolean by = (code.getBlue() + code.getYellow() == target);
        boolean bp = (code.getBlue() + code.getPurple() == target);
        boolean yp = (code.getYellow() + code.getPurple() == target);
        if (by) return 1;
        if (bp) return 2;
        if (yp) return 3;
        return 0;
    }

    private int getSpecificSumEqualsCategory(Code code, int target) {
        return getSpecificTargetSumCategory(code, target);
    }

    private int getRelationToValueCategory(Code code, int val) {
        if (code.getBlue() == val) return 1;
        if (code.getBlue() > val) return 2;
        if (code.getYellow() == val) return 3;
        if (code.getYellow() > val) return 4;
        if (code.getPurple() == val) return 5;
        if (code.getPurple() > val) return 6;
        return 0;
    }

    private int getRelationToThreeCategory(Code code) {
        int b = code.getBlue(), y = code.getYellow(), p = code.getPurple();
        if (b < 3) return 1;  if (b == 3) return 2;  if (b > 3) return 3;
        if (y < 3) return 4;  if (y == 3) return 5;  if (y > 3) return 6;
        if (p < 3) return 7;  if (p == 3) return 8;  return 9;
    }

    private int getRelationToFourCategory(Code code) {
        int b = code.getBlue(), y = code.getYellow(), p = code.getPurple();
        if (b < 4) return 1;  if (b == 4) return 2;  if (b > 4) return 3;
        if (y < 4) return 4;  if (y == 4) return 5;  if (y > 4) return 6;
        if (p < 4) return 7;  if (p == 4) return 8;  return 9;
    }

    private int getMinMaxCategory(Code code) {
        int b = code.getBlue(), y = code.getYellow(), p = code.getPurple();
        if (b < y && b < p) return 1;
        if (b > y && b > p) return 2;
        if (y < b && y < p) return 3;
        if (y > b && y > p) return 4;
        if (p < b && p < y) return 5;
        return 6;
    }

    private int getBlueToSpecificColorCategory(Code code) {
        int b = code.getBlue(), y = code.getYellow(), p = code.getPurple();
        if (b < y) return 1;  if (b == y) return 2;  if (b > y) return 3;
        if (b < p) return 4;  if (b == p) return 5;  return 6;
    }

    private int getYellowToSpecificColorCategory(Code code) {
        int b = code.getBlue(), y = code.getYellow(), p = code.getPurple();
        if (y < b) return 1;  if (y == b) return 2;  if (y > b) return 3;
        if (y < p) return 4;  if (y == p) return 5;  return 6;
    }

    private int getCountOfOneOrThreeCategory(Code code) {
        int c1 = countValue(code, 1);
        int c3 = countValue(code, 3);
        if (c1 == 0) return 1; if (c1 == 1) return 2; if (c1 == 2) return 3;
        if (c3 == 0) return 4; if (c3 == 1) return 5; return 6;
    }

    private int getCountOfThreeOrFourCategory(Code code) {
        int c3 = countValue(code, 3);
        int c4 = countValue(code, 4);
        if (c3 == 0) return 1; if (c3 == 1) return 2; if (c3 == 2) return 3;
        if (c4 == 0) return 4; if (c4 == 1) return 5; return 6;
    }

    private int getCountOfOneOrFourCategory(Code code) {
        int c1 = countValue(code, 1);
        int c4 = countValue(code, 4);
        if (c1 == 0) return 1; if (c1 == 1) return 2; if (c1 == 2) return 3;
        if (c4 == 0) return 4; if (c4 == 1) return 5; return 6;
    }

    private int getTwoColorsRelationCategory(Code code) {
        int b = code.getBlue(), y = code.getYellow(), p = code.getPurple();
        if (b < y) return 1;  if (b == y) return 2;  if (b > y) return 3;
        if (b < p) return 4;  if (b == p) return 5;  if (b > p) return 6;
        if (y < p) return 7;  if (y == p) return 8;  return 9;
    }

    @Override
    public String toString() {
        return "Card #" + cardId + ": " + title;
    }
}