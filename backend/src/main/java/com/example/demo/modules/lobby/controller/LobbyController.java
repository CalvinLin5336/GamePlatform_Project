package com.example.demo.modules.lobby.controller;

import com.example.demo.modules.lobby.entity.Room;
import com.example.demo.modules.lobby.repository.RoomRepository;
import com.example.demo.modules.lobby.server.RoomWebSocketHandler;
import com.example.demo.modules.lobby.service.RoomLifecycleService;
import com.example.demo.modules.user.repository.UserPageRepository;
import com.example.demo.modules.game.management.repository.GameModeRepository;
import com.example.demo.modules.game.management.repository.GameRepository;
import com.example.demo.modules.game.management.model.GameMode;
import com.example.demo.modules.game.management.model.Game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
public class LobbyController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameModeRepository gameModeRepository;

    @Autowired
    private RoomLifecycleService roomLifecycleService;
    
    @Autowired
    private UserPageRepository userPageRepository;

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
            gameMap.put("imagePath", game.getImagePath());
            gameMap.put("frontendPath", game.getFrontendPath()); 
            
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
        String hostName = (String) request.get("hostName"); 
        if (hostName == null || hostName.isBlank()) hostName = hostAccount;

        Long gameId = Long.valueOf(request.get("gameId").toString());
        String modeCode = (String) request.get("modeCode");
        Object playerCountObj = request.get("playerCount");
        
        Optional<GameMode> optionalMode = gameModeRepository.findByGameIdAndModeCode(gameId, modeCode);
        if (optionalMode.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "找不到該遊戲模式的設定資料！");
            return ResponseEntity.badRequest().body(error);
        }

        GameMode modeConfig = optionalMode.get();
        int finalMaxPlayers = modeConfig.getMaxPlayers();
        if (playerCountObj != null) {
            try { finalMaxPlayers = Integer.parseInt(playerCountObj.toString()); } catch (NumberFormatException e) {}
        }
        
        Room newRoom = new Room();
        newRoom.setHostAccount(hostAccount); 
        newRoom.setGameId(gameId);
        newRoom.setModeId(modeConfig.getModeId()); 
        newRoom.setMinPlayers(modeConfig.getMinPlayers()); 
        newRoom.setMaxPlayers(finalMaxPlayers);
        newRoom.setComputerPlayers(modeConfig.getComputerPlayers()); 
        
        newRoom.getPlayers().add(hostAccount);
        newRoom.getPlayerNames().put(hostAccount, hostName); 

        // 🌟 透過 UserPageRepository 取得房主頭貼
        try {
            var hostUser = userPageRepository.findByAccount(hostAccount);
            if (hostUser != null && hostUser.avatar() != null) {
                newRoom.getPlayerAvatars().put(hostAccount, hostUser.avatar());
            }
        } catch (Exception e) {
            // 略過錯誤，維持預設
        }
        
        Room savedRoom = roomRepository.save(newRoom);
        
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
        String playerName = (String) request.get("playerName"); 
        if (playerName == null || playerName.isBlank()) playerName = playerAccount;

        Map<String, Object> response = new HashMap<>();

        // 🌟 必須先從資料庫找出房間，才能進行後續檢查與修改
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

        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            response.put("success", false);
            response.put("message", "加入失敗：房間人數已滿！");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (!room.getPlayers().contains(playerAccount)) {
            room.getPlayers().add(playerAccount); 
            room.getPlayerNames().put(playerAccount, playerName); 
            
            // 🌟 正確抓取加入者的頭貼並放入 room 物件中
            try {
                var joiningUser = userPageRepository.findByAccount(playerAccount);
                if (joiningUser != null && joiningUser.avatar() != null) {
                    room.getPlayerAvatars().put(playerAccount, joiningUser.avatar());
                }
            } catch (Exception e) {
                // 略過錯誤
            }
            
            roomRepository.save(room);    
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

        if ("PLAYING".equals(room.getStatus())) {
            response.put("success", false);
            response.put("message", "遊戲進行中，無法直接離開房間。");
            return ResponseEntity.badRequest().body(response);
        }

        room.getPlayers().remove(playerAccount);
        room.getPlayerNames().remove(playerAccount); 
        room.getPlayerAvatars().remove(playerAccount); // 清理頭貼對照

        if (room.getHostAccount().equals(playerAccount) || room.getPlayers().isEmpty()) {
            try {
                Map<String, Object> disbandMsg = new HashMap<>();
                disbandMsg.put("type", "ROOM_DISBANDED");
                disbandMsg.put("message", "房主已離開，房間解散");
                broadcastAfterCommit(roomId, objectMapper.writeValueAsString(disbandMsg));
            } catch (Exception e) {
                e.printStackTrace();
            }
            roomRepository.delete(room);
            response.put("message", "房間已解散");
        } else {
            roomRepository.save(room);
            broadcastRoomSync(roomId, room);
            response.put("message", "成功離開房間");
        }

        response.put("success", true);
        return ResponseEntity.ok(response);
    }
    
    // =========================================================
    // 7. 更新房間設定
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

        Optional<GameMode> optionalMode = gameModeRepository.findById(modeId);
        if (optionalMode.isPresent()) {
            GameMode modeConfig = optionalMode.get();
            room.setMinPlayers(modeConfig.getMinPlayers());
            room.setComputerPlayers(modeConfig.getComputerPlayers());
        }
        
        room.setModeId(modeId);
        room.setMaxPlayers(maxPlayers);
        roomRepository.save(room);
        
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
    @Transactional
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
        room.getPlayerNames().remove(targetAccount);
        room.getPlayerAvatars().remove(targetAccount);
        roomRepository.save(room);

        try {
            Map<String, Object> kickMessage = new HashMap<>();
            kickMessage.put("action", "KICKED");
            kickMessage.put("targetAccount", targetAccount);
            broadcastAfterCommit(roomId, objectMapper.writeValueAsString(kickMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }

        broadcastRoomSync(roomId, room);

        response.put("success", true);
        response.put("message", "已將玩家踢出房間");
        return ResponseEntity.ok(response);
    }

    private void broadcastRoomSync(String roomId, Room room) {
        try {
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("action", "SYNC_ROOM");
            wsMessage.put("roomData", room);
            
            String jsonString = objectMapper.writeValueAsString(wsMessage);
            broadcastAfterCommit(roomId, jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // =========================================================
    // 9. 房主開始遊戲
    // =========================================================
    @PostMapping("/room/{roomId}/start")
    @Transactional
    public ResponseEntity<Map<String, Object>> startGame(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {

        String hostAccount = request.get("hostAccount");
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
            response.put("message", "只有房主可以開始遊戲！");
            return ResponseEntity.status(403).body(response);
        }

        if (!"WAITING".equals(room.getStatus())) {
            response.put("success", false);
            response.put("message", "遊戲已開始或結束，不能重複開始！");
            return ResponseEntity.badRequest().body(response);
        }

        if (room.getPlayers().size() < room.getMinPlayers()) {
            response.put("success", false);
            response.put("message", "人數不足，無法開始遊戲！最低需要 " + room.getMinPlayers() + " 人。");
            return ResponseEntity.badRequest().body(response);
        }

        roomLifecycleService.startRoom(room);

        Optional<Game> optionalGame = gameRepository.findById(room.getGameId());
        String frontendUrl = "/pages/Games/poker/poker_client.html"; 
        
        if (optionalGame.isPresent() && optionalGame.get().getFrontendPath() != null) {
            frontendUrl = optionalGame.get().getFrontendPath();
        } else {
            if (room.getGameId() != null && room.getGameId() == 3L) {
                frontendUrl = "/pages/Games/quiz/quiz_client.html";
            }
        }
        
        if (!frontendUrl.startsWith("/")) {
            frontendUrl = "/" + frontendUrl;
        }
        
        String targetUrl = frontendUrl + "?room=" + roomId;

        try {
            Map<String, Object> startMessage = new HashMap<>();
            startMessage.put("action", "START_GAME");
            startMessage.put("url", targetUrl);
            
            broadcastAfterCommit(roomId, objectMapper.writeValueAsString(startMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    private void broadcastAfterCommit(String roomId, String message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { RoomWebSocketHandler.broadcastToRoom(roomId, message); }
            });
        } else {
            RoomWebSocketHandler.broadcastToRoom(roomId, message);
        }
    }
    
    // =========================================================
    // 取得指定玩家正在進行中的房間
    // =========================================================
    @GetMapping("/my-active-rooms")
    public ResponseEntity<Map<String, Object>> getMyActiveRooms(@RequestParam String account) {
        List<Room> allPlayingRooms = roomRepository.findByStatus("PLAYING");
        List<Room> myActiveRooms = new ArrayList<>();
        
        for (Room room : allPlayingRooms) {
            if (room.getPlayers() != null && room.getPlayers().contains(account)) {
                myActiveRooms.add(room);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("rooms", myActiveRooms);
        
        return ResponseEntity.ok(response);
    }
    
 // =========================================================
    // 10. 廢棄/放棄房間
    // =========================================================
    @PostMapping("/room/{roomId}/abandon")
    @Transactional
    public ResponseEntity<Map<String, Object>> abandonRoom(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {

        String playerAccount = request.get("playerAccount");
        Map<String, Object> response = new HashMap<>();

        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        if (optionalRoom.isPresent()) {
            Room room = optionalRoom.get();
            room.setStatus("ABANDONED");
            room.setEndReason("ABANDONED");
            room.setEndedAt(java.time.LocalDateTime.now()); 
            
            roomRepository.save(room);
            
            try {
                Map<String, Object> disbandMsg = new HashMap<>();
                disbandMsg.put("type", "ROOM_DISBANDED");
                disbandMsg.put("message", "玩家 " + playerAccount + " 已經放棄遊戲，房間已廢棄！");
                broadcastAfterCommit(roomId, objectMapper.writeValueAsString(disbandMsg));
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            response.put("success", true);
            response.put("message", "房間已標記為廢棄 (ABANDONED)");
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "找不到指定的房間！");
        return ResponseEntity.badRequest().body(response);
    }
}