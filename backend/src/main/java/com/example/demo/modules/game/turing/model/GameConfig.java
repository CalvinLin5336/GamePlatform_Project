package com.example.demo.modules.game.turing.model;

import jakarta.persistence.*;

@Entity
@Table(name = "game_config")
public class GameConfig {
    
    @Id // 以此為主鍵
    @Column(name = "difficulty_char")
    private Character difficultyChar; // JPA 建議使用物件型別 Character 取代基本型別 char
    
    @Column(name = "base_reward")
    private Integer baseReward;
    
    @Column(name = "speed_bonus_threshold")
    private Integer speedBonusThreshold;
    
    @Column(name = "speed_bonus_amount")
    private Integer speedBonusAmount;

    public GameConfig() {} // JPA 規定 Entity 必須要有預設建構子

    public GameConfig(Character difficultyChar, int baseReward, int speedBonusThreshold, int speedBonusAmount) {
        this.difficultyChar = difficultyChar;
        this.baseReward = baseReward;
        this.speedBonusThreshold = speedBonusThreshold;
        this.speedBonusAmount = speedBonusAmount;
    }

    // Getters & Setters
    public Character getDifficultyChar() { return difficultyChar; }
    public void setDifficultyChar(Character difficultyChar) { this.difficultyChar = difficultyChar; }

	public Integer getBaseReward() {
		return baseReward;
	}

	public void setBaseReward(Integer baseReward) {
		this.baseReward = baseReward;
	}

	public Integer getSpeedBonusThreshold() {
		return speedBonusThreshold;
	}

	public void setSpeedBonusThreshold(Integer speedBonusThreshold) {
		this.speedBonusThreshold = speedBonusThreshold;
	}

	public Integer getSpeedBonusAmount() {
		return speedBonusAmount;
	}

	public void setSpeedBonusAmount(Integer speedBonusAmount) {
		this.speedBonusAmount = speedBonusAmount;
	}

}