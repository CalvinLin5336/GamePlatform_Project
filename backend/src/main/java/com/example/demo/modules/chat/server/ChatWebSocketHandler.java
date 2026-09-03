package com.example.demo.modules.chat.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 🌟 核心修改：依據 roomId 分組存放連線
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    // 紀錄 sessionId 對應的 roomId，斷線時方便精準清理
    private static final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Client connected to chat: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        
        try {
            // 解析前端傳來的 JSON
            JsonNode jsonNode = objectMapper.readTree(payload);
            // 如果前端沒傳 roomId，預設分發到 "LOBBY" (大廳)
            String roomId = jsonNode.has("roomId") ? jsonNode.get("roomId").asText() : "LOBBY";

            // 確保該房間的清單存在，並將此連線加入該房間
            roomSessions.putIfAbsent(roomId, new CopyOnWriteArrayList<>());
            if (!roomSessions.get(roomId).contains(session)) {
                roomSessions.get(roomId).add(session);
                sessionRoomMap.put(session.getId(), roomId);
            }

            // 🌟 核心修改：只廣播給「同一個 roomId」的玩家
            for (WebSocketSession s : roomSessions.get(roomId)) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(payload));
                }
            }
        } catch (Exception e) {
            System.err.println("Chat JSON 解析失敗: " + e.getMessage());
        }
    }

    // 🌟 新增：由 RoomLifecycleListener 觸發，用來關閉並清理指定房間的聊天頻道
    public void closeRoom(String roomId, String reason) {
        var sessions = roomSessions.remove(roomId);

        if (sessions == null) {
            return;
        }

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    // 選擇性：可先送出結束訊息給前端，再關閉連線
                    // session.sendMessage(new TextMessage("{\"type\":\"ROOM_FINISHED\",\"reason\":\"" + reason + "\"}"));
                    session.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            sessionRoomMap.remove(session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 斷線時，從對應的房間中移除該玩家的連線
        String roomId = sessionRoomMap.remove(session.getId());
        if (roomId != null && roomSessions.containsKey(roomId)) {
            roomSessions.get(roomId).remove(session);
            // 如果房間空了，順手清掉釋放記憶體
            if (roomSessions.get(roomId).isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
        System.out.println("Connection closed: " + session.getId());
    }
}
