package com.example.demo.modules.lobby.event;

public record RoomFinishedEvent(
        String roomId,
        String reason
) {
}
