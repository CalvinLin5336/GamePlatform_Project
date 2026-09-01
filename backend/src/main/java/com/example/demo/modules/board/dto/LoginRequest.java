package com.example.demo.modules.board.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {
	@NotBlank
	private String account;
	@NotBlank
	private String password;
}
