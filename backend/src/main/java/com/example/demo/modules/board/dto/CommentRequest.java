package com.example.demo.modules.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {
	private Long memberId;
	@NotBlank
	@jakarta.validation.constraints.Size(max = 1000)
	private String content;
}
