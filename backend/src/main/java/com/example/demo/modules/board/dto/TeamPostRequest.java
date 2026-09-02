package com.example.demo.modules.board.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeamPostRequest {
	@NotBlank
	@Size(max = 100)
	private String title;
	@NotNull
	@Positive
	private Long gameId;
	@NotNull
	@Positive
	private Long modeId;
	@Min(1)
	private Integer playerCount;
	private String activityType;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Boolean voiceRequired;
	private String rankRequirement;
	@NotBlank
	private String description;
	private String tags;
	@NotNull
	@Positive
	private Long captainId;
}
