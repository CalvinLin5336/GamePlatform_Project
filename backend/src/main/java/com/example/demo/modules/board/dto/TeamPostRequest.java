package com.example.demo.modules.board.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.example.demo.modules.board.entity.PostStatus;

@Data
public class TeamPostRequest {
	@NotBlank
	private String title;
	@NotBlank
	private String gameName;
	private String activityType;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	@NotNull
	@Min(2)
	private Integer maxPlayers;
	private Boolean voiceRequired;
	private String rankRequirement;
	@NotBlank
	private String description;
	private String tags;
	private PostStatus status;
	private Long captainId;
}
