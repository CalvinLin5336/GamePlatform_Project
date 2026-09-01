package com.example.demo.modules.game.management.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.modules.game.management.dto.GameModeRequest;
import com.example.demo.modules.game.management.dto.GameModeView;
import com.example.demo.modules.game.management.dto.GameRequest;
import com.example.demo.modules.game.management.dto.GameView;
import com.example.demo.modules.game.management.model.Game;
import com.example.demo.modules.game.management.model.GameMode;
import com.example.demo.modules.game.management.repository.GameModeRepository;
import com.example.demo.modules.game.management.repository.GameRepository;
import com.example.demo.modules.game.management.service.GameManagementService;

@Service
public class GameManagementServiceImpl implements GameManagementService {
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameModeRepository gameModeRepository;

    @Override
    public List<GameView> findEnabledGames() {
        List<Game> games = gameRepository.findByEnabledTrueOrderByGameIdAsc();
        return toGameViews(games, false);
    }

    @Override
    public List<GameView> findAllGames() {
        return toGameViews(gameRepository.findAll(), true);
    }

    @Override
    public GameView findGame(Long gameId, boolean admin) {
        Game game = requireGame(gameId);
        if (!admin && !game.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到遊戲");
        }
        return toGameView(game, admin);
    }

    @Override
    public GameView createGame(GameRequest request) {
        validateGame(request, null);
        Game game = new Game();
        copyGame(request, game);
        return toGameView(gameRepository.save(game), true);
    }

    @Override
    public GameView updateGame(Long gameId, GameRequest request) {
        Game game = requireGame(gameId);
        validateGame(request, gameId);
        copyGame(request, game);
        return toGameView(gameRepository.save(game), true);
    }

    @Override
    @Transactional
    public void deleteGame(Long gameId) {
        requireGame(gameId);
        gameModeRepository.deleteByGameId(gameId);
        gameRepository.deleteById(gameId);
    }

    @Override
    public GameModeView createMode(Long gameId, GameModeRequest request) {
        requireGame(gameId);
        validateMode(gameId, request, null);
        GameMode mode = new GameMode();
        mode.setGameId(gameId);
        copyMode(request, mode);
        return new GameModeView(gameModeRepository.save(mode));
    }

    @Override
    public GameModeView updateMode(Long gameId, Long modeId, GameModeRequest request) {
        requireGame(gameId);
        GameMode mode = requireMode(gameId, modeId);
        validateMode(gameId, request, modeId);
        copyMode(request, mode);
        return new GameModeView(gameModeRepository.save(mode));
    }

    @Override
    public void deleteMode(Long gameId, Long modeId) {
        requireGame(gameId);
        GameMode mode = requireMode(gameId, modeId);
        gameModeRepository.delete(mode);
    }

    private List<GameView> toGameViews(List<Game> games, boolean admin) {
        List<GameView> views = new ArrayList<GameView>();
        for (Game game : games) views.add(toGameView(game, admin));
        return views;
    }

    private GameView toGameView(Game game, boolean admin) {
        GameView view = new GameView(game);
        List<GameMode> modes = admin
                ? gameModeRepository.findByGameIdOrderByModeIdAsc(game.getGameId())
                : gameModeRepository.findByGameIdAndEnabledTrueOrderByModeIdAsc(game.getGameId());
        List<GameModeView> modeViews = new ArrayList<GameModeView>();
        for (GameMode mode : modes) modeViews.add(new GameModeView(mode));
        view.setModes(modeViews);
        return view;
    }

    private Game requireGame(Long gameId) {
        if (gameId == null || gameId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "遊戲 ID 不正確");
        }
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到遊戲"));
    }

    private GameMode requireMode(Long gameId, Long modeId) {
        if (modeId == null || modeId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模式 ID 不正確");
        }
        GameMode mode = gameModeRepository.findById(modeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到遊戲模式"));
        if (!gameId.equals(mode.getGameId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "這個遊戲沒有該模式");
        }
        return mode;
    }

    private void validateGame(GameRequest request, Long currentGameId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少遊戲資料");
        }
        requireText(request.getGameCode(), "遊戲代碼");
        requireText(request.getGameName(), "遊戲名稱");
        requireText(request.getFrontendPath(), "前端入口");
        requireText(request.getBackendPath(), "後端入口");

        gameRepository.findByGameCode(request.getGameCode().trim().toUpperCase())
                .ifPresent(found -> {
                    if (currentGameId == null || !currentGameId.equals(found.getGameId())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "遊戲代碼已存在");
                    }
                });
    }

    private void validateMode(Long gameId, GameModeRequest request, Long currentModeId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少模式資料");
        }
        requireText(request.getModeCode(), "模式代碼");
        requireText(request.getModeName(), "模式名稱");
        if (request.getMinPlayers() <= 0 || request.getMaxPlayers() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "真人玩家人數必須大於 0");
        }
        if (request.getMinPlayers() > request.getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "最少人數不能大於最多人數");
        }
        if (request.getComputerPlayers() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "電腦玩家人數不能小於 0");
        }

        gameModeRepository.findByGameIdAndModeCode(gameId, request.getModeCode().trim().toUpperCase())
                .ifPresent(found -> {
                    if (currentModeId == null || !currentModeId.equals(found.getModeId())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "模式代碼已存在");
                    }
                });
    }

    private void copyGame(GameRequest request, Game game) {
        game.setGameCode(request.getGameCode().trim().toUpperCase());
        game.setGameName(request.getGameName().trim());
        game.setDescription(trimOrNull(request.getDescription()));
        game.setFrontendPath(request.getFrontendPath().trim());
        game.setBackendPath(request.getBackendPath().trim());
        game.setImagePath(trimOrNull(request.getImagePath()));
        game.setEnabled(request.isEnabled());
    }

    private void copyMode(GameModeRequest request, GameMode mode) {
        mode.setModeCode(request.getModeCode().trim().toUpperCase());
        mode.setModeName(request.getModeName().trim());
        mode.setMinPlayers(request.getMinPlayers());
        mode.setMaxPlayers(request.getMaxPlayers());
        mode.setComputerPlayers(request.getComputerPlayers());
        mode.setEnabled(request.isEnabled());
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + "不可空白");
        }
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
