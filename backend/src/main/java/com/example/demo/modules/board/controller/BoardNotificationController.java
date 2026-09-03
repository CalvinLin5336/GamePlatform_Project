package com.example.demo.modules.board.controller;
import com.example.demo.modules.board.dto.BoardPage;
import com.example.demo.modules.board.entity.NotificationCategory;
import com.example.demo.modules.board.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/board/notifications")
@RequiredArgsConstructor
public class BoardNotificationController {
    private final BoardNotificationService notices;
    private final BoardSessionService sessions;
    @GetMapping
    public BoardPage<BoardNotificationService.NoticeView> page(@RequestHeader(value="Authorization", required=false) String token,
            @RequestParam(required=false) NotificationCategory category, @RequestParam(defaultValue="0") int page) {
        return notices.page(sessions.requireMember(token).getId(), category, page);
    }
    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestHeader(value="Authorization", required=false) String token) {
        return notices.summary(sessions.requireMember(token).getId());
    }
    @PutMapping("/read")
    public Map<String, Boolean> read(@RequestHeader(value="Authorization", required=false) String token, @RequestBody List<Long> ids) {
        notices.markRead(sessions.requireMember(token).getId(), ids);
        return Map.of("success", true);
    }
}
