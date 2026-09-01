package com.example.demo.modules.user.controller;

import com.example.demo.modules.user.dto.UserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/user/admin/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public List<UserResponse> findAll() {
		return userService.findAll();
	}

	@GetMapping("/{id}")
	public UserResponse findById(@PathVariable Long id) {
		return userService.findById(id);
	}

	@PostMapping
	public UserResponse create(
			@RequestBody UserRequest request,
			Authentication authentication) {
		return userService.create(request, authentication.getName());
	}

	@PutMapping("/{id}")
	public UserResponse update(
			@PathVariable Long id,
			@RequestBody UserRequest request,
			Authentication authentication) {
		return userService.update(id, request, authentication.getName());
	}

	@DeleteMapping("/{id}")
	public void delete(
			@PathVariable Long id,
			Authentication authentication) {
		userService.delete(id, authentication.getName());
	}
}
