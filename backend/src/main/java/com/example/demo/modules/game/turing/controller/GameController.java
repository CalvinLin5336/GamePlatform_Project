package com.example.demo.modules.game.turing.controller;

import com.example.demo.modules.game.turing.dto.VerifyCardRequest; 
import com.example.demo.modules.game.turing.dto.FinalGuessRequest; 
import com.example.demo.modules.game.turing.model.*;

import com.example.demo.modules.game.turing.service.impl.GameService; 
import com.example.demo.modules.lobby.entity.Room;
import com.example.demo.modules.lobby.repository.RoomRepository;
import com.example.demo.modules.game.management.model.GameMode;
import com.example.demo.modules.game.management.repository.GameModeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/game")
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GameModeRepository gameModeRepository;

    // 🌟 核心解法：使用記憶體快取來儲存每個房間的正解！
    private static final Map<String, Code> activeRoomSecrets = new ConcurrentHashMap<>();

    // =========================================================
    // 1. 初始化遊戲與動態生成題目 (保留原本從 DB 撈取模式難度的邏輯)
    // =========================================================
    @GetMapping("/new")
    public ResponseEntity<Map<String, Object>> startNewGame(@RequestParam String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("找不到指定的房間資料"));
        GameMode mode = gameModeRepository.findById(room.getModeId())
                .orElseThrow(() -> new RuntimeException("找不到遊戲模式設定"));
        
        String modeCode = mode.getModeCode(); 
        String diffChar = "B"; 
        int cardCount = 5;     
        
        if (modeCode != null) {
            if (modeCode.contains("4")) { diffChar = "A"; cardCount = 4; } 
            else if (modeCode.contains("6")) { diffChar = "C"; cardCount = 6; }
        }
        
        // 生成對應難度的謎題
        Puzzle puzzle = gameService.createNewPuzzle(diffChar);
        
        // 🌟 將正解存入快取！(不信任遞送給前端的資料)
        Code secret = puzzle.getSecretCode(); 
        activeRoomSecrets.put(roomId, secret);

        Map<String, Object> response = new HashMap<>();
        response.put("puzzle", puzzle);
        response.put("difficulty", cardCount); 
        
        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 2. 玩家進行單張卡片檢驗
    // =========================================================
    @PostMapping("/verify")
    public ResponseEntity<Boolean> testVerifierCard(@RequestBody VerifyCardRequest request) {
        // 🌟 1. 從記憶體快取中拿出這個房間的真正解答
        Code secret = activeRoomSecrets.get(request.getRoomId());
        
        if (secret == null) {
            throw new RuntimeException("房間不存在或遊戲已結束");
        }

        // 🌟 2. 用記憶體裡的解答進行驗證，不信任前端
        boolean result = gameService.verifyProposal(request.getProposal(), request.getCardId(), secret);
        return ResponseEntity.ok(result);
    }

    // =========================================================
    // 3. 提交最終答案與結算 (加上了更新大廳狀態與時間)
    // =========================================================
    @PostMapping("/submit")
    @Transactional // 🌟 確保戰績與房間狀態更新能同步安全寫入
    public ResponseEntity<GameResultDto> submitFinalGuess(@RequestBody FinalGuessRequest request) {
        
        // 1. 從記憶體快取中拿出這個房間的正解
        Code secret = activeRoomSecrets.get(request.getRoomId());
        
        if (secret == null) {
            throw new RuntimeException("該房間的遊戲已結束或不存在");
        }

        // 2. 建立玩家的猜測密碼與快取正解進行比對
        Code proposal = new Code(request.getB(), request.getY(), request.getP());
        boolean isCorrect = gameService.checkWin(proposal, secret);

        int rewardTokens = isCorrect ? 10 : 0; 
        
        // 🌟 3. 組裝公佈答案的字串 (無論對錯都把正解排版好)
        String answerRevealMsg = String.format("[ 藍 %d - 黃 %d - 紫 %d ]", secret.getBlue(), secret.getYellow(), secret.getPurple());
        
        String resultMessage;
        if (isCorrect) {
            resultMessage = "🎉 恭喜你成功破譯密碼！\n正確答案是 " + answerRevealMsg;
        } else {
            resultMessage = "💥 密碼錯誤，破譯失敗！\n正確答案是 " + answerRevealMsg;
        }

        GameResultDto result = new GameResultDto(
                isCorrect,
                resultMessage,
                rewardTokens
        );

        // 4. 紀錄戰績
        GameRecord record = new GameRecord(
                request.getPlayerName(),
                request.getRoomId(),
                request.getCurrentRound(),
                request.getTotalTests(),
                secret.toString(),
                isCorrect,
                rewardTokens
        );
        gameService.saveRecord(record);
        
        // 🌟 5. 遊戲正常結束，更新大廳房間狀態並精準寫入時間
        roomRepository.findById(request.getRoomId()).ifPresent(room -> {
            room.setStatus("FINISHED");
            room.setEndReason("FINISHED");
            room.setEndedAt(LocalDateTime.now());
            roomRepository.save(room);
        });

        // 6. 遊戲結束，清空該房間的快取，釋放記憶體
        activeRoomSecrets.remove(request.getRoomId());

        return ResponseEntity.ok(result);
    }

    // =========================================================
    // 4. 專供中途放棄遊戲時呼叫，用來清除殭屍快取
    // =========================================================
    @DeleteMapping("/clear-cache/{roomId}")
    public ResponseEntity<?> clearCache(@PathVariable String roomId) {
        activeRoomSecrets.remove(roomId);
        return ResponseEntity.ok().build();
    }
}
