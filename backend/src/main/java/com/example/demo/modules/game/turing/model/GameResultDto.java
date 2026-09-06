package com.example.demo.modules.game.turing.model;

/**
 * 專門用來承載賽局終極結算結果的資料傳輸物件 (DTO)
 */
public class GameResultDto {
    private final boolean won;
    private final String message;
    private final Integer rewardTokens;

    public GameResultDto(boolean won, String message, int rewardTokens) {
        this.won = won;
        this.message = message;
        this.rewardTokens = rewardTokens;
    }

    public boolean isWon() { return won; }
    public String getMessage() { return message; }
    public Integer getRewardTokens() { return rewardTokens; }
}