package com.example.demo.modules.game.tjpoker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.modules.game.tjpoker.dto.GameView;
import com.example.demo.modules.game.tjpoker.dto.JoinResult;
import com.example.demo.modules.game.tjpoker.exception.GameException;
import com.example.demo.modules.game.tjpoker.model.GameRoom;

class TjpokerGameServiceImplTests {
    private TjpokerGameServiceImpl service;

    @BeforeEach
    void setUp() {
        service=new TjpokerGameServiceImpl();
        ReflectionTestUtils.setField(service, "pokerRuleService", new TjpokerRuleServiceImpl());
    }

    @Test
    @SuppressWarnings("unchecked")
    void serverControlsThreeSecondPauseAndHumanAutoSelectOnlyUsesCurrentRound() {
        JoinResult joined=service.join("test-room", GameRoom.MODE_COMPUTER, "玩家甲");

        GameView first=service.autoSelect("test-room", joined.getToken());
        assertEquals(3, cardsInSlot(first, 1));
        assertEquals(0, cardsInSlot(first, 2));
        assertEquals(0, cardsInSlot(first, 3));

        GameView paused=service.confirm("test-room", joined.getToken());
        assertEquals(GameRoom.STATUS_ROUND_RESULT, paused.getStatus());
        assertTrue(paused.getRoundResultRemainingMillis()>0);
        assertTrue(paused.getRoundResultRemainingMillis()<=3000);
        GameException early=assertThrows(GameException.class,
                () -> service.nextRound("test-room", joined.getToken()));
        assertEquals("RESULT_PAUSE", early.getCode());

        Map<String, GameRoom> rooms=(Map<String, GameRoom>)ReflectionTestUtils.getField(service, "rooms");
        rooms.get("test-room").setRoundResultEndsAt(System.currentTimeMillis()-1);
        GameView secondRound=service.view("test-room", joined.getToken());
        assertEquals(2, secondRound.getCurrentRound());

        GameView second=service.autoSelect("test-room", joined.getToken());
        assertEquals(3, cardsInSlot(second, 1));
        assertEquals(5, cardsInSlot(second, 2));
        assertEquals(0, cardsInSlot(second, 3));
    }

    private long cardsInSlot(GameView view, int slot) {
        return view.getHand().stream().filter(card -> card.getSlot()==slot).count();
    }
}
