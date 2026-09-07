package com.example.demo.modules.game.management.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.modules.game.management.dto.GameModeRequest;
import com.example.demo.modules.game.management.dto.GameModeView;
import com.example.demo.modules.game.management.dto.GameRequest;
import com.example.demo.modules.game.management.model.Game;
import com.example.demo.modules.game.management.model.GameMode;
import com.example.demo.modules.game.management.repository.GameModeRepository;
import com.example.demo.modules.game.management.repository.GameRepository;

class GameManagementServiceImplTests {
    @Test
    void findModeReturnsItsGameIdAndModeCode() {
        GameRepository gameRepository=mock(GameRepository.class);
        GameModeRepository modeRepository=mock(GameModeRepository.class);
        GameManagementServiceImpl service=new GameManagementServiceImpl();
        ReflectionTestUtils.setField(service, "gameRepository", gameRepository);
        ReflectionTestUtils.setField(service, "gameModeRepository", modeRepository);

        Game game=new Game();
        game.setGameId(1L);
        game.setEnabled(true);
        GameMode mode=new GameMode();
        mode.setModeId(2L);
        mode.setGameId(1L);
        mode.setModeCode("PLAYER");
        mode.setEnabled(true);

        when(modeRepository.findById(2L)).thenReturn(Optional.of(mode));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        GameModeView found=service.findMode(2L, false);

        assertEquals(2L, found.getModeId());
        assertEquals(1L, found.getGameId());
        assertEquals("PLAYER", found.getModeCode());
    }

    @Test
    void updateGameRejectsChangingEstablishedGameCode() {
        GameRepository gameRepository=mock(GameRepository.class);
        GameModeRepository modeRepository=mock(GameModeRepository.class);
        GameManagementServiceImpl service=new GameManagementServiceImpl();
        ReflectionTestUtils.setField(service, "gameRepository", gameRepository);
        ReflectionTestUtils.setField(service, "gameModeRepository", modeRepository);

        Game game=new Game();
        game.setGameId(1L);
        game.setGameCode("QUIZ");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        GameRequest request=new GameRequest();
        request.setGameCode("OTHER");

        ResponseStatusException error=assertThrows(
                ResponseStatusException.class,
                () -> service.updateGame(1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("遊戲代碼建立後不可修改", error.getReason());
    }

    @Test
    void updateModeRejectsChangingEstablishedModeCode() {
        GameRepository gameRepository=mock(GameRepository.class);
        GameModeRepository modeRepository=mock(GameModeRepository.class);
        GameManagementServiceImpl service=new GameManagementServiceImpl();
        ReflectionTestUtils.setField(service, "gameRepository", gameRepository);
        ReflectionTestUtils.setField(service, "gameModeRepository", modeRepository);

        Game game=new Game();
        game.setGameId(1L);
        GameMode mode=new GameMode();
        mode.setModeId(2L);
        mode.setGameId(1L);
        mode.setModeCode("SINGLE");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(modeRepository.findById(2L)).thenReturn(Optional.of(mode));

        GameModeRequest request=new GameModeRequest();
        request.setModeCode("PLAYER");

        ResponseStatusException error=assertThrows(
                ResponseStatusException.class,
                () -> service.updateMode(1L, 2L, request));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("模式代碼建立後不可修改", error.getReason());
    }
}
