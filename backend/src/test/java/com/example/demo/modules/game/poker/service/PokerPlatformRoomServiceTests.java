package com.example.demo.modules.game.poker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.modules.game.management.dto.GameModeView;
import com.example.demo.modules.game.management.dto.GameView;
import com.example.demo.modules.game.management.service.GameManagementService;
import com.example.demo.modules.game.poker.exception.GameException;
import com.example.demo.modules.lobby.entity.Room;
import com.example.demo.modules.lobby.repository.RoomRepository;

class PokerPlatformRoomServiceTests {
    @Test
    void acceptsStartedPokerRoomContainingJwtAccount() {
        RoomRepository roomRepository=mock(RoomRepository.class);
        GameManagementService gameManagementService=mock(GameManagementService.class);
        PokerPlatformRoomService service=new PokerPlatformRoomService();
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "gameManagementService", gameManagementService);

        Room room=playingRoom();
        GameModeView mode=new GameModeView();
        mode.setModeId(2L);
        mode.setGameId(1L);
        mode.setModeCode("PLAYER");
        GameView game=new GameView();
        game.setGameId(1L);
        game.setGameCode("POKER");

        when(roomRepository.findById("ROOM1234")).thenReturn(Optional.of(room));
        when(gameManagementService.findMode(2L, false)).thenReturn(mode);
        when(gameManagementService.findGame(1L, false)).thenReturn(game);

        assertSame(mode, service.requireJoinableRoom("ROOM1234", 2L, "player8"));
    }

    @Test
    void rejectsAccountThatIsNotInPlatformRoom() {
        RoomRepository roomRepository=mock(RoomRepository.class);
        PokerPlatformRoomService service=new PokerPlatformRoomService();
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "gameManagementService",
                mock(GameManagementService.class));
        when(roomRepository.findById("ROOM1234")).thenReturn(Optional.of(playingRoom()));

        GameException error=assertThrows(GameException.class,
                () -> service.requireJoinableRoom("ROOM1234", 2L, "stranger"));
        assertEquals("PLAYER_NOT_IN_ROOM", error.getCode());
    }

    private Room playingRoom() {
        Room room=new Room();
        room.setId("ROOM1234");
        room.setGameId(1L);
        room.setModeId(2L);
        room.setStatus("PLAYING");
        room.getPlayers().add("player8");
        room.getPlayers().add("player9");
        return room;
    }
}
