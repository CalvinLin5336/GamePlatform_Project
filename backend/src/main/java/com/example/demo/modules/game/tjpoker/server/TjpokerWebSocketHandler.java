package com.example.demo.modules.game.tjpoker.server;

import com.example.demo.modules.game.tjpoker.service.PokerGameService;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class TjpokerWebSocketHandler extends TextWebSocketHandler {
    private PokerGameService service;

    public TjpokerWebSocketHandler(PokerGameService service) {
        this.service = service;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> query = query(session.getUri());
        String roomId = query.get("roomId");
        String token = query.get("token");
        if (!service.tokenIsValid(roomId, token)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put("roomId", roomId);
        session.getAttributes().put("token", token);
        session.sendMessage(new TextMessage("connected"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object roomId = session.getAttributes().get("roomId");
        Object token = session.getAttributes().get("token");
        if (roomId != null && token != null
                && service.tokenIsValid(roomId.toString(), token.toString())) {
            service.leave(roomId.toString(), token.toString());
        }
    }

    private Map<String, String> query(URI uri) {
        Map<String, String> answer = new HashMap<String, String>();
        if (uri == null || uri.getRawQuery() == null) return answer;
        for (String item : uri.getRawQuery().split("&")) {
            String[] pair = item.split("=", 2);
            answer.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    pair.length == 2
                            ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                            : "");
        }
        return answer;
    }
}
