package com.example.demo.modules.game.management.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.modules.game.management.dto.GameModeView;
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
}
