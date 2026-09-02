package com.example.demo.modules.lobby.server; 

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component // 確保加上這個，Spring 才能把它當作 Bean 注入到 Config 裡
public class RoomWebSocketHandler extends TextWebSocketHandler {

    // 用來記錄「哪個房間」有哪些「玩家連線 (Session)」
    private static final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

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

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 玩家斷線時的清理邏輯可以寫這
        System.out.println("❌ 玩家斷開連線: " + session.getId());
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