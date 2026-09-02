package com.example.demo.modules.game.tjpoker.controller;

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

import com.example.demo.modules.game.tjpoker.dto.ErrorResponse;
import com.example.demo.modules.game.tjpoker.dto.GameView;
import com.example.demo.modules.game.tjpoker.dto.JoinRequest;
import com.example.demo.modules.game.tjpoker.dto.JoinResult;
import com.example.demo.modules.game.tjpoker.dto.SelectionRequest;
import com.example.demo.modules.game.tjpoker.exception.GameException;
import com.example.demo.modules.game.tjpoker.model.GameRoom;
import com.example.demo.modules.game.tjpoker.service.PokerGameService;

@CrossOrigin(origins="*")
@RestController
@RequestMapping("/api/poker")
public class TjpokerGameController {
    @Autowired
    private PokerGameService pokerGameService;

    @PostMapping("/join")
    public JoinResult join(@RequestBody JoinRequest request) {
        String mode=request.getMode();
        if(mode!=null) mode=mode.toUpperCase();
        if(!GameRoom.MODE_PLAYER.equals(mode) && !GameRoom.MODE_COMPUTER.equals(mode)) {
            throw new GameException("INVALID_MODE", "模式必須是 PLAYER 或 COMPUTER");
        }
        return pokerGameService.join(request.getRoomId(), mode, request.getPlayerName());
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
