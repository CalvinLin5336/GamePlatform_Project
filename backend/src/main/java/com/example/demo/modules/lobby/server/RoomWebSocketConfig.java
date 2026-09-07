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
        // 使用 /* 攔截動態房號；未指定跨域來源時只接受同源連線。
        registry.addHandler(roomWebSocketHandler, "/ws/room/*");
    }
}
