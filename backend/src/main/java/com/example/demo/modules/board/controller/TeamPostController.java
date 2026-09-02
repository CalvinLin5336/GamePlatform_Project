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
			@RequestParam(required = false) PostStatus status) {
		List<TeamPost> data = service.list(keyword, status);
		return data.size() > 0 ? ResponseEntity.ok(data) : ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	public ResponseEntity<TeamPost> one(@PathVariable Long id) {
		return ResponseEntity.ok(service.get(id));
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
