package com.example.demo.modules.board.controller;

import com.example.demo.modules.board.entity.TeamPost;
import com.example.demo.modules.board.service.BoardTeamService;
import com.example.demo.modules.game.poker.dto.JoinResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/board/team-posts")
@RequiredArgsConstructor
public class BoardTeamController {
    private final BoardTeamService teams;

    @PostMapping("/{id}/kick")
    public TeamPost kick(@PathVariable Long id, @RequestParam Long captainId, @RequestBody Map<String, String> request) {
        return teams.kick(id, captainId, request.get("account"));
    }

    @PostMapping("/{id}/start")
    public Map<String, String> start(@PathVariable Long id, @RequestParam Long captainId) {
        return teams.start(id, captainId);
    }

    @GetMapping("/{id}/game")
    public Map<String, String> game(@PathVariable Long id, @RequestParam Long memberId) {
        return teams.access(id, memberId);
    }

    @PostMapping("/{id}/game/join")
    public JoinResult join(@PathVariable Long id, @RequestParam Long memberId) {
        return teams.joinGame(id, memberId);
    }
}
