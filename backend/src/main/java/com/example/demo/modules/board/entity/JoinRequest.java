package com.example.demo.modules.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "join_requests", uniqueConstraints = @UniqueConstraint(columnNames = { "post_id", "member_id" }))
@Data
@NoArgsConstructor
public class JoinRequest {
	@Id
	@GeneratedValue(generator = "board_join_requests")
	@org.hibernate.annotations.GenericGenerator(name = "board_join_requests",
			type = com.example.demo.modules.board.config.BoardSequenceGenerator.class,
			parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "join_requests_seq"))
	private Long id;
	@Version
	@Column(nullable = false, columnDefinition = "bigint default 0")
	private long version;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "post_id")
	private TeamPost post;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "member_id")
	private Member applicant;
	private String message;
	@Enumerated(EnumType.STRING)
	private ApplicationStatus status = ApplicationStatus.PENDING;
	private LocalDateTime createdAt;

	@PrePersist
	void create() {
		createdAt = LocalDateTime.now();
	}
}
