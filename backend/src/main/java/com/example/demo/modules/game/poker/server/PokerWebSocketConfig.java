package com.example.demo.modules.game.poker.server;

import com.example.demo.modules.game.poker.service.PokerGameService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class PokerWebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private PokerGameService pokerGameService;

    @Bean
    public PokerWebSocketHandler pokerWebSocketHandler() {
        return new PokerWebSocketHandler(pokerGameService);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pokerWebSocketHandler(), "/ws/games/poker").setAllowedOrigins("*");
    }
}
