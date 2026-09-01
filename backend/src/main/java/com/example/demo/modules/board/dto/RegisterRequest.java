package com.example.demo.modules.board.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
	@NotBlank
	private String account;
	@NotBlank
	@Size(min = 4)
	private String password;
	@NotBlank
	private String nickname;
	@Email
	@NotBlank
	private String email;
}
