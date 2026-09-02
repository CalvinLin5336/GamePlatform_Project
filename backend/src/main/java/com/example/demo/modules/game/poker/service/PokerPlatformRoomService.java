package com.example.demo.modules.game.poker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.modules.game.management.dto.GameModeView;
import com.example.demo.modules.game.management.dto.GameView;
import com.example.demo.modules.game.management.service.GameManagementService;
import com.example.demo.modules.game.poker.exception.GameException;
import com.example.demo.modules.game.poker.model.GameRoom;
import com.example.demo.modules.lobby.entity.Room;
import com.example.demo.modules.lobby.repository.RoomRepository;

@Service
public class PokerPlatformRoomService {
    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GameManagementService gameManagementService;

    @Transactional(readOnly = true)
    public GameModeView requireJoinableRoom(
            String roomId,
            Long requestedModeId,
            String playerAccount) {
        if (roomId == null || roomId.isBlank()) {
            throw new GameException("INVALID_ROOM", "缺少平台房間編號");
        }

        Room room = roomRepository.findById(roomId.trim())
                .orElseThrow(() -> new GameException("ROOM_NOT_FOUND", "找不到平台房間"));

        if (!"PLAYING".equals(room.getStatus())) {
            throw new GameException("ROOM_NOT_STARTED", "平台房間尚未開始遊戲");
        }
        if (requestedModeId == null || !requestedModeId.equals(room.getModeId())) {
            throw new GameException("MODE_MISMATCH", "遊戲模式與平台房間不一致");
        }
        if (playerAccount == null || !room.getPlayers().contains(playerAccount)) {
            throw new GameException("PLAYER_NOT_IN_ROOM", "目前登入者不在這個平台房間內");
        }

        GameModeView mode = gameManagementService.findMode(room.getModeId(), false);
        if (!room.getGameId().equals(mode.getGameId())) {
            throw new GameException("MODE_MISMATCH", "平台房間的遊戲與模式不一致");
        }

        GameView game = gameManagementService.findGame(room.getGameId(), false);
        if (!"POKER".equals(game.getGameCode())) {
            throw new GameException("INVALID_GAME", "這個平台房間不是田忌撲克");
        }
        if (!GameRoom.MODE_PLAYER.equals(mode.getModeCode())
                && !GameRoom.MODE_COMPUTER.equals(mode.getModeCode())) {
            throw new GameException("INVALID_MODE", "這個模式不屬於田忌撲克");
        }
        return mode;
    }
}
