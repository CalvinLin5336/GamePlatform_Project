package com.example.demo.modules.board.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
	ResponseEntity<Map<String, String>> status(org.springframework.web.server.ResponseStatusException e) {
		return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "請求失敗" : e.getReason()));
	}

	@ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, String>> validation(org.springframework.web.bind.MethodArgumentNotValidException e) {
		return ResponseEntity.badRequest().body(Map.of("message", "請填寫完整公告內容，並選擇遊戲與模式"));
	}

	@ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
	ResponseEntity<Map<String, String>> conflict(org.springframework.dao.ConcurrencyFailureException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "隊伍資料已更新，請重新整理後再操作"));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<Map<String, String>> handle(IllegalArgumentException e) {
		return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Map<String, String>> unexpected(Exception e) {
		return ResponseEntity.internalServerError()
				.body(Map.of("message", e.getMessage() == null ? "系統錯誤" : e.getMessage()));
	}
}
