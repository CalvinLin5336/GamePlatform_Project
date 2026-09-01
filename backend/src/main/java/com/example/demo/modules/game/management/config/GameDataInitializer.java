package com.example.demo.modules.game.management.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.modules.game.management.model.Game;
import com.example.demo.modules.game.management.model.GameMode;
import com.example.demo.modules.game.management.repository.GameModeRepository;
import com.example.demo.modules.game.management.repository.GameRepository;

@Component
public class GameDataInitializer implements CommandLineRunner {
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameModeRepository gameModeRepository;

    @Override
    public void run(String... args) {
        Game poker = gameRepository.findByGameCode("POKER").orElse(null);
        if (poker == null) {
            poker = new Game();
            poker.setGameCode("POKER");
            poker.setGameName("田忌撲克");
            poker.setDescription("第一輪選 3 張牌，第二、三輪各選 5 張牌的三輪撲克遊戲。");
            poker.setFrontendPath("/src/pages/Games/poker/poker_client.html");
            poker.setBackendPath("/api/games/poker");
            poker.setImagePath("/src/assets/Games/poker/poker_game_icon.png");
            poker.setEnabled(true);
            poker = gameRepository.save(poker);
        }

        createModeIfMissing(poker.getGameId(), "COMPUTER", "對戰電腦", 1, 1, 1, true);
        createModeIfMissing(poker.getGameId(), "PLAYER", "玩家對戰", 2, 2, 0, true);

        Game tjpoker = gameRepository.findByGameCode("TJPOKER").orElse(null);
        if (tjpoker == null) {
            tjpoker = new Game();
            tjpoker.setGameCode("TJPOKER");
            tjpoker.setGameName("田忌撲克（測試版）");
            tjpoker.setDescription("保留原始測試入口，不提供房間系統的一般玩家選擇。");
            tjpoker.setFrontendPath("/src/pages/Games/tjpoker/poker_client.html");
            tjpoker.setBackendPath("/api/poker");
            tjpoker.setImagePath("/src/assets/Games/tjpoker/poker_game_icon.png");
            tjpoker.setEnabled(false);
            tjpoker = gameRepository.save(tjpoker);
        }

        createModeIfMissing(tjpoker.getGameId(), "COMPUTER", "對戰電腦", 1, 1, 1, false);
        createModeIfMissing(tjpoker.getGameId(), "PLAYER", "玩家對戰", 2, 2, 0, false);
    }

    private void createModeIfMissing(
            Long gameId,
            String modeCode,
            String modeName,
            int minPlayers,
            int maxPlayers,
            int computerPlayers,
            boolean enabled) {
        if (gameModeRepository.findByGameIdAndModeCode(gameId, modeCode).isPresent()) return;

        GameMode mode = new GameMode();
        mode.setGameId(gameId);
        mode.setModeCode(modeCode);
        mode.setModeName(modeName);
        mode.setMinPlayers(minPlayers);
        mode.setMaxPlayers(maxPlayers);
        mode.setComputerPlayers(computerPlayers);
        mode.setEnabled(enabled);
        gameModeRepository.save(mode);
    }
}
