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
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
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
