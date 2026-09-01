package com.example.demo.modules.game.poker.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.modules.game.management.dto.GameModeView;
import com.example.demo.modules.game.management.dto.GameView;
import com.example.demo.modules.game.management.service.GameManagementService;
import com.example.demo.modules.game.poker.dto.JoinRequest;
import com.example.demo.modules.game.poker.dto.JoinResult;
import com.example.demo.modules.game.poker.service.PokerGameService;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.service.UserService;

class PokerGameControllerTests {
    @Test
    void joinLoadsModeGameAndPlayerNameFromBackendServices() {
        PokerGameService pokerService=mock(PokerGameService.class);
        GameManagementService gameService=mock(GameManagementService.class);
        UserService userService=mock(UserService.class);
        PokerGameController controller=new PokerGameController();
        ReflectionTestUtils.setField(controller, "pokerGameService", pokerService);
        ReflectionTestUtils.setField(controller, "gameSystemService", gameService);
        ReflectionTestUtils.setField(controller, "userService", userService);

        GameModeView mode=new GameModeView();
        mode.setModeId(2L);
        mode.setGameId(1L);
        mode.setModeCode("PLAYER");
        GameView game=new GameView();
        game.setGameId(1L);
        game.setGameCode("POKER");
        UserResponse user=new UserResponse(8L, "player8", "玩家八", null, null,
                "PLAYER", "Active", null);
        JoinResult expected=mock(JoinResult.class);

        when(gameService.findMode(2L, false)).thenReturn(mode);
        when(gameService.findGame(1L, false)).thenReturn(game);
        when(userService.findById(8L)).thenReturn(user);
        when(pokerService.join("15", "PLAYER", 8L, "玩家八")).thenReturn(expected);

        JoinRequest request=new JoinRequest();
        request.setRoomId("15");
        request.setModeId(2L);
        request.setUserId(8L);

        assertSame(expected, controller.join(request));
        verify(gameService).findMode(2L, false);
        verify(gameService).findGame(1L, false);
        verify(userService).findById(8L);
        verify(pokerService).join("15", "PLAYER", 8L, "玩家八");
    }
}
