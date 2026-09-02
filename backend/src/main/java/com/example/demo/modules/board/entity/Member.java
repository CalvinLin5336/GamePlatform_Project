package com.example.demo.modules.board.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
public class Member {
	@Id
	@GeneratedValue(generator = "board_members")
	@org.hibernate.annotations.GenericGenerator(name = "board_members",
			type = com.example.demo.modules.board.config.BoardSequenceGenerator.class,
			parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "members_seq"))
	private Long id;
	@Column(unique = true)
	private Long platformUserId;
	@Column(nullable = false, unique = true, length = 50)
	private String account;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@Column(nullable = false)
	private String password;
	@Column(nullable = false, length = 100)
	private String nickname;
	@Column(nullable = false, unique = true, length = 100)
	private String email;
	private Integer level = 1;
	private String role = "PLAYER";
}
