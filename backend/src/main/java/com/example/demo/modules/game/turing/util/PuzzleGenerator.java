package com.example.demo.modules.game.turing.util;

import com.example.demo.modules.game.turing.model.TuringQuestion;
import com.example.demo.modules.game.turing.model.TuringQuestionCondition;
import com.example.demo.modules.game.turing.repository.TuringQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class PuzzleGenerator {

    @Autowired
    private TuringQuestionRepository turingQuestionRepository;

    private static final int[] MAP_12 = {7, 2, 9, 0, 4, 11, 1, 6, 3, 10, 5, 8};
    private static final int[] MAP_15 = {11, 4, 0, 8, 14, 2, 9, 13, 5, 1, 7, 12, 6, 3, 10};
    private static final int[] MAP_18 = {11, 4, 15, 0, 8, 14, 2, 9, 13, 5, 17, 1, 7, 12, 6, 16, 3, 10};

    public static String shuffleString(String input) {
        int len = input.length();
        int[] map = (len == 12) ? MAP_12 : (len == 15) ? MAP_15 : (len == 18) ? MAP_18 : null;
        if (map == null) return input;
        
        char[] shuffled = new char[len];
        for (int i = 0; i < len; i++) {
            shuffled[map[i]] = input.charAt(i);
        }
        return new String(shuffled);
    }

    public static String unshuffleString(String shuffledInput) {
        int len = shuffledInput.length();
        int[] map = (len == 12) ? MAP_12 : (len == 15) ? MAP_15 : (len == 18) ? MAP_18 : null;
        if (map == null) return shuffledInput;
        
        char[] original = new char[len];
        for (int i = 0; i < len; i++) {
            original[i] = shuffledInput.charAt(map[i]);
        }
        return new String(original);
    }

    // 🟢 核心方法：解開分數枷鎖 + 修正迴圈 Bug + 動態冗餘判斷
    public PuzzleResult generatePuzzle(
            List<ActiveCondition> allConditions, 
            List<int[]> allCodes, 
            char targetDifficulty, 
            int cardCount) throws Exception {

        if (allConditions == null || allConditions.isEmpty() || allCodes == null || allCodes.isEmpty()) {
            throw new Exception("條件或密碼庫為空，無法生成謎題");
        }

        Random random = new Random();
        
        Map<Integer, List<ActiveCondition>> cardMap = new HashMap<>();
        for (ActiveCondition cond : allConditions) {
            cardMap.computeIfAbsent(cond.cardId, k -> new ArrayList<>()).add(cond);
        }
        List<Integer> availableCardIds = new ArrayList<>(cardMap.keySet());
        
        int attemptCount = 0; 
        int MAX_ATTEMPTS = 100000; 

        // 💡 修正 1：迴圈條件改為 attemptCount < MAX_ATTEMPTS
        while (attemptCount < MAX_ATTEMPTS) {
            attemptCount++;
            
            Collections.shuffle(availableCardIds);
            List<Integer> chosenCardIds = new ArrayList<>(availableCardIds.subList(0, cardCount));
            Collections.sort(chosenCardIds); 

            // 💡 修正 2：移除 totalScore 限制，避免資訊量不足無法產生唯一解

            int ansIndex = random.nextInt(allCodes.size());
            int[] secretAns = allCodes.get(ansIndex); 

            List<ActiveCondition> activeConditions = new ArrayList<>();
            for (int cardId : chosenCardIds) {
                List<ActiveCondition> subs = cardMap.get(cardId);
                
                // 💡 修正：收集該張卡片所有符合密碼的子條件
                List<ActiveCondition> validSubsForThisCard = new ArrayList<>();
                for (ActiveCondition sub : subs) {
                    if (sub.bitMatrix.get(ansIndex)) {
                        validSubsForThisCard.add(sub);
                    }
                }
                
                // 如果這張卡有符合的子條件，隨機挑選其中一個，而不是永遠只拿第一個！
                if (!validSubsForThisCard.isEmpty()) {
                    ActiveCondition selectedSub = validSubsForThisCard.get(random.nextInt(validSubsForThisCard.size()));
                    activeConditions.add(selectedSub);
                }
            }

            if (isExcludedByDb(activeConditions)) {
                continue; 
            }

            BitSet gameIntersect = new BitSet(125);
            gameIntersect.set(0, 125); 

            for (int k = 0; k < activeConditions.size(); k++) {
                gameIntersect.and(activeConditions.get(k).bitMatrix); 
            }

            int trueCount = 0;
            for (int m = 0; m < 125; m++) {
                if (gameIntersect.get(m)) trueCount++;
            }

            // 確保是唯一解
            if (trueCount == 1 && activeConditions.size() == cardCount) {
                
                int redundantCardCount = 0;
                int totalInterference = 0; 
                
                // 逆向冗餘與干擾檢查
                for (int i = 0; i < cardCount; i++) {
                    BitSet testIntersect = new BitSet(125);
                    testIntersect.set(0, 125); 
                    
                    for (int j = 0; j < cardCount; j++) {
                        if (i == j) continue; 
                        testIntersect.and(activeConditions.get(j).bitMatrix);
                    }
                    
                    int testTrueCount = 0;
                    for (int m = 0; m < 125; m++) {
                        if (testIntersect.get(m)) testTrueCount++;
                    }
                    
                    if (testTrueCount == 1) {
                        redundantCardCount++; 
                    } else {
                        totalInterference += (testTrueCount - 1);
                    }
                }
                
             // 不管是 4 張、5 張還是 6 張卡，只要有任何一張卡是多餘的，直接淘汰重抽！
                if (redundantCardCount > 0) {
                    continue;
                }

                // 依據你的邏輯，精確判定實際干擾難度
                double avgInterference = (double) totalInterference / cardCount;
                
                // 難度干擾係數門檻維持我們剛剛放寬的設定，確保算得出題目
                double thresholdA = (cardCount >= 6) ? 1.0 : 1.5;
                double thresholdB = (cardCount >= 6) ? 2.0 : 4.0;
                
                char actualLogicDifficulty;
                if (avgInterference <= thresholdA) {
                    actualLogicDifficulty = 'A'; 
                } else if (avgInterference <= thresholdB) {
                    actualLogicDifficulty = 'B'; 
                } else {
                    actualLogicDifficulty = 'C'; 
                }

                // 動態寬容：6張卡若算出唯一解，干擾度為 B 也視為通過 C 難度
                boolean isMatch = (actualLogicDifficulty == targetDifficulty);
                if (cardCount >= 6 && targetDifficulty == 'C' && (actualLogicDifficulty == 'B' || actualLogicDifficulty == 'C')) {
                    isMatch = true;
                }

                // 如果符合玩家選擇的難度，正式出題！
                if (isMatch) {
                    PuzzleResult result = new PuzzleResult();
                    result.blueAns = secretAns[0];
                    result.yellowAns = secretAns[1];
                    result.purpleAns = secretAns[2];
                    result.cardIds = chosenCardIds;
                    result.activeConditions = activeConditions;
                    
                    result.puzzleCode = buildPuzzleCode(targetDifficulty, cardCount, activeConditions);
                    
                    System.out.println("✅ 成功生成題目！總共嘗試次數: " + attemptCount);
                    return result;
                }
            }
        } // 👈 迴圈正確地在這裡結束！
        
        // 🚨 修正 4：把拋出例外移到迴圈「外面」！只有真的跑完 10 萬次都沒中，才會報錯。
        throw new Exception("經過 " + MAX_ATTEMPTS + " 次運算，仍無法產出符合 [" + targetDifficulty + "] 難度的唯一解題目，請重試！");
    }

    public static String buildPuzzleCode(char difficulty, int cardCount, List<ActiveCondition> activeConditions) {
        StringBuilder sb10 = new StringBuilder();
        
        for (ActiveCondition cond : activeConditions) {
            String cIdStr = String.format("%02d", cond.cardId);
            int conditionSubIndex = (cond.conditionIndex % 10) + 1;
            sb10.append(cIdStr).append(conditionSubIndex);
        }

        String shuffled10 = shuffleString(sb10.toString());
        java.math.BigInteger bigNum = new java.math.BigInteger(shuffled10);
        String code36 = bigNum.toString(36).toUpperCase();

        return "#" + difficulty + cardCount + " " + code36;
    }

    // 🟢 解碼器保留
    public static PuzzleResult decodePuzzleCode(String puzzleCode, List<ActiveCondition> allConditions) throws Exception {
        try {
            puzzleCode = puzzleCode.trim();
            String[] tokens = puzzleCode.split("\\s+");
            if (tokens.length < 2) throw new IllegalArgumentException("序號格式不完整");

            String prefix = tokens[0];
            int cardCount = Integer.parseInt(prefix.substring(2));
            int expectedLength = cardCount * 3;

            java.math.BigInteger bigNum = new java.math.BigInteger(tokens[1], 36);
            String shuffled10 = bigNum.toString();
            
            while (shuffled10.length() < expectedLength) {
                shuffled10 = "0" + shuffled10;
            }

            String restored10 = unshuffleString(shuffled10);

            List<ActiveCondition> activeConditions = new ArrayList<>();
            List<Integer> cardIds = new ArrayList<>();

            for (int i = 0; i < restored10.length(); i += 3) {
                int cardId = Integer.parseInt(restored10.substring(i, i + 2));
                int subConditionNum = Integer.parseInt(restored10.substring(i + 2, i + 3));
                
                ActiveCondition targetCond = findConditionInCache(allConditions, cardId, subConditionNum);
                if (targetCond != null) {
                    activeConditions.add(targetCond);
                    if (!cardIds.contains(cardId)) cardIds.add(cardId);
                }
            }

            BitSet intersect = new BitSet(125);
            intersect.set(0, 125);
            for (ActiveCondition cond : activeConditions) {
                intersect.and(cond.bitMatrix);
            }

            int trueCount = 0;
            for (int m = 0; m < 125; m++) {
                if (intersect.get(m)) trueCount++;
            }

            if (trueCount != 1) {
                throw new IllegalStateException("該序號交集無法還原出唯一解，可能遭到竄改！");
            }

            int ansIndex = intersect.nextSetBit(0);
            int b = (ansIndex / 25) + 1;
            int y = ((ansIndex % 25) / 5) + 1;
            int p = (ansIndex % 5) + 1;

            PuzzleResult result = new PuzzleResult();
            result.puzzleCode = puzzleCode.toUpperCase();
            result.blueAns = b;
            result.yellowAns = y;
            result.purpleAns = p;
            result.cardIds = cardIds;
            result.activeConditions = activeConditions;
            return result;

        } catch (Exception e) {
            throw new Exception("序號還原失敗！錯誤原因: " + e.getMessage());
        }
    }

    private static ActiveCondition findConditionInCache(List<ActiveCondition> allConditions, int cardId, int subNum) {
        for (ActiveCondition cond : allConditions) {
            if (cond.cardId == cardId && ((cond.conditionIndex % 10) + 1) == subNum) {
                return cond;
            }
        }
        return null;
    }

    private static boolean isExcludedByDb(List<ActiveCondition> conditions) {
        return false; // 這裡可以擴充如果你想要將玩過的題目過濾掉
    }

    public static class ActiveCondition {
        public int cardId;
        public int conditionIndex;
        public BitSet bitMatrix; 
        
        public ActiveCondition(int cardId, int conditionIndex, BitSet bitMatrix) {
            this.cardId = cardId;
            this.conditionIndex = conditionIndex;
            this.bitMatrix = bitMatrix;
        }
    }

    public static class PuzzleResult {
        public String puzzleCode;
        public int blueAns;
        public int yellowAns;
        public int purpleAns;
        public List<Integer> cardIds;
        public List<ActiveCondition> activeConditions;
    }
    
    @Transactional
    public void overwriteLivePuzzle(PuzzleResult dynamicPuzzle) throws Exception {
        
        // 1. 清空所有舊題目與條件，並加上 flush 強制立刻執行 Delete SQL
        turingQuestionRepository.deleteAll();
        turingQuestionRepository.flush();
        
        // 2. 建立全新的題目 (⚠️ 絕對不要加上 q.setId(1)，讓資料庫自動派發 ID！)
        TuringQuestion q = new TuringQuestion();
        q.setAnsB(dynamicPuzzle.blueAns);
        q.setAnsY(dynamicPuzzle.yellowAns);
        q.setAnsP(dynamicPuzzle.purpleAns);
        q.setKCount(dynamicPuzzle.cardIds.size());
        
        q.setConditions(new ArrayList<>());
        
        for (ActiveCondition cond : dynamicPuzzle.activeConditions) {
            TuringQuestionCondition qc = new TuringQuestionCondition();
            qc.setConditionId(cond.cardId);
            qc.setSubIndex((cond.conditionIndex % 10) + 1); 
            qc.setDescription("Dynamic Criteria for Card " + cond.cardId);
            qc.setQuestion(q); // 關聯主表
            q.getConditions().add(qc);
        }
        
        // 3. 全新寫入！並強制刷新進資料庫，避免狀態延遲
        turingQuestionRepository.saveAndFlush(q);
    }
}