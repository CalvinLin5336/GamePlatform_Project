package com.example.demo.modules.chat.listener;

import com.example.demo.modules.lobby.event.RoomFinishedEvent;
import com.example.demo.modules.lobby.server.RoomWebSocketHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RoomFinishedEventListener {

    private final RoomWebSocketHandler roomWebSocketHandler;

    public RoomFinishedEventListener(RoomWebSocketHandler roomWebSocketHandler) {
        this.roomWebSocketHandler = roomWebSocketHandler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomFinished(RoomFinishedEvent event) {
        roomWebSocketHandler.closeRoomChannel(
                event.roomId(),
                event.reason()
        );
    }
}
