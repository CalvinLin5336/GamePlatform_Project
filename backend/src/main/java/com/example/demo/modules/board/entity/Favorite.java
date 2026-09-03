package com.example.demo.modules.board.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(columnNames = { "post_id", "member_id" }))
@Data
@NoArgsConstructor
public class Favorite {
	@Id
	@GeneratedValue(generator = "board_favorites")
	@org.hibernate.annotations.GenericGenerator(name = "board_favorites",
			type = com.example.demo.modules.board.config.BoardSequenceGenerator.class,
			parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "favorites_seq"))
	private Long id;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "post_id")
	private TeamPost post;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "member_id")
	private Member member;
}
