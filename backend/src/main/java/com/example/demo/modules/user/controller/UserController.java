package com.example.demo.modules.user.controller;

import com.example.demo.modules.user.dto.UserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/board/admin/users")
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
	public UserResponse create(@RequestBody UserRequest request) {
		return userService.create(request, "admin");
	}

	@PutMapping("/{id}")
	public UserResponse update(@PathVariable Long id, @RequestBody UserRequest request) {
		return userService.update(id, request, "admin");
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		userService.delete(id, "admin");
	}
}
