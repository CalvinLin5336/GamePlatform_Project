package com.example.demo.modules.game.poker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.modules.game.poker.dto.JoinResult;
import com.example.demo.modules.game.poker.dto.GameView;
import com.example.demo.modules.game.poker.exception.GameException;
import com.example.demo.modules.game.poker.model.Card;
import com.example.demo.modules.game.poker.model.GameRoom;

class PokerGameServiceImplTests {
    private PokerGameServiceImpl service;

    @BeforeEach
    void setUp() {
        service=new PokerGameServiceImpl();
        ReflectionTestUtils.setField(service, "pokerRuleService", new PokerRuleServiceImpl());
    }

    @Test
    void playerModeAssignsDifferentSeatsAndStartsAfterBothPlayersJoin() {
        JoinResult first=service.join("room-1", GameRoom.MODE_PLAYER, 11L, "玩家甲");
        assertEquals(GameRoom.STATUS_WAITING, first.getGame().getStatus());

        JoinResult second=service.join("room-1", GameRoom.MODE_PLAYER, 22L, "玩家乙");
        assertNotEquals(first.getSeat(), second.getSeat());
        assertEquals(GameRoom.STATUS_PLAYING, second.getGame().getStatus());
    }

    @Test
    void reconnectKeepsTheAssignedSeat() {
        JoinResult first=service.join("room-2", GameRoom.MODE_PLAYER, 11L, "玩家甲");
        JoinResult reconnect=service.join("room-2", GameRoom.MODE_PLAYER, 11L, "玩家甲");

        assertEquals(first.getSeat(), reconnect.getSeat());
        assertEquals(GameRoom.STATUS_WAITING, reconnect.getGame().getStatus());
    }

    @Test
    void thirdPlayerCannotJoinAFullRoom() {
        service.join("room-3", GameRoom.MODE_PLAYER, 11L, "玩家甲");
        service.join("room-3", GameRoom.MODE_PLAYER, 22L, "玩家乙");

        GameException error=assertThrows(GameException.class,
                () -> service.join("room-3", GameRoom.MODE_PLAYER, 33L, "玩家丙"));
        assertEquals("ROOM_FULL", error.getCode());
    }

    @Test
    void computerModeKeepsHumanInSeatOne() {
        JoinResult joined=service.join("room-4", GameRoom.MODE_COMPUTER, 11L, "玩家甲");

        assertEquals(1, joined.getSeat());
        assertEquals(GameRoom.STATUS_PLAYING, joined.getGame().getStatus());
    }

    @Test
    void roomIdIsTrimmedAndInvalidModeIsRejectedByTheService() {
        JoinResult first=service.join("  room-trim  ", GameRoom.MODE_PLAYER, 11L, "玩家甲");
        JoinResult second=service.join("room-trim", GameRoom.MODE_PLAYER, 22L, "玩家乙");

        assertEquals("room-trim", first.getRoomId());
        assertEquals(GameRoom.STATUS_PLAYING, second.getGame().getStatus());
        GameException error=assertThrows(GameException.class,
                () -> service.join("room-invalid", "UNKNOWN", 33L, "玩家丙"));
        assertEquals("INVALID_MODE", error.getCode());
    }

    @Test
    void redSuitComparisonUsesStringContent() {
        PokerRuleServiceImpl rules=new PokerRuleServiceImpl();

        assertTrue(rules.isRedSuit(new Card(new String("♡"), 1, 0)));
        assertTrue(rules.isRedSuit(new Card(new String("♢"), 1, 1)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void serverKeepsRoundResultForThreeSecondsBeforeOpeningNextRound() {
        JoinResult joined=service.join("room-5", GameRoom.MODE_COMPUTER, 11L, "玩家甲");
        Map<String, GameRoom> rooms=(Map<String, GameRoom>)ReflectionTestUtils.getField(service, "rooms");
        GameRoom room=rooms.get("room-5");
        room.setStatus(GameRoom.STATUS_ROUND_RESULT);
        room.setRoundResultEndsAt(System.currentTimeMillis()+3000);

        GameView paused=service.view("room-5", joined.getToken());
        assertEquals(GameRoom.STATUS_ROUND_RESULT, paused.getStatus());
        assertTrue(paused.getRoundResultRemainingMillis()>0);
        GameException early=assertThrows(GameException.class,
                () -> service.nextRound("room-5", joined.getToken()));
        assertEquals("RESULT_PAUSE", early.getCode());

        room.setRoundResultEndsAt(System.currentTimeMillis()-1);
        GameView advanced=service.view("room-5", joined.getToken());
        assertEquals(GameRoom.STATUS_PLAYING, advanced.getStatus());
        assertEquals(2, advanced.getCurrentRound());
        assertEquals(0, advanced.getRoundResultRemainingMillis());
    }

    @Test
    @SuppressWarnings("unchecked")
    void bothPlayersUseTheSameServerDeadlineAndEnterTheSameNextRound() {
        JoinResult first=service.join("room-6", GameRoom.MODE_PLAYER, 11L, "玩家甲");
        JoinResult second=service.join("room-6", GameRoom.MODE_PLAYER, 22L, "玩家乙");
        Map<String, GameRoom> rooms=(Map<String, GameRoom>)ReflectionTestUtils.getField(service, "rooms");
        GameRoom room=rooms.get("room-6");
        room.setStatus(GameRoom.STATUS_ROUND_RESULT);
        room.setRoundResultEndsAt(System.currentTimeMillis()+3000);

        GameView firstPaused=service.view("room-6", first.getToken());
        GameView secondPaused=service.view("room-6", second.getToken());
        assertEquals(GameRoom.STATUS_ROUND_RESULT, firstPaused.getStatus());
        assertEquals(GameRoom.STATUS_ROUND_RESULT, secondPaused.getStatus());
        assertTrue(Math.abs(firstPaused.getRoundResultRemainingMillis()
                -secondPaused.getRoundResultRemainingMillis())<100);

        room.setRoundResultEndsAt(System.currentTimeMillis()-1);
        GameView firstAdvanced=service.view("room-6", first.getToken());
        GameView secondAdvanced=service.view("room-6", second.getToken());
        assertEquals(GameRoom.STATUS_PLAYING, firstAdvanced.getStatus());
        assertEquals(GameRoom.STATUS_PLAYING, secondAdvanced.getStatus());
        assertEquals(2, firstAdvanced.getCurrentRound());
        assertEquals(2, secondAdvanced.getCurrentRound());
    }
}
