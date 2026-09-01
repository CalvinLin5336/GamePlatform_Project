package com.example.demo.modules.user.controller;

import com.example.demo.modules.user.dto.OperationLogResponse;
import com.example.demo.modules.user.service.OperationLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/admin/operation-logs")
public class OperationLogController {
	private final OperationLogService service;

	public OperationLogController(OperationLogService service) {
		this.service = service;
	}

	@GetMapping
	public List<OperationLogResponse> findAll() {
		return service.findAll();
	}
}
