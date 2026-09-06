package com.example.demo.modules.game.turing.dto;

public class FinalGuessRequest {
    
    private String roomId; // 🌟 新增 roomId 欄位來接收前端傳來的房號
    
    private Integer puzzleId;
    private Integer currentRound;
    private Integer totalTests;
    private Integer b;
    private Integer y;
    private Integer p;
    private Character diffChar;
    private String playerName;

    // --- Getters and Setters ---
    
    // 🌟 新增 roomId 的 Getter 與 Setter
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    
    public Integer getCurrentRound() { return currentRound; }
    public Integer getPuzzleId() {
        return puzzleId;
    }
    public void setPuzzleId(Integer puzzleId) {
        this.puzzleId = puzzleId;
    }
    public void setCurrentRound(Integer currentRound) { this.currentRound = currentRound; }

    public Integer getTotalTests() { return totalTests; }
    public void setTotalTests(Integer totalTests) { this.totalTests = totalTests; }

    public Integer getB() { return b; }
    public void setB(Integer b) { this.b = b; }

    public Integer getY() { return y; }
    public void setY(Integer y) { this.y = y; }

    public Integer getP() { return p; }
    public void setP(Integer p) { this.p = p; }

    public Character getDiffChar() { return diffChar; }
    public void setDiffChar(Character diffChar) { this.diffChar = diffChar; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
}