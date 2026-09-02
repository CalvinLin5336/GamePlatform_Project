package com.example.demo.modules.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_posts")
@Data
@NoArgsConstructor
public class TeamPost {
	@Id
	@GeneratedValue(generator = "board_team_posts")
	@org.hibernate.annotations.GenericGenerator(name = "board_team_posts",
			type = com.example.demo.modules.board.config.BoardSequenceGenerator.class,
			parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "team_posts_seq"))
	private Long id;
	@Version
	@Column(nullable = false, columnDefinition = "bigint default 0")
	private long version;
	@Column(nullable = false, length = 100)
	private String title;
	@Column(nullable = false, length = 80)
	private String gameName;
	private Long gameId;
	private Long modeId;
	private String modeCode;
	private String modeName;
	private Integer minPlayers;
	private Integer modeMaxPlayers;
	private Integer computerPlayers;
	private String roomId;
	private String activityType;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Integer maxPlayers;
	private Integer currentPlayers = 1;
	private Boolean voiceRequired = false;
	private String rankRequirement;
	@Lob
	private String description;
	private String tags;
	@Enumerated(EnumType.STRING)
	private PostStatus status = PostStatus.RECRUITING;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "member_id")
	private Member captain;
	private LocalDateTime createdAt;

	@PrePersist
	void create() {
		createdAt = LocalDateTime.now();
	}
}
