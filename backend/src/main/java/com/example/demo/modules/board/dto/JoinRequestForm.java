package com.example.demo.modules.board.dto;

import lombok.Data;

@Data
public class JoinRequestForm {
	private Long memberId;
	private String message;
}
