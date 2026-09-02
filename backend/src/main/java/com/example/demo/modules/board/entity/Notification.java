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
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "member_id")
	private Member member;
	private String title;
	private String message;
	private Boolean readFlag = false;
	private LocalDateTime createdAt;

	@PrePersist
	void create() {
		createdAt = LocalDateTime.now();
	}
}
