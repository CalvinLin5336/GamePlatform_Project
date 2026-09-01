package com.example.demo.modules.game.quiz.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="players")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Player {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, unique = true, length = 200)
	private String username;
	
	private Integer highScore=0;
}

