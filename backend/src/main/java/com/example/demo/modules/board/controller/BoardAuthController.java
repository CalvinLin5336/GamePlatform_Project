package com.example.demo.modules.board.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.modules.board.dto.*;
import com.example.demo.modules.board.entity.Member;
import com.example.demo.modules.board.service.AuthService;
import com.example.demo.modules.board.service.BoardSessionService;

@RestController
@RequestMapping("/board/auth")
@RequiredArgsConstructor
public class BoardAuthController {
	private final AuthService service;
	private final BoardSessionService sessions;

	@PostMapping("/session")
	public ResponseEntity<Member> session(@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ResponseEntity.ok(sessions.currentMember(authorization));
	}

	@PostMapping("/login")
	public ResponseEntity<Member> login(@Valid @RequestBody LoginRequest f) {
		return ResponseEntity.ok(service.login(f));
	}

	@PostMapping("/register")
	public ResponseEntity<Member> register(@Valid @RequestBody RegisterRequest f) {
		return ResponseEntity.ok(service.register(f));
	}
}
