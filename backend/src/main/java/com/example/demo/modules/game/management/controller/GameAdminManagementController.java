package com.example.demo.modules.game.management.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
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
import com.example.demo.modules.game.management.service.GameAdminAuthorizationService;
import com.example.demo.modules.game.management.service.GameManagementService;

@RestController
@RequestMapping("/api/admin/game-management")
public class GameAdminManagementController {
    private final GameManagementService gameSystemService;
    private final GameAdminAuthorizationService authorizationService;

    public GameAdminManagementController(
            GameManagementService gameSystemService,
            GameAdminAuthorizationService authorizationService) {
        this.gameSystemService = gameSystemService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/games")
    public List<GameView> findAllGames(Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        return gameSystemService.findAllGames();
    }

    @GetMapping("/games/{gameId}")
    public GameView findGame(@PathVariable Long gameId, Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        return gameSystemService.findGame(gameId, true);
    }

    @PostMapping("/games")
    public GameView createGame(@RequestBody GameRequest request, Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        return gameSystemService.createGame(request);
    }

    @PutMapping("/games/{gameId}")
    public GameView updateGame(
            @PathVariable Long gameId,
            @RequestBody GameRequest request,
            Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        return gameSystemService.updateGame(gameId, request);
    }

    @DeleteMapping("/games/{gameId}")
    public void deleteGame(@PathVariable Long gameId, Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        gameSystemService.deleteGame(gameId);
    }

    @PostMapping("/games/{gameId}/modes")
    public GameModeView createMode(
            @PathVariable Long gameId,
            @RequestBody GameModeRequest request,
            Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        return gameSystemService.createMode(gameId, request);
    }

    @PutMapping("/games/{gameId}/modes/{modeId}")
    public GameModeView updateMode(
            @PathVariable Long gameId,
            @PathVariable Long modeId,
            @RequestBody GameModeRequest request,
            Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        return gameSystemService.updateMode(gameId, modeId, request);
    }

    @DeleteMapping("/games/{gameId}/modes/{modeId}")
    public void deleteMode(
            @PathVariable Long gameId,
            @PathVariable Long modeId,
            Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        gameSystemService.deleteMode(gameId, modeId);
    }
}
