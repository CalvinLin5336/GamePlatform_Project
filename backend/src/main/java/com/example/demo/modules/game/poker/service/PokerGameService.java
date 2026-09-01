package com.example.demo.modules.game.poker.service;

import java.util.Map;

import com.example.demo.modules.game.poker.dto.GameView;
import com.example.demo.modules.game.poker.dto.JoinResult;
public interface PokerGameService {
    JoinResult join(String roomId, String mode, Long userId, Integer seat, String playerName);
    void leave(String roomId, String token);
    GameView view(String roomId, String token);
    GameView select(String roomId, String token, Map<Integer, Integer> choices);
    GameView confirm(String roomId, String token);
    GameView nextRound(String roomId, String token);
    GameView restart(String roomId, String token);
    GameView autoSelect(String roomId, String token);
    boolean tokenIsValid(String roomId, String token);
}
