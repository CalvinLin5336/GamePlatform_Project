package com.example.demo.modules.game.turing.controller;

import com.example.demo.modules.game.turing.dto.VerifyCardRequest; 
import com.example.demo.modules.game.turing.model.*;
import com.example.demo.modules.game.turing.service.impl.GameService; 
import com.example.demo.modules.lobby.entity.Room;
import com.example.demo.modules.lobby.repository.RoomRepository;
import com.example.demo.modules.game.management.model.GameMode;
import com.example.demo.modules.game.management.repository.GameModeRepository;
import com.example.demo.modules.lobby.server.RoomWebSocketHandler; 

import com.fasterxml.jackson.databind.ObjectMapper; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; 
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*") 
public class GameController {

    @Autowired
    private GameService gameService;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private GameModeRepository gameModeRepository;
    
    // 1. 遊戲題目快取
    private static final Map<String, Puzzle> activeRoomPuzzles = new ConcurrentHashMap<>();
    
    // 2. 換輪準備狀態快取 [房號 -> [輪次 -> 準備好的玩家名單]]
    private static final Map<String, Map<Integer, Set<String>>> roomRoundReadyStates = new ConcurrentHashMap<>();
    
    // 3. 最終答案提交快取 [房號 -> [玩家名單 -> 結算成績]]
    private static final Map<String, Map<String, PlayerScore>> roomSubmissions = new ConcurrentHashMap<>();

    // 定義內部類別來儲存玩家成績
    public static class PlayerScore {
        public String playerName;
        public boolean isCorrect;
        public int rounds;
        public int tests;
        public String code;
        
        public PlayerScore(String playerName, boolean isCorrect, int rounds, int tests, String code) {
            this.playerName = playerName; 
            this.isCorrect = isCorrect; 
            this.rounds = rounds; 
            this.tests = tests; 
            this.code = code;
        }
    }

    // =========================================================
    // 1. 初始化遊戲與動態生成題目
    // =========================================================
    @GetMapping("/new")
    public ResponseEntity<Map<String, Object>> startNewGame(@RequestParam String roomId) {
        
        // 防呆機制：如果這間房已經有題目，直接回傳舊的
        if (activeRoomPuzzles.containsKey(roomId)) {
            Puzzle existingPuzzle = activeRoomPuzzles.get(roomId);
            Map<String, Object> response = new HashMap<>();
            response.put("puzzle", existingPuzzle);
            response.put("difficulty", existingPuzzle.getVerifiers().size());
            return ResponseEntity.ok(response);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("找不到房間"));
        
        // 根據房間的 ModeId 動態判斷卡片數量與難度
        GameMode mode = gameModeRepository.findById(room.getModeId())
                .orElseThrow(() -> new RuntimeException("找不到遊戲模式設定"));
        
        String modeCode = mode.getModeCode(); 
        String diffChar = "B"; 
        int cardCount = 5;     
        
        if (modeCode != null) {
            if (modeCode.contains("4")) { diffChar = "A"; cardCount = 4; } 
            else if (modeCode.contains("6")) { diffChar = "C"; cardCount = 6; }
        }
        
        Puzzle puzzle = gameService.createNewPuzzle(diffChar);
        activeRoomPuzzles.put(roomId, puzzle);

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
        Puzzle puzzle = activeRoomPuzzles.get(request.getRoomId());
        if (puzzle == null) throw new RuntimeException("遊戲已結束");
        
        boolean result = gameService.verifyProposal(request.getProposal(), request.getCardId(), puzzle.getSecretCode());
        return ResponseEntity.ok(result);
    }

    // =========================================================
    // 3. 處理多人同步：玩家按下「結束本輪」並透過 WS 廣播
    // =========================================================
    @PostMapping("/ready")
    public ResponseEntity<?> playerReadyNextRound(@RequestBody Map<String, Object> request, Principal principal) throws Exception {
        String roomId = (String) request.get("roomId");
        Integer round = (Integer) request.get("round");
        Integer totalPlayers = (Integer) request.get("totalPlayers");
        
        // 從 JWT 解析出來的 principal 拿取絕對真實身分
        String realPlayerName = (principal != null) ? principal.getName() : "未知玩家";

        roomRoundReadyStates.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(round, k -> ConcurrentHashMap.newKeySet())
                            .add(realPlayerName); 

        Set<String> readyPlayers = roomRoundReadyStates.get(roomId).get(round);

        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "ROUND_READY");
        wsMsg.put("playerName", realPlayerName); 
        wsMsg.put("round", round);
        wsMsg.put("readyCount", readyPlayers.size());
        wsMsg.put("totalPlayers", totalPlayers);
        
