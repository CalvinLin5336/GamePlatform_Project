package com.example.demo.modules.game.turing.service;

import com.example.demo.modules.game.turing.model.TuringUser;

import java.util.List;
import java.util.Map;

public interface UserService {
    TuringUser login(String username, String password);
    TuringUser register(String username, String password);
    void createUser(TuringUser username);
    void updateUser(TuringUser user);
    void deleteUser(int userId);
    List<TuringUser> getAllUsers();
    void adjustUserTokens(int userId, int tokens);
    void toggleUserBlockStatus(int userId, boolean blocked);
    List<TuringUser> getLeaderboard();
    Map<String, String> getGameConfigs();
    int rewardTokensIfEligible(int userId, int roundsUsed);
    int rewardTokensIfEligible(int userId, int roundsUsed, char diffChar);
}