package com.example.demo.modules.game.turing.model;

import java.util.ArrayList;
import java.util.List;

public class Puzzle {
	private Integer puzzleId;
    private String difficulty; 
    private Code secretCode;
    private List<VerifierCard> verifiers;

    // 💡 必須加上預設無參數建構子，讓 Jackson 能順利反序列化 JSON
    public Puzzle() {
        this.verifiers = new ArrayList<>();
    }

    public Integer getPuzzleId() { 
        return puzzleId; 
    }

    public void setPuzzleId(Integer puzzleId) { 
        this.puzzleId = puzzleId; 
    }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Code getSecretCode() { return secretCode; }
    public void setSecretCode(Code secretCode) { this.secretCode = secretCode; }

    public List<VerifierCard> getVerifiers() { return verifiers; }
    public void setVerifiers(List<VerifierCard> verifiers) { this.verifiers = verifiers; }

    public void addVerifier(VerifierCard verifier) {
        this.verifiers.add(verifier);
    }

    @Override
    public String toString() {
        return "Puzzle " + puzzleId + " (" + difficulty + ") with " + verifiers.size() + " verifiers";
    }
}