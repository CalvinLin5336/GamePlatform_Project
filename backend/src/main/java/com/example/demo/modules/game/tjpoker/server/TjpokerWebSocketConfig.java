package com.example.demo.modules.game.tjpoker.server;

import com.example.demo.modules.game.tjpoker.service.PokerGameService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class TjpokerWebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private PokerGameService pokerGameService;

    @Bean
    public TjpokerWebSocketHandler tjpokerWebSocketHandler() {
        return new TjpokerWebSocketHandler(pokerGameService);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(tjpokerWebSocketHandler(), "/ws/poker").setAllowedOrigins("*");
    }
}
