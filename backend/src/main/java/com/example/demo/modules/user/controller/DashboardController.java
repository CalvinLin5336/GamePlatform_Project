package com.example.demo.modules.user.controller;

import com.example.demo.modules.user.dto.DashboardResponse;
import com.example.demo.modules.user.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/board/admin/dashboard")
public class DashboardController {
	private final DashboardService service;

	public DashboardController(DashboardService service) {
		this.service = service;
	}

	@GetMapping
	public DashboardResponse summary() {
		return service.summary();
	}
}
