package com.example.demo.modules.game.system.service;

import java.util.List;

import com.example.demo.modules.game.system.dto.GameModeRequest;
import com.example.demo.modules.game.system.dto.GameModeView;
import com.example.demo.modules.game.system.dto.GameRequest;
import com.example.demo.modules.game.system.dto.GameView;

public interface GameSystemService {
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
