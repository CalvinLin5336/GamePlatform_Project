package com.example.demo.modules.game.management.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.modules.game.management.dto.GameModeRequest;
import com.example.demo.modules.game.management.dto.GameModeView;
import com.example.demo.modules.game.management.dto.GameRequest;
import com.example.demo.modules.game.management.dto.GameView;
import com.example.demo.modules.game.management.service.GameManagementService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/admin/game-management")
public class GameAdminManagementController {
    @Autowired
    private GameManagementService gameSystemService;

    @GetMapping("/games")
    public List<GameView> findAllGames() {
        return gameSystemService.findAllGames();
    }

    @GetMapping("/games/{gameId}")
    public GameView findGame(@PathVariable Long gameId) {
        return gameSystemService.findGame(gameId, true);
    }

    @PostMapping("/games")
    public GameView createGame(@RequestBody GameRequest request) {
        return gameSystemService.createGame(request);
    }

    @PutMapping("/games/{gameId}")
    public GameView updateGame(@PathVariable Long gameId, @RequestBody GameRequest request) {
        return gameSystemService.updateGame(gameId, request);
    }

    @DeleteMapping("/games/{gameId}")
    public void deleteGame(@PathVariable Long gameId) {
        gameSystemService.deleteGame(gameId);
    }

    @PostMapping("/games/{gameId}/modes")
    public GameModeView createMode(
            @PathVariable Long gameId,
            @RequestBody GameModeRequest request) {
        return gameSystemService.createMode(gameId, request);
    }

    @PutMapping("/games/{gameId}/modes/{modeId}")
    public GameModeView updateMode(
            @PathVariable Long gameId,
            @PathVariable Long modeId,
            @RequestBody GameModeRequest request) {
        return gameSystemService.updateMode(gameId, modeId, request);
    }

    @DeleteMapping("/games/{gameId}/modes/{modeId}")
    public void deleteMode(@PathVariable Long gameId, @PathVariable Long modeId) {
        gameSystemService.deleteMode(gameId, modeId);
    }
}
