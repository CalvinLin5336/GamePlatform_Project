package com.example.demo.modules.chat.listener; // 請依你的專案 package 路徑調整

import com.example.demo.modules.lobby.event.RoomFinishedEvent;
import com.example.demo.modules.chat.server.ChatWebSocketHandler; // 假設你的 Chat WebSocket 處理器叫這個名字
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RoomLifecycleListener {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public RoomLifecycleListener(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @EventListener
    public void onRoomFinished(RoomFinishedEvent event) {
        // 當收到房間結束事件時，直接呼叫 Chat 處理器來關閉與清理該房間的頻道
        chatWebSocketHandler.closeRoom(
                event.roomId(),
                event.reason()
        );
    }
}