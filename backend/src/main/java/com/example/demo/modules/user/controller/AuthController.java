package com.example.demo.modules.user.controller;

import com.example.demo.modules.user.dto.LoginRequest;
import com.example.demo.modules.user.dto.LoginResponse;
import com.example.demo.modules.user.dto.RegisterRequest;
import com.example.demo.modules.user.dto.UserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.service.UserService;
import com.example.demo.modules.user.service.LoginSessionService;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(originPatterns = "*")
@RequestMapping("/api/user/auth")
public class AuthController {
	private final UserService userService;
	private final LoginSessionService sessions;

	public AuthController(UserService userService, LoginSessionService sessions) {
		this.userService = userService;
		this.sessions = sessions;
	}

	@GetMapping("/me")
	public UserResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
		return sessions.requireUser(authorization);
	}

	@PostMapping("/login")
	public LoginResponse login(@RequestBody LoginRequest request) {
		return userService.login(request);
	}

	@PostMapping("/register")
	public UserResponse register(@RequestBody RegisterRequest request) {
		String avatar = request.avatar();
		if (avatar == null || avatar.isBlank()) {
			avatar = "/pages/User/Player/avatar/user.png";
		}

		UserRequest userRequest = new UserRequest(request.account(), request.password(), request.username(),
				avatar, request.description(), "PLAYER", "Active");
		return userService.create(userRequest, "SYSTEM");
	}
}
