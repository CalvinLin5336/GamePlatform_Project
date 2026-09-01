package com.example.demo.modules.board.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.modules.board.dto.*;
import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.service.InteractionService;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InteractionController {
	private final InteractionService service;

	@PostMapping("/team-posts/{id}/join")
	ResponseEntity<JoinRequest> join(@PathVariable Long id, @RequestBody JoinRequestForm f) {
		return ResponseEntity.ok(service.join(id, f));
	}

	@GetMapping("/applications/member/{id}")
	ResponseEntity<List<JoinRequest>> my(@PathVariable Long id) {
		return ResponseEntity.ok(service.myApplications(id));
	}

	@GetMapping("/applications/captain/{id}")
	ResponseEntity<List<JoinRequest>> received(@PathVariable Long id) {
		return ResponseEntity.ok(service.captainRequests(id));
	}

	@PutMapping("/applications/{id}/{status}")
	ResponseEntity<JoinRequest> review(@PathVariable Long id, @PathVariable ApplicationStatus status) {
		return ResponseEntity.ok(service.review(id, status));
	}

	@GetMapping("/team-posts/{id}/comments")
	ResponseEntity<List<Comment>> comments(@PathVariable Long id) {
		return ResponseEntity.ok(service.comments(id));
	}

	@PostMapping("/team-posts/{id}/comments")
	ResponseEntity<Comment> comment(@PathVariable Long id, @Valid @RequestBody CommentRequest f) {
		return ResponseEntity.ok(service.addComment(id, f));
	}

	@DeleteMapping("/comments/{id}")
	ResponseEntity<Void> deleteComment(@PathVariable Long id) {
		service.deleteComment(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/team-posts/{postId}/favorite/{memberId}")
	ResponseEntity<Map<String, Boolean>> favorite(@PathVariable Long postId, @PathVariable Long memberId) {
		return ResponseEntity.ok(Map.of("favorite", service.toggleFavorite(postId, memberId)));
	}

	@GetMapping("/favorites/member/{id}")
	ResponseEntity<List<Favorite>> favorites(@PathVariable Long id) {
		return ResponseEntity.ok(service.favoriteList(id));
	}

	@GetMapping("/notifications/member/{id}")
	ResponseEntity<List<Notification>> notices(@PathVariable Long id) {
		return ResponseEntity.ok(service.notifications(id));
	}
}
