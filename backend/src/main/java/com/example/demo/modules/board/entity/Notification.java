package com.example.demo.modules.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {
	@Id
	@GeneratedValue(generator = "board_notifications")
	@org.hibernate.annotations.GenericGenerator(name = "board_notifications",
			type = com.example.demo.modules.board.config.BoardSequenceGenerator.class,
			parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "notifications_seq"))
	private Long id;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "member_id")
	private Member member;
	private String title;
	@Column(length = 2000)
	private String message;
	private Long postId;
	private Long applicationId;
	private Long commentId;
	private String category;
	private Boolean readFlag = false;
	private LocalDateTime createdAt;

	@PrePersist
	void create() {
		createdAt = LocalDateTime.now();
	}
}
