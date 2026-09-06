package com.example.demo.modules.game.turing.service.impl;

import com.example.demo.modules.game.turing.model.Code;
import com.example.demo.modules.game.turing.model.GameRecord;
import com.example.demo.modules.game.turing.model.GameResultDto;
import com.example.demo.modules.game.turing.model.Puzzle;
import com.example.demo.modules.game.turing.model.TuringUser;

import java.util.List;

/**
 * Service Layer Interface representing the game engine business operations.
 */
public interface GameService {

    /**
     * Initializes a standard preset puzzle by its ID or difficulty.
     * @param difficulty "Easy", "Standard", or "Hard"
     * @return the initialized Puzzle object
     */
    Puzzle createNewPuzzle(String difficulty);

    /**
     * Initializes a randomized puzzle from the question bank with a specific card count.
     * @param kCount the card count (e.g., 4 or 5), or <= 0 for any count
     * @return the initialized Puzzle object
     */
    Puzzle createRandomPuzzleFromBank(int kCount);

    /**
     * Verifies a proposal code against a single verifier card in a puzzle.
     * @param proposal the 3-digit proposal code
     * @param verifierId the ID of the verifier card to test
     * @param secret the secret code for this puzzle
     * @return true if proposal and secret behave identically under the card's rules, false otherwise
     */
    boolean verifyProposal(Code proposal, int verifierId, Code secret);

    /**
     * Checks if the proposal code matches the secret code.
     * @param proposal the proposal code
     * @param secret the actual secret code
     * @return true if exactly equal, false otherwise
     */
    boolean checkWin(Code proposal, Code secret);

    /**
     * Logs and saves a gameplay result.
     * @param record the record containing player stats and final result
     */
    void saveRecord(GameRecord record);

    /**
     * Retrieves the entire player history.
     * @return list of historical game play records
     */
    List<GameRecord> getHistory();

    /**
     * Clears all historical records.
     */
    void clearHistory();
    
    public int determineCardCount(String uiSelectedText);
    
    /**
     * 🟢 全新修改：驗證最終推測密碼並動態計算對應難度的代幣獎勵
     * @param activePuzzle 當前賽局題目
     * @param currentUser 當前挑戰玩家
     * @param currentRound 當前消耗輪次
     * @param totalTests 累計測試次數
     * @param b 藍色推測值
     * @param y 黃色推測值
     * @param p 紫色推測值
     * @param diffChar 當前賽局難度代號 ('A':簡單, 'B':標準, 'C':困難)
     * @return 封裝勝負結果與動態獎勵公告的 DTO
     */
    public GameResultDto verifyFinalGuess(Puzzle activePuzzle, TuringUser currentUser, int currentRound, int totalTests, int b, int y, int p, char diffChar);
}