        RoomWebSocketHandler.broadcastToRoom(roomId, new ObjectMapper().writeValueAsString(wsMsg));
        return ResponseEntity.ok().build();
    }

    // =========================================================
    // 4. 多人結算系統：所有人提交後才判定勝負
    // =========================================================
    @PostMapping("/submit")
    @Transactional 
    public ResponseEntity<?> submitFinalGuess(@RequestBody Map<String, Object> request, Principal principal) throws Exception {
        String roomId = (String) request.get("roomId");
        int currentRound = (Integer) request.get("currentRound");
        int totalTests = (Integer) request.get("totalTests");
        int b = (Integer) request.get("b"); 
        int y = (Integer) request.get("y"); 
        int p = (Integer) request.get("p");
        int totalPlayers = (Integer) request.get("totalPlayers");

        // 結算時強制使用 JWT 真實身分
        String realPlayerName = (principal != null) ? principal.getName() : "未知玩家";

        Puzzle puzzle = activeRoomPuzzles.get(roomId);
        if (puzzle == null) throw new RuntimeException("遊戲已結束");

        Code secret = puzzle.getSecretCode();
        Code proposal = new Code(b, y, p);
        boolean isCorrect = gameService.checkWin(proposal, secret);

        // 記錄該名真實玩家的成績
        roomSubmissions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                       .put(realPlayerName, new PlayerScore(realPlayerName, isCorrect, currentRound, totalTests, proposal.toString()));

        Map<String, PlayerScore> currentSubmissions = roomSubmissions.get(roomId);
        ObjectMapper mapper = new ObjectMapper();

        // 如果還有人沒交卷，就只廣播「某人已提交」，回傳等待狀態
        if (currentSubmissions.size() < totalPlayers) {
            Map<String, Object> wsMsg = new HashMap<>();
            wsMsg.put("type", "PLAYER_SUBMITTED");
            wsMsg.put("playerName", realPlayerName);
            RoomWebSocketHandler.broadcastToRoom(roomId, mapper.writeValueAsString(wsMsg));
            
            return ResponseEntity.ok(Map.of("status", "WAITING"));
        }

        // 全員提交完畢，開始進行嚴格排序 (正確 > 輪次少 > 測試少)
        List<PlayerScore> rankings = new ArrayList<>(currentSubmissions.values());
        rankings.sort((p1, p2) -> {
            if (p1.isCorrect != p2.isCorrect) return p1.isCorrect ? -1 : 1; 
            if (p1.rounds != p2.rounds) return Integer.compare(p1.rounds, p2.rounds); 
            return Integer.compare(p1.tests, p2.tests); 
        });

        // 判定冠軍
        PlayerScore winner = rankings.get(0);
        String resultMsg = "正確答案是 " + secret.toString() + "\n\n";
        
        if (winner.isCorrect) {
            resultMsg += "🏆 獲勝者：" + winner.playerName + " (耗費 " + winner.rounds + " 輪, " + winner.tests + " 次驗證)\n";
        } else {
            resultMsg += "💥 全軍覆沒！沒有人成功破譯密碼。\n";
        }

        // 儲存每個人的戰績到資料庫
        for (PlayerScore score : rankings) {
            int rewardTokens = score.isCorrect ? 10 : 0;
            GameRecord record = new GameRecord(
                    score.playerName, roomId, score.rounds, score.tests, secret.toString(), score.isCorrect, rewardTokens
            );
            gameService.saveRecord(record);
        }
        
        roomRepository.findById(roomId).ifPresent(room -> {
            room.setStatus("FINISHED");
            room.setEndedAt(LocalDateTime.now());
            roomRepository.save(room);
        });

        // 廣播遊戲結束與排名
        Map<String, Object> wsGameOver = new HashMap<>();
        wsGameOver.put("type", "GAME_OVER");
        wsGameOver.put("title", winner.isCorrect ? "🎉 遊戲結算" : "💥 破譯失敗");
        wsGameOver.put("message", resultMsg);
        RoomWebSocketHandler.broadcastToRoom(roomId, mapper.writeValueAsString(wsGameOver));

        // 清除該房間所有快取
        activeRoomPuzzles.remove(roomId);
        roomRoundReadyStates.remove(roomId);
        roomSubmissions.remove(roomId);

        return ResponseEntity.ok(Map.of("status", "FINISHED"));
    }

    // =========================================================
    // 5. 中途放棄遊戲時呼叫，用來清除殭屍快取
    // =========================================================
    @DeleteMapping("/clear-cache/{roomId}")
    public ResponseEntity<?> clearCache(@PathVariable String roomId) {
        activeRoomPuzzles.remove(roomId);
        roomRoundReadyStates.remove(roomId);
        roomSubmissions.remove(roomId);
        return ResponseEntity.ok().build();
    }
}