package com.example.demo.modules.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
public class Comment {
	@Id
	@GeneratedValue(generator = "board_comments")
	@org.hibernate.annotations.GenericGenerator(name = "board_comments",
			type = com.example.demo.modules.board.config.BoardSequenceGenerator.class,
			parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "comments_seq"))
	private Long id;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "post_id")
	private TeamPost post;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "member_id")
	private Member member;
	@Column(nullable = false, length = 1000)
	private String content;
	private LocalDateTime createdAt;

	@PrePersist
	void create() {
		createdAt = LocalDateTime.now();
	}
}
