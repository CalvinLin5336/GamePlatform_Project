package com.example.demo.modules.board.server;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.*;
@Configuration
@EnableWebSocket
@EnableScheduling
@RequiredArgsConstructor
public class BoardWebSocketConfig implements WebSocketConfigurer {
    private final BoardWebSocketHandler handler;
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 與既有 Board CORS 一致；私人訊息仍須通過 User JWT 驗證。
        registry.addHandler(handler, "/ws/board").setAllowedOriginPatterns("*");
    }
}
