package com.example.demo.modules.game.system.dto;

public class LobbyUserView {
    private Long userId;
    private String username;
    private String nickname;
    private String role;
    private boolean testData;

    public LobbyUserView() { }

    public LobbyUserView(Long userId, String username, String nickname, String role, boolean testData) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
        this.testData = testData;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isTestData() { return testData; }
    public void setTestData(boolean testData) { this.testData = testData; }
}
