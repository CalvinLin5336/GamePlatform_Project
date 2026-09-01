package com.example.demo.modules.game.quiz.model;


import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="options")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Option {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Lob
	@Column(columnDefinition = "LONGTEXT", nullable = false)
	private String optionText;
	
	@Column(nullable = false)
	private Boolean isCorrect;
	
	@ManyToOne(fetch= FetchType.LAZY)
	@JoinColumn(name="question_id")
	@JsonIgnore
	private Question question;
}
