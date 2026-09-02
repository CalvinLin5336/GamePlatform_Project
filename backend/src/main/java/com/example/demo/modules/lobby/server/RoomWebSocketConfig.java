package com.example.demo.modules.lobby.server; // 請改成你的 package 路徑

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSocket
public class RoomWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private RoomWebSocketHandler roomWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 🌟 關鍵在這裡：使用 /* 來攔截所有動態房號，並允許跨域連線
        registry.addHandler(roomWebSocketHandler, "/ws/room/*")
                .setAllowedOrigins("*");
    }
}