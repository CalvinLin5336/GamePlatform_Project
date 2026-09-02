package com.example.demo.modules.lobby.controller;

import com.example.demo.modules.lobby.entity.Room;
import com.example.demo.modules.lobby.repository.RoomRepository;
import com.example.demo.modules.lobby.server.RoomWebSocketHandler;
import com.example.demo.modules.game.management.repository.GameModeRepository;
import com.example.demo.modules.game.management.repository.GameRepository;
import com.example.demo.modules.game.management.model.GameMode;
import com.example.demo.modules.game.management.model.Game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

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

    // 注入隊員寫好的 Game 與 GameMode Repository
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameModeRepository gameModeRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    // =========================================================
    // 0. 取得所有已啟用的遊戲與對應模式，供前端大廳動態渲染
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
                modeMap.put("modeId", mode.getModeId());
                modeMap.put("modeCode", mode.getModeCode());
                modeMap.put("modeName", mode.getModeName());
                modeMap.put("minPlayers", mode.getMinPlayers());
                modeMap.put("maxPlayers", mode.getMaxPlayers());
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
    // 2. 建立遊戲房間
    // =========================================================
    @PostMapping("/create-room")
    public ResponseEntity<Map<String, Object>> createRoom(@RequestBody Map<String, Object> request) {
    	String hostAccount = (String) request.get("hostAccount"); 
        
        // 接收前端傳來的 gameId 與 modeCode
        Long gameId = Long.valueOf(request.get("gameId").toString());
        String modeCode = (String) request.get("modeCode");
        
        Object playerCountObj = request.get("playerCount");
        
        // 呼叫隊員寫好的方法，精準查出該模式的設定
        Optional<GameMode> optionalMode = gameModeRepository.findByGameIdAndModeCode(gameId, modeCode);
        
        if (optionalMode.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "找不到該遊戲模式的設定資料！");
            return ResponseEntity.badRequest().body(error);
        }

        GameMode modeConfig = optionalMode.get();

        // 決定最終人數：如果前端有傳，就用前端的；如果沒傳(或發生錯誤)，就退回使用資料庫的預設最大值
        int finalMaxPlayers = modeConfig.getMaxPlayers();
        if (playerCountObj != null) {
            try {
                finalMaxPlayers = Integer.parseInt(playerCountObj.toString());
            } catch (NumberFormatException e) {
                // 忽略錯誤，維持預設值
            }
        }
        
        // 建立新的 Room 物件並寫入限制條件
        Room newRoom = new Room();
        newRoom.setHostAccount(hostAccount); 
        newRoom.setGameId(gameId);
        newRoom.setModeId(modeConfig.getModeId()); 
        newRoom.setMinPlayers(modeConfig.getMinPlayers()); 
        newRoom.setMaxPlayers(finalMaxPlayers);
        newRoom.setComputerPlayers(modeConfig.getComputerPlayers()); 
        
        // 房主開房的同時，自動把房主加入玩家名單中
        newRoom.getPlayers().add(hostAccount);
        
        // 存入資料庫
        Room savedRoom = roomRepository.save(newRoom);
        
        // 回傳真正的房間資料給前端
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("roomId", savedRoom.getId());
        response.put("room", savedRoom); 
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
        String playerAccount = (String) request.get("playerAccount");
        Map<String, Object> response = new HashMap<>();

        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        
        if (optionalRoom.isEmpty()) {
            response.put("success", false);
            response.put("message", "找不到該房間，請確認房號是否正確！");
            return ResponseEntity.badRequest().body(response);
        }

        Room room = optionalRoom.get();

        if (!"WAITING".equals(room.getStatus())) {
            response.put("success", false);
            response.put("message", "這場遊戲已經開始或結束，無法加入！");
            return ResponseEntity.badRequest().body(response);
        }

        // 防護網：檢查人數是否已達上限
        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            response.put("success", false);
            response.put("message", "加入失敗：房間人數已滿！");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (!room.getPlayers().contains(playerAccount)) {
            room.getPlayers().add(playerAccount); 
            roomRepository.save(room);    
            // 🌟 加入成功，廣播給全場更新名單
            broadcastRoomSync(roomId, room);
        }

        response.put("success", true);
        response.put("room", room); 
        response.put("message", "成功加入房間！");
        response.put("players", room.getPlayers()); 
        
        return ResponseEntity.ok(response);
    }
    
    // =========================================================
    // 4. 取得所有等待中的房間列表
    // =========================================================
    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> getWaitingRooms() {
        List<Room> waitingRooms = roomRepository.findByStatus("WAITING");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("rooms", waitingRooms); 
        
        return ResponseEntity.ok(response);
    }
    
    // =========================================================
    // 5. 取得單一房間詳細資料
    // =========================================================
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoomDetail(@PathVariable String roomId) {
        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        Map<String, Object> response = new HashMap<>();
        
        if (optionalRoom.isEmpty()) {
            response.put("success", false);
            response.put("message", "找不到此房間！");
            return ResponseEntity.badRequest().body(response);
        }
        
        response.put("success", true);
        response.put("room", optionalRoom.get());
        return ResponseEntity.ok(response);
    }
    
    // =========================================================
    // 6. 離開房間
    // =========================================================
    @PostMapping("/room/{roomId}/leave")
    public ResponseEntity<Map<String, Object>> leaveRoom(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {

        String playerAccount = request.get("playerAccount");
        Map<String, Object> response = new HashMap<>();

        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        if (optionalRoom.isEmpty()) {
            response.put("success", false);
            response.put("message", "找不到該房間！");
            return ResponseEntity.badRequest().body(response);
        }

        Room room = optionalRoom.get();
        room.getPlayers().remove(playerAccount);

        if (room.getHostAccount().equals(playerAccount) || room.getPlayers().isEmpty()) {
            roomRepository.delete(room);
            response.put("message", "房間已解散");
        } else {
            roomRepository.save(room);
            // 🌟 有人離開，廣播給全場更新名單
            broadcastRoomSync(roomId, room);
            response.put("message", "成功離開房間");
        }

        response.put("success", true);
        return ResponseEntity.ok(response);
    }
    
    // =========================================================
    // 7. 更新房間設定 (僅限房主)
    // =========================================================
    @PutMapping("/room/{roomId}/settings")
    public ResponseEntity<Map<String, Object>> updateRoomSettings(
            @PathVariable String roomId,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();
        
        String hostAccount = (String) request.get("hostAccount");
        Long modeId = Long.valueOf(request.get("modeId").toString());
        Integer maxPlayers = Integer.valueOf(request.get("maxPlayers").toString());

        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        if (optionalRoom.isEmpty()) {
            response.put("success", false);
            response.put("message", "找不到該房間！");
            return ResponseEntity.badRequest().body(response);
        }

        Room room = optionalRoom.get();

        if (!room.getHostAccount().equals(hostAccount)) {
            response.put("success", false);
            response.put("message", "權限不足，只有房主可以修改設定！");
            return ResponseEntity.status(403).body(response);
        }

        room.setModeId(modeId);
        room.setMaxPlayers(maxPlayers);
        roomRepository.save(room);
        
        // 🌟 廣播設定變更
        broadcastRoomSync(roomId, room);

        response.put("success", true);
        response.put("message", "設定更新成功");
        response.put("room", room); 
        
        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 8. 房主踢除玩家 API
    // =========================================================
    @PostMapping("/room/{roomId}/kick")
    public ResponseEntity<Map<String, Object>> kickPlayer(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {

        String hostAccount = request.get("hostAccount");
        String targetAccount = request.get("targetAccount"); 
        Map<String, Object> response = new HashMap<>();

        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        if (optionalRoom.isEmpty()) {
            response.put("success", false);
            response.put("message", "找不到該房間！");
            return ResponseEntity.badRequest().body(response);
        }

        Room room = optionalRoom.get();

        if (!room.getHostAccount().equals(hostAccount)) {
            response.put("success", false);
            response.put("message", "權限不足，只有房主可以踢人！");
            return ResponseEntity.status(403).body(response);
        }

        room.getPlayers().remove(targetAccount);
        roomRepository.save(room);

        // 發送 KICKED 指令給被踢的人
        try {
            Map<String, Object> kickMessage = new HashMap<>();
            kickMessage.put("action", "KICKED");
            kickMessage.put("targetAccount", targetAccount);
            RoomWebSocketHandler.broadcastToRoom(roomId, objectMapper.writeValueAsString(kickMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 廣播給留在房間裡的人更新名單
        broadcastRoomSync(roomId, room);

        response.put("success", true);
        response.put("message", "已將玩家踢出房間");
        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 輔助方法：發送房間狀態同步廣播
    // =========================================================
    private void broadcastRoomSync(String roomId, Room room) {
        try {
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("action", "SYNC_ROOM");
            wsMessage.put("roomData", room);
            
            String jsonString = objectMapper.writeValueAsString(wsMessage);
            RoomWebSocketHandler.broadcastToRoom(roomId, jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
 // =========================================================
    // 9. 房主開始遊戲
    // =========================================================
    @PostMapping("/room/{roomId}/start")
    public ResponseEntity<Map<String, Object>> startGame(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {

        String hostAccount = request.get("hostAccount");
        Map<String, Object> response = new HashMap<>();

        // 1. 尋找房間
        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        if (optionalRoom.isEmpty()) {
            response.put("success", false);
            response.put("message", "找不到該房間！");
            return ResponseEntity.badRequest().body(response);
        }

        Room room = optionalRoom.get();

        // 2. 防護網：確認是房主按的
        if (!room.getHostAccount().equals(hostAccount)) {
            response.put("success", false);
            response.put("message", "只有房主可以開始遊戲！");
            return ResponseEntity.status(403).body(response);
        }

        // 3. (選擇性) 防護網：檢查人數是否符合最低要求
        if (room.getPlayers().size() < room.getMinPlayers()) {
            response.put("success", false);
            response.put("message", "人數不足，無法開始遊戲！最低需要 " + room.getMinPlayers() + " 人。");
            return ResponseEntity.badRequest().body(response);
        }

        // 4. 更改房間狀態並存檔
        room.setStatus("PLAYING");
        roomRepository.save(room);

        // 5. 找出這款遊戲的前端網址 (假設在 games 表格中有存 frontendPath，例如 "poker.html")
        Optional<Game> optionalGame = gameRepository.findById(room.getGameId());
        String frontendUrl = "game.html"; // 預設值
        if (optionalGame.isPresent() && optionalGame.get().getFrontendPath() != null) {
            frontendUrl = optionalGame.get().getFrontendPath();
        }
        
        // 組合出最終帶有房號的網址，例如： poker.html?room=128F0C0E
        String targetUrl = frontendUrl + "?room=" + roomId;

        // 6. 廣播 START_GAME 指令給全場
        try {
            Map<String, Object> startMessage = new HashMap<>();
            startMessage.put("action", "START_GAME");
            startMessage.put("url", targetUrl);
            
            RoomWebSocketHandler.broadcastToRoom(roomId, objectMapper.writeValueAsString(startMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}