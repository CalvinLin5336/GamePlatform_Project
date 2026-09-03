package com.example.demo.modules.lobby.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {

    // roomId -> playerAccount -> WebSocketSession
    private static final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // sessionId -> 房間與玩家資訊，供斷線時精準清理
    private static final Map<String, ConnectionInfo> sessionInfoMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String path = session.getUri().getPath();
        String roomId = path.substring(path.lastIndexOf('/') + 1);
        String playerAccount = getQueryParameter(session, "player");

        if (playerAccount == null || playerAccount.isBlank()) {
            playerAccount = session.getId();
        }

        roomSessions
                .computeIfAbsent(roomId, key -> new ConcurrentHashMap<>())
                .put(playerAccount, session);

        sessionInfoMap.put(
                session.getId(),
                new ConnectionInfo(roomId, playerAccount)
        );

        System.out.println("✅ 玩家 [" + playerAccount + "] 成功連線到房間 [" + roomId + "]");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();

        try {
            JsonNode node = objectMapper.readTree(payload);

            if (node.has("type")
                    && "ROOM_CHAT".equals(node.get("type").asText())
                    && node.has("roomId")) {

                String roomId = node.get("roomId").asText();
                broadcastToRoom(roomId, payload);
            }
        } catch (Exception e) {
            System.err.println("房間訊息解析失敗：" + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ConnectionInfo info = sessionInfoMap.remove(session.getId());

        if (info != null) {
            Map<String, WebSocketSession> sessions = roomSessions.get(info.roomId());

            if (sessions != null) {
                // 只有當 Map 裡現在仍是「這一條 session」時才刪除。
                // 遊戲開始時父頁面可能會用同帳號建立新連線，避免舊連線關閉時誤刪新連線。
                sessions.computeIfPresent(info.playerAccount(), (key, currentSession) ->
                        currentSession.getId().equals(session.getId()) ? null : currentSession
                );

                if (sessions.isEmpty()) {
                    roomSessions.remove(info.roomId(), sessions);
                }
            }
        }

        System.out.println("❌ 玩家斷開房間 WebSocket: " + session.getId());
    }

    public static void broadcastToRoom(String roomId, String message) {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);

        if (sessions == null) {
            return;
        }

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

    /**
     * Room 結束時由 Chat 的 RoomFinishedEventListener 呼叫。
     * 先通知前端，再關閉並清除該房間的所有 WebSocket。
     */
    public void closeRoomChannel(String roomId, String reason) {
        Map<String, WebSocketSession> sessions = roomSessions.remove(roomId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ROOM_FINISHED");
            message.put("roomId", roomId);
            message.put("reason", reason);
            message.put("message", "遊戲已結束，房間頻道已關閉");

            String json = objectMapper.writeValueAsString(message);

            for (WebSocketSession session : sessions.values()) {
                sessionInfoMap.remove(session.getId());

                if (!session.isOpen()) {
                    continue;
                }

                try {
                    session.sendMessage(new TextMessage(json));
                    session.close(CloseStatus.NORMAL);
                } catch (Exception e) {
                    System.err.println("關閉房間頻道失敗：" + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("建立 ROOM_FINISHED 訊息失敗：" + e.getMessage());
        }
    }

    private String getQueryParameter(WebSocketSession session, String parameterName) {
        if (session.getUri() == null || session.getUri().getQuery() == null) {
            return null;
        }

        String[] pairs = session.getUri().getQuery().split("&");

        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2 && parameterName.equals(parts[0])) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    private record ConnectionInfo(String roomId, String playerAccount) {
    }
}
