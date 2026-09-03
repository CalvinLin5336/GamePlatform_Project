package com.example.demo.modules.lobby.server; 

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {

    // 用來記錄「哪個房間」有哪些「玩家連線 (Session)」
    private static final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    
    // 用來解析 JSON 格式的工具
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 1. 從網址抓出房號 (例如 /ws/room/95F35EF8)
        String path = session.getUri().getPath();
        String roomId = path.substring(path.lastIndexOf('/') + 1);

        // 2. 假設你有從 query 抓到 playerAccount (前端傳的 ?player=...)
        String query = session.getUri().getQuery();
        String playerAccount = query != null ? query.split("=")[1] : session.getId();

        // 3. 把這個玩家的連線存入對應的房間中
        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(playerAccount, session);

        System.out.println("✅ 玩家 [" + playerAccount + "] 成功連線到房間 [" + roomId + "]");
    }

    // 🌟 這是補上的核心方法：接收玩家訊息並原封不動廣播給同房所有人
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        try {
            // 解析前端傳來的 JSON 訊息
            JsonNode node = objectMapper.readTree(payload);
            
            // 確認這是一則房間聊天訊息，並且有帶上房號
            if (node.has("type") && "ROOM_CHAT".equals(node.get("type").asText()) && node.has("roomId")) {
                String roomId = node.get("roomId").asText();
                
                // 呼叫下方的靜態廣播方法，把訊息分發給該房間
                broadcastToRoom(roomId, payload);
            }
        } catch (Exception e) {
            System.err.println("房間訊息解析失敗：" + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("❌ 玩家斷開連線: " + session.getId());
        
        // 進階建議：這裡其實可以加上從 roomSessions 移除斷線 Session 的清理邏輯
        // 避免房間解散後，記憶體裡還卡著無效的連線物件
    }
    
    public static void broadcastToRoom(String roomId, String message) {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (Exception e) {
                        System.err.println("廣播訊息失敗：" + e.getMessage());
                    }
                }
            }
        }
    }
}