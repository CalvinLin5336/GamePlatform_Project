package com.example.demo.modules.lobby.controller;

import com.example.demo.modules.lobby.entity.Room;
import com.example.demo.modules.lobby.repository.RoomRepository;
import com.example.demo.modules.game.management.repository.GameModeRepository;
import com.example.demo.modules.game.management.repository.GameRepository;
import com.example.demo.modules.game.management.model.GameMode;
import com.example.demo.modules.game.management.model.Game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/lobby")
@CrossOrigin(origins = "*") // 允許跨域請求，方便前端連線測試
public class LobbyController {

    // 注入 Repository 來操作資料庫
    @Autowired
    private RoomRepository roomRepository;

    // 🌟 注入隊員寫好的 Game 與 GameMode Repository
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameModeRepository gameModeRepository;

    // =========================================================
    // 0. (全新) 取得所有已啟用的遊戲與對應模式，供前端大廳動態渲染
    // =========================================================
    @GetMapping("/games-info")
    public ResponseEntity<Map<String, Object>> getGamesInfo() {
        List<Game> games = gameRepository.findByEnabledTrueOrderByGameIdAsc();
        List<Map<String, Object>> gamesData = new ArrayList<>();
        
        for (Game game : games) {
            Map<String, Object> gameMap = new HashMap<>();
            gameMap.put("gameId", game.getGameId());
            gameMap.put("gameCode", game.getGameCode());
            gameMap.put("gameName", game.getGameName());
            gameMap.put("description", game.getDescription());
            
            // 抓出這款遊戲對應的所有「已啟用」模式
            List<GameMode> modes = gameModeRepository.findByGameIdAndEnabledTrueOrderByModeIdAsc(game.getGameId());
            
            List<Map<String, Object>> modesData = new ArrayList<>();
            for (GameMode mode : modes) {
                Map<String, Object> modeMap = new HashMap<>();
                modeMap.put("modeCode", mode.getModeCode());
                modeMap.put("modeName", mode.getModeName());
                modeMap.put("minPlayers", mode.getMinPlayers());
                modesData.add(modeMap);
            }
            gameMap.put("modes", modesData);
            gamesData.add(gameMap);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("games", gamesData);
        
        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 1. 檢查玩家目前狀態 (是否在遊戲中)
    // =========================================================
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkPlayerStatus(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        
        // 暫時的模擬邏輯：預設玩家沒有在進行中的遊戲
        response.put("hasActiveGame", false);
        response.put("roomId", null);
        response.put("gameType", null);
        
        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 2. (升級) 建立遊戲房間，查出組員設定的模式限制並寫入 SQLite
    // =========================================================
    @PostMapping("/create-room")
    public ResponseEntity<Map<String, Object>> createRoom(@RequestBody Map<String, Object> request) {
        String hostName = (String) request.get("hostName"); 
        
        // 接收前端傳來的 gameId 與 modeCode
        Long gameId = Long.valueOf(request.get("gameId").toString());
        String modeCode = (String) request.get("modeCode");
        
        // 呼叫隊員寫好的方法，精準查出該模式的設定
        Optional<GameMode> optionalMode = gameModeRepository.findByGameIdAndModeCode(gameId, modeCode);
        
        if (optionalMode.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "找不到該遊戲模式的設定資料！");
            return ResponseEntity.badRequest().body(error);
        }

        GameMode modeConfig = optionalMode.get();

        // 建立新的 Room 物件並寫入限制條件
        Room newRoom = new Room();
        newRoom.setHostName(hostName);
        newRoom.setGameId(gameId);
        newRoom.setModeName(modeConfig.getModeName()); 
        newRoom.setMinPlayers(modeConfig.getMinPlayers()); 
        newRoom.setMaxPlayers(modeConfig.getMaxPlayers()); 
        newRoom.setComputerPlayers(modeConfig.getComputerPlayers()); 
        
        // 房主開房的同時，自動把房主加入玩家名單中
        newRoom.getPlayers().add(hostName);
        
        // 存入資料庫
        Room savedRoom = roomRepository.save(newRoom);
        
        // 回傳真正的房間資料給前端
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("roomId", savedRoom.getId());
        response.put("status", savedRoom.getStatus());
        response.put("message", "房間建立成功！人數限制：" + savedRoom.getMinPlayers() + "~" + savedRoom.getMaxPlayers() + "人");
        
        return ResponseEntity.ok(response);
    }
    
    // =========================================================
    // 3. 其他玩家加入房間
    // =========================================================
    @PostMapping("/join-room")
    public ResponseEntity<Map<String, Object>> joinRoom(@RequestBody Map<String, Object> request) {
        String roomId = (String) request.get("roomId");
        String playerName = (String) request.get("playerName");
        Map<String, Object> response = new HashMap<>();

        // 去資料庫找這個房間是否存在
        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        
        if (optionalRoom.isEmpty()) {
            response.put("success", false);
            response.put("message", "找不到該房間，請確認房號是否正確！");
            return ResponseEntity.badRequest().body(response);
        }

        Room room = optionalRoom.get();

        // 檢查房間狀態，如果已經開打就不能加了
        if (!"WAITING".equals(room.getStatus())) {
            response.put("success", false);
            response.put("message", "這場遊戲已經開始或結束，無法加入！");
            return ResponseEntity.badRequest().body(response);
        }

        // 檢查玩家是否已經在裡面 (防止重複加入)
        if (!room.getPlayers().contains(playerName)) {
            room.getPlayers().add(playerName); // 把玩家加入名單
            roomRepository.save(room);         // 存回資料庫
        }

        response.put("success", true);
        response.put("message", "成功加入房間！");
        response.put("players", room.getPlayers()); // 回傳目前名單給前端
        
        return ResponseEntity.ok(response);
    }
    
    // =========================================================
    // 4. 取得所有等待中的房間列表
    // =========================================================
    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> getWaitingRooms() {
        // 撈取資料
        List<Room> waitingRooms = roomRepository.findByStatus("WAITING");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("rooms", waitingRooms); 
        
        return ResponseEntity.ok(response);
    }
}