package com.example.demo.modules.board.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.modules.board.dto.TeamPostRequest;
import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.service.TeamPostService;

import java.util.List;

@RestController
@RequestMapping("/board/team-posts")
@RequiredArgsConstructor
public class TeamPostController {
	private final TeamPostService service;

	@GetMapping
	public ResponseEntity<List<TeamPost>> all(@RequestParam(defaultValue = "") String keyword,
			@RequestParam(required = false) PostStatus status,
			@RequestParam(required = false) Long gameId, @RequestParam(required = false) Long modeId) {
		return ResponseEntity.ok(service.list(keyword, status, gameId, modeId));
	}

	@GetMapping("/{id}")
	public ResponseEntity<TeamPost> one(@PathVariable Long id) {
		return ResponseEntity.ok(service.get(id));
	}

	@GetMapping("/{id}/room")
	public ResponseEntity<java.util.Map<String, String>> room(@PathVariable Long id, @RequestParam Long memberId) {
		return ResponseEntity.ok(java.util.Map.of("roomId", service.roomId(id, memberId)));
	}

	@GetMapping("/captain/{memberId}")
	public ResponseEntity<List<TeamPost>> mine(@PathVariable Long memberId) {
		return ResponseEntity.ok(service.mine(memberId));
	}

	@PostMapping
	public ResponseEntity<TeamPost> create(@Valid @RequestBody TeamPostRequest f) {
		return ResponseEntity.ok(service.create(f));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TeamPost> update(@PathVariable Long id, @Valid @RequestBody TeamPostRequest f) {
		return ResponseEntity.ok(service.update(id, f));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
