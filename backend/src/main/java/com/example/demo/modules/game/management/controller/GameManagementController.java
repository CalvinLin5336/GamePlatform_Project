package com.example.demo.modules.game.management.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.modules.game.management.dto.GameView;
import com.example.demo.modules.game.management.service.GameManagementService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/game-management")
public class GameManagementController {
    @Autowired
    private GameManagementService gameSystemService;

    @GetMapping("/games")
    public List<GameView> findEnabledGames() {
        return gameSystemService.findEnabledGames();
    }

    @GetMapping("/games/{gameId}")
    public GameView findGame(@PathVariable Long gameId) {
        return gameSystemService.findGame(gameId, false);
    }
}
