package com.example.demo.modules.game.poker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.modules.game.poker.dto.ErrorResponse;
import com.example.demo.modules.game.poker.dto.GameView;
import com.example.demo.modules.game.poker.dto.JoinRequest;
import com.example.demo.modules.game.poker.dto.JoinResult;
import com.example.demo.modules.game.poker.dto.SelectionRequest;
import com.example.demo.modules.game.poker.exception.GameException;
import com.example.demo.modules.game.poker.model.GameRoom;
import com.example.demo.modules.game.poker.service.PokerGameService;
import com.example.demo.modules.game.management.dto.GameModeView;
import com.example.demo.modules.game.management.service.GameManagementService;

@CrossOrigin(origins="*")
@RestController
@RequestMapping("/api/games/poker")
public class PokerGameController {
    @Autowired
    private PokerGameService pokerGameService;

    @Autowired
    private GameManagementService gameSystemService;

    @PostMapping("/join")
    public JoinResult join(@RequestBody JoinRequest request) {
        String mode=request.getMode();
        if(mode!=null) mode=mode.toUpperCase();
        if(!GameRoom.MODE_PLAYER.equals(mode) && !GameRoom.MODE_COMPUTER.equals(mode)) {
            throw new GameException("INVALID_MODE", "模式必須是 PLAYER 或 COMPUTER");
        }
        com.example.demo.modules.game.management.dto.GameView selectedGame =
                gameSystemService.findGame(request.getGameId(), false);
        if(!"POKER".equals(selectedGame.getGameCode())) {
            throw new GameException("INVALID_GAME", "這個入口只能開啟田忌撲克");
        }
        GameModeView selectedMode=null;
        for(GameModeView gameMode:selectedGame.getModes()) {
            if(gameMode.getModeId().equals(request.getModeId())) selectedMode=gameMode;
        }
        if(selectedMode==null) {
            throw new GameException("INVALID_MODE", "找不到房間選擇的遊戲模式");
        }
        if(!selectedMode.getModeCode().equals(mode)) {
            throw new GameException("MODE_MISMATCH", "遊戲模式資料不一致");
        }
        return pokerGameService.join(request.getRoomId(), mode,
                request.getUserId(), request.getSeat(), request.getPlayerName());
    }

    @GetMapping("/rooms/{roomId}")
    public GameView state(@PathVariable String roomId,
            @RequestHeader("X-Player-Token") String token) {
        return pokerGameService.view(roomId, token);
    }

    @PutMapping("/rooms/{roomId}/selection")
    public GameView select(@PathVariable String roomId,
            @RequestHeader("X-Player-Token") String token,
            @RequestBody SelectionRequest request) {
        return pokerGameService.select(roomId, token, request.getChoices());
    }

    @PostMapping("/rooms/{roomId}/confirm")
    public GameView confirm(@PathVariable String roomId,
            @RequestHeader("X-Player-Token") String token) {
        return pokerGameService.confirm(roomId, token);
    }

    @PostMapping("/rooms/{roomId}/next-round")
    public GameView nextRound(@PathVariable String roomId,
            @RequestHeader("X-Player-Token") String token) {
        return pokerGameService.nextRound(roomId, token);
    }

    @PostMapping("/rooms/{roomId}/restart")
    public GameView restart(@PathVariable String roomId,
            @RequestHeader("X-Player-Token") String token) {
        return pokerGameService.restart(roomId, token);
    }

    @PostMapping("/rooms/{roomId}/auto-select")
    public GameView autoSelect(@PathVariable String roomId,
            @RequestHeader("X-Player-Token") String token) {
        return pokerGameService.autoSelect(roomId, token);
    }

    @DeleteMapping("/rooms/{roomId}/leave")
    public void leave(@PathVariable String roomId,
            @RequestHeader("X-Player-Token") String token) {
        pokerGameService.leave(roomId, token);
    }

    @ExceptionHandler(GameException.class)
    public ResponseEntity<ErrorResponse> gameError(GameException e) {
        ErrorResponse response=new ErrorResponse(e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
