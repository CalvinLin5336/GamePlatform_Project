package com.example.demo.modules.board.server;
import com.example.demo.modules.board.service.BoardSessionService;
import com.example.demo.modules.user.service.LoginSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class BoardWebSocketHandler extends TextWebSocketHandler {
    private final BoardSessionService boardSessions;
    private final LoginSessionService loginSessions;
    private final ObjectMapper json = new ObjectMapper();
    private record Client(WebSocketSession socket, Long memberId, String authorization, long connectedAt, boolean ready) {}
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    @Override public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(4096);
        clients.put(session.getId(), new Client(new ConcurrentWebSocketSessionDecorator(session, 5000, 65536), null, null, System.currentTimeMillis(), false));
    }
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Client client = clients.get(session.getId());
            if (client == null) return;
            var body = json.readTree(message.getPayload());
            String type = body.path("type").asText();
            if (!client.ready()) {
                Long memberId = null;
                String authorization = null;
                if ("AUTH".equals(type)) {
                    authorization = "Bearer " + body.path("token").asText();
                    memberId = boardSessions.requireMember(authorization).getId();
                } else if (!"SUBSCRIBE".equals(type)) { close(client); return; }
                client = new Client(client.socket(), memberId, authorization, client.connectedAt(), true);
                clients.put(session.getId(), client);
                send(client, Map.of("type", "READY"));
            } else if ("PING".equals(type)) {
                if (valid(client)) send(client, Map.of("type", "PONG"));
            } else close(client);
        } catch (Exception e) {
            Client client = clients.get(session.getId());
            if (client != null) close(client);
        }
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { clients.remove(session.getId()); }
    @Override public void handleTransportError(WebSocketSession session, Throwable exception) {
        Client client = clients.get(session.getId());
        if (client != null) close(client);
    }
    private boolean valid(Client client) {
        if (!client.socket().isOpen()) { clients.remove(client.socket().getId()); return false; }
        try { if (client.authorization() != null) loginSessions.requireUser(client.authorization()); return true; }
        catch (RuntimeException e) { close(client); return false; }
    }
    public void publish(Long memberId, Map<String, Object> event) {
        clients.values().forEach(client -> {
            if (client.ready() && (memberId == null || memberId.equals(client.memberId())) && valid(client)) send(client, event);
        });
    }
    @Scheduled(fixedDelay = 30000)
    public void expireSessions() {
        clients.values().forEach(client -> {
            if (!client.ready() && System.currentTimeMillis() - client.connectedAt() > 15000) close(client);
            else if (client.ready()) valid(client);
        });
    }
    private void send(Client client, Map<String, Object> event) {
        try { client.socket().sendMessage(new TextMessage(json.writeValueAsString(event))); }
        catch (Exception e) { close(client); }
    }
    private void close(Client client) {
        clients.remove(client.socket().getId());
        try { client.socket().close(CloseStatus.POLICY_VIOLATION); } catch (Exception ignored) {}
    }
}
