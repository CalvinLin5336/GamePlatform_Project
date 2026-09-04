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
            poker.setDescription("三輪撲克策略對戰");
            poker.setFrontendPath("/pages/Games/poker/poker_client.html");
            poker.setBackendPath("/api/games/poker");
            poker.setImagePath("/assets/Games/poker/poker_game_icon.png");
            poker.setEnabled(true);
            poker = gameRepository.save(poker);
        }

        migrateStaticPaths(
                poker,
                "/src/pages/Games/poker/poker_client.html",
                "/pages/Games/poker/poker_client.html",
                "/src/assets/Games/poker/poker_game_icon.png",
                "/assets/Games/poker/poker_game_icon.png");
        migratePokerDescription(poker);

        createModeIfMissing(poker.getGameId(), "COMPUTER", "對戰電腦", 1, 1, 1, true);
        createModeIfMissing(poker.getGameId(), "PLAYER", "玩家對戰", 2, 2, 0, true);

        Game tjpoker = gameRepository.findByGameCode("TJPOKER").orElse(null);
        if (tjpoker == null) {
            tjpoker = new Game();
            tjpoker.setGameCode("TJPOKER");
            tjpoker.setGameName("田忌撲克（測試版）");
            tjpoker.setDescription("保留原始測試入口，不提供房間系統的一般玩家選擇。");
            tjpoker.setFrontendPath("/pages/Games/tjpoker/poker_client.html");
            tjpoker.setBackendPath("/api/poker");
            tjpoker.setImagePath("/assets/Games/tjpoker/poker_game_icon.png");
            tjpoker.setEnabled(false);
            tjpoker = gameRepository.save(tjpoker);
        }

        migrateStaticPaths(
                tjpoker,
                "/src/pages/Games/tjpoker/poker_client.html",
                "/pages/Games/tjpoker/poker_client.html",
                "/src/assets/Games/tjpoker/poker_game_icon.png",
                "/assets/Games/tjpoker/poker_game_icon.png");

        createModeIfMissing(tjpoker.getGameId(), "COMPUTER", "對戰電腦", 1, 1, 1, false);
        createModeIfMissing(tjpoker.getGameId(), "PLAYER", "玩家對戰", 2, 2, 0, false);

        Game quiz = gameRepository.findByGameCode("QUIZ").orElse(null);
        if (quiz == null) {
            quiz = new Game();
            quiz.setGameCode("QUIZ");
            quiz.setGameName("限時問答挑戰");
            quiz.setDescription("20 題計時問答挑戰，內容涵蓋電腦科學與軟體開發基礎知識。");
            quiz.setFrontendPath("/pages/Games/quiz/quiz_client.html");
            quiz.setBackendPath("/api/quiz");
            quiz.setImagePath("/assets/Games/quiz/quiz_game_icon.png");
            quiz.setEnabled(true);
            quiz = gameRepository.save(quiz);
        }

        migrateStaticPaths(
                quiz,
                "/src/pages/Games/quiz/quiz_client.html",
                "/pages/Games/quiz/quiz_client.html",
                "/src/assets/Games/quiz/quiz_game_icon.png",
                "/assets/Games/quiz/quiz_game_icon.png");

        createModeIfMissing(quiz.getGameId(), "SINGLE", "單人挑戰", 1, 1, 0, true);
    }

    private void migrateStaticPaths(
            Game game,
            String oldFrontendPath,
            String newFrontendPath,
            String oldImagePath,
            String newImagePath) {
        boolean changed = false;
        if (oldFrontendPath.equals(game.getFrontendPath())) {
            game.setFrontendPath(newFrontendPath);
            changed = true;
        }
        if (oldImagePath.equals(game.getImagePath())) {
            game.setImagePath(newImagePath);
            changed = true;
        }
        if (changed) gameRepository.save(game);
    }

    private void migratePokerDescription(Game poker) {
        String oldDescription = "第一輪選 3 張牌，第二、三輪各選 5 張牌的三輪撲克遊戲。";
        if (oldDescription.equals(poker.getDescription())) {
            poker.setDescription("三輪撲克策略對戰");
            gameRepository.save(poker);
        }
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
        mode = gameModeRepository.save(mode);
    }
}
