package com.example.demo.modules.lobby.service;

import com.example.demo.modules.lobby.entity.Room;
import com.example.demo.modules.lobby.event.RoomFinishedEvent;
import com.example.demo.modules.lobby.repository.RoomRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RoomLifecycleService {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_PLAYING = "PLAYING";
    public static final String STATUS_FINISHED = "FINISHED";

    public static final String END_NORMAL = "NORMAL";
    public static final String END_ABANDONED = "ABANDONED";
    public static final String END_TIMEOUT = "TIMEOUT";

    private static final long GAME_TIMEOUT_HOURS = 1L;

    private final RoomRepository roomRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RoomLifecycleService(
            RoomRepository roomRepository,
            ApplicationEventPublisher eventPublisher) {
        this.roomRepository = roomRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Room startRoom(Room room) {
        LocalDateTime now = LocalDateTime.now();

        room.setStatus(STATUS_PLAYING);
        room.setStartedAt(now);
        room.setExpiresAt(now.plusHours(GAME_TIMEOUT_HOURS));
        room.setEndedAt(null);
        room.setEndReason(null);

        return roomRepository.save(room);
    }

    @Transactional
    public boolean finishRoom(String roomId, String reason) {
        Room room = roomRepository.findById(roomId).orElse(null);

        if (room == null) {
            return false;
        }

        if (STATUS_FINISHED.equals(room.getStatus())) {
            return false;
        }

        room.setStatus(STATUS_FINISHED);
        room.setEndedAt(LocalDateTime.now());
        room.setEndReason(reason);
        roomRepository.save(room);

        eventPublisher.publishEvent(new RoomFinishedEvent(roomId, reason));
        return true;
    }
}
