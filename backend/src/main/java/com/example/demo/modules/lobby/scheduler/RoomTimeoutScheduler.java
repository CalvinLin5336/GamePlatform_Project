package com.example.demo.modules.lobby.scheduler;

import com.example.demo.modules.lobby.entity.Room;
import com.example.demo.modules.lobby.repository.RoomRepository;
import com.example.demo.modules.lobby.service.RoomLifecycleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RoomTimeoutScheduler {

    private final RoomRepository roomRepository;
    private final RoomLifecycleService roomLifecycleService;

    public RoomTimeoutScheduler(
            RoomRepository roomRepository,
            RoomLifecycleService roomLifecycleService) {
        this.roomRepository = roomRepository;
        this.roomLifecycleService = roomLifecycleService;
    }

    @Scheduled(fixedRate = 60000)
    public void closeExpiredRooms() {
        LocalDateTime now = LocalDateTime.now();
        List<Room> playingRooms = roomRepository.findByStatus(RoomLifecycleService.STATUS_PLAYING);

        for (Room room : playingRooms) {
            LocalDateTime expiresAt = room.getExpiresAt();

            if (expiresAt != null && !expiresAt.isAfter(now)) {
                roomLifecycleService.finishRoom(
                        room.getId(),
                        RoomLifecycleService.END_TIMEOUT
                );
            }
        }
    }
}
