package com.example.demo.modules.game.turing.service;

import com.example.demo.modules.game.turing.model.*;
import com.example.demo.modules.game.turing.service.impl.GameService;
import com.example.demo.modules.game.turing.repository.RecordRepository;
import com.example.demo.modules.game.turing.util.PuzzleGenerator;
import com.example.demo.modules.game.turing.util.TuringCardRegistry;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameServiceImpl implements GameService {

    @Autowired
    private RecordRepository recordRepository;
    
    // 🗑️ 刪除：不再需要注入 TuringQuestionRepository，因為我們不存題庫了！
    
    @Autowired
    private PuzzleGenerator puzzleGenerator; 

    @Autowired
    private UserService userService;

    @Override
    public Puzzle createNewPuzzle(String difficulty) {
        int cardCount = 4;
        char diffChar = 'B';
        if (difficulty.equalsIgnoreCase("Easy") || difficulty.equalsIgnoreCase("A") || difficulty.equals("4")) {
            cardCount = 4;
            diffChar = 'A';
        } else if (difficulty.equalsIgnoreCase("Hard") || difficulty.equalsIgnoreCase("C") || difficulty.equals("6")) {
            cardCount = 6;
            diffChar = 'C';
        } else {
            cardCount = 5;
            diffChar = 'B';
        }

        try {
            // 1. 準備所有密碼組合 (111 ~ 555，共 125 組)
            List<int[]> allCodes = new ArrayList<>();
            for (int b = 1; b <= 5; b++) {
                for (int y = 1; y <= 5; y++) {
                    for (int p = 1; p <= 5; p++) {
                        allCodes.add(new int[]{b, y, p});
                    }
                }
            }

            // 2. 準備所有驗證卡條件
            List<PuzzleGenerator.ActiveCondition> allConditions = new ArrayList<>();
            TuringCardRegistry.registerAll(allConditions, allCodes);

            System.out.println("✅ 密碼庫載入數量: " + allCodes.size());
            System.out.println("✅ 條件庫載入數量: " + allConditions.size());
            
            // 3. 透過 PuzzleGenerator 完全動態生成新題目
            PuzzleGenerator.PuzzleResult result = puzzleGenerator.generatePuzzle(allConditions, allCodes, diffChar, cardCount);
            
            if (result != null) {
                // 🌟 核心修改：移除存入資料庫的邏輯，直接建立前端需要的 Puzzle 物件！
                Code secretCode = new Code(result.blueAns, result.yellowAns, result.purpleAns);
                Puzzle puzzle = new Puzzle();
                
                // 設定為 -1 標示這是動態記憶體題目，不再依賴 DB 流水號
                puzzle.setPuzzleId(-1); 
                puzzle.setDifficulty(cardCount + "-張卡片模式");
                puzzle.setSecretCode(secretCode);
                
                if (result.activeConditions != null) {
                    for (PuzzleGenerator.ActiveCondition cond : result.activeConditions) {
                        puzzle.addVerifier(new VerifierCard(
                            cond.cardId, 
                            "驗證卡 #" + cond.cardId, 
                            "動態條件檢驗"
                        ));
                    }
                }
                return puzzle;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("動態生成謎題失敗：" + e.getMessage());
        }

        throw new RuntimeException("動態生成謎題失敗，無法取得結果！");
    }

    @Override
    public Puzzle createRandomPuzzleFromBank(int kCount) {
        return createNewPuzzle(String.valueOf(kCount));
    }

    @Override
    public boolean verifyProposal(Code proposal, int verifierId, Code secret) {
        VerifierCard card = new VerifierCard(verifierId, "", "");
        return card.verify(proposal, secret);
    }

    @Override
    public boolean checkWin(Code proposal, Code secret) {
        return proposal.equals(secret);
    }

    @Transactional
    @Override
    public GameResultDto verifyFinalGuess(Puzzle activePuzzle, TuringUser currentUser, int currentRound, int totalTests, int b, int y, int p, char diffChar) {
        boolean won = activePuzzle.getSecretCode().getBlue() == b 
                   && activePuzzle.getSecretCode().getYellow() == y 
                   && activePuzzle.getSecretCode().getPurple() == p;
        
        Code secretCode = activePuzzle.getSecretCode();
        String answerRevealMsg = String.format("[ 藍 %d - 黃 %d - 紫 %d ]", secretCode.getBlue(), secretCode.getYellow(), secretCode.getPurple());

        int reward = 0;
        if (won && currentUser != null && currentUser.getId() != -1) {
            try {
                reward = userService.rewardTokensIfEligible(currentUser.getId(), currentRound, diffChar); 
            } catch (Exception e) {
                System.err.println("發放獎勵代幣失敗: " + e.getMessage());
            }
        }

        StringBuilder msg = new StringBuilder();
        if (won) {
            msg.append("【解碼成功】恭喜你！推理正確！ ").append(answerRevealMsg).append(" \n");
        } else {
            msg.append("【解碼失敗】推理遺憾出錯！正確答案是 ").append(answerRevealMsg).append("\n");
        }

        return new GameResultDto(won, msg.toString(), reward);
    }

    @Override
    public void saveRecord(GameRecord record) {
        recordRepository.save(record);
    }

    @Override
    public List<GameRecord> getHistory() {
        return recordRepository.findAll();
    }

    @Override
    public void clearHistory() {
        recordRepository.deleteAll();
    }

    @Override
    public int determineCardCount(String uiSelectedText) {
        if (uiSelectedText != null && uiSelectedText.contains("6")) return 6;
        if (uiSelectedText != null && uiSelectedText.contains("4")) return 4;
        return 5;
    }
}