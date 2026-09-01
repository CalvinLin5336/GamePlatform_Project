package com.example.demo.modules.game.management.service;

import java.util.List;

import com.example.demo.modules.game.management.dto.GameModeRequest;
import com.example.demo.modules.game.management.dto.GameModeView;
import com.example.demo.modules.game.management.dto.GameRequest;
import com.example.demo.modules.game.management.dto.GameView;

public interface GameManagementService {
    List<GameView> findEnabledGames();
    List<GameView> findAllGames();
    GameView findGame(Long gameId, boolean admin);
    GameView createGame(GameRequest request);
    GameView updateGame(Long gameId, GameRequest request);
    void deleteGame(Long gameId);
    GameModeView createMode(Long gameId, GameModeRequest request);
    GameModeView updateMode(Long gameId, Long modeId, GameModeRequest request);
    void deleteMode(Long gameId, Long modeId);
}
