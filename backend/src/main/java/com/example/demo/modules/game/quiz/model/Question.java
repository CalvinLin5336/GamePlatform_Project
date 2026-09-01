package com.example.demo.modules.game.quiz.model;


import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Question {
	
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;
	
	//columnDefinition = "TEXAT" 支援 > 1000 字長文與HTML排版
	@Lob
	@Column(columnDefinition = "LONGTEXT", nullable = false)
	private String title;
	
	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String explanation; //解答說明/試題解析
	
	private Integer timeLimitSeconds = 30; //預設是30秒
	
	@OneToMany(mappedBy= "question",cascade=CascadeType.ALL, orphanRemoval = true)
	private List<Option> options = new ArrayList<>();
	//輔助方法:處理雙向關聯
	public void addOption(Option option) {
		options.add(option);
		option.setQuestion(this);
	}
}
