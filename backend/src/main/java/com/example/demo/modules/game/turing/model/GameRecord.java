package com.example.demo.modules.game.turing.model;


import jakarta.persistence.*;

import java.sql.Timestamp;

/**
 * Represents a saved game play session stored in the database.
 */
@Entity
@Table(name = "game_records") // 對應資料表名稱
public class GameRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "player_name")
    private String playerName;
    
    @Column(name = "puzzle_id")
    private String puzzleId;
    
    @Column(name = "rounds_used")
    private Integer roundsUsed;
    
    @Column(name = "tests_used")
    private Integer testsUsed;
    
    @Column(name = "secret_code")
    private String secretCode;
    
    @Column(name = "is_won") // 確保對應資料庫欄位
    private boolean won;
    
    @Column(name = "played_at")
    private Timestamp playedAt;
    
    @Column(name = "tokens_rewarded")
    private Integer tokensRewarded;
    
    public GameRecord() {
    }

    public GameRecord(String playerName, String puzzleId, int roundsUsed, int testsUsed, String secretCode, boolean won) {
        this.playerName = playerName;
        this.puzzleId = puzzleId;
        this.roundsUsed = roundsUsed;
        this.testsUsed = testsUsed;
        this.secretCode = secretCode;
        this.won = won;
    }

    public GameRecord(String playerName, String puzzleId, int roundsUsed, int testsUsed, String secretCode, boolean won, int tokensRewarded) {
        this.playerName = playerName;
        this.puzzleId = puzzleId;
        this.roundsUsed = roundsUsed;
        this.testsUsed = testsUsed;
        this.secretCode = secretCode;
        this.won = won;
        this.tokensRewarded = tokensRewarded;
    }

    // Getters and Setters...
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getPuzzleId() { return puzzleId; }
    public void setPuzzleId(String puzzleId) { this.puzzleId = puzzleId; }

    public Integer getRoundsUsed() {
		return roundsUsed;
	}

	public void setRoundsUsed(Integer roundsUsed) {
		this.roundsUsed = roundsUsed;
	}

	public Integer getTestsUsed() {
		return testsUsed;
	}

	public void setTestsUsed(Integer testsUsed) {
		this.testsUsed = testsUsed;
	}

	public Integer getTokensRewarded() {
		return tokensRewarded;
	}

	public void setTokensRewarded(Integer tokensRewarded) {
		this.tokensRewarded = tokensRewarded;
	}

	public String getSecretCode() { return secretCode; }
    public void setSecretCode(String secretCode) { this.secretCode = secretCode; }
    public boolean isWon() { return won; }
    public void setWon(boolean won) { this.won = won; }
    public Timestamp getPlayedAt() { return playedAt; }
    public void setPlayedAt(Timestamp playedAt) { this.playedAt = playedAt; }


    @Override
    public String toString() {
        return "GameRecord{" +
                "id=" + id +
                ", playerName='" + playerName + '\'' +
                ", puzzleId='" + puzzleId + '\'' +
                ", roundsUsed=" + roundsUsed +
                ", testsUsed=" + testsUsed +
                ", secretCode='" + secretCode + '\'' +
                ", won=" + won +
                ", playedAt=" + playedAt +
                '}';
    }
}