package com.example.demo.modules.game.quiz.dto;


import java.util.List;
import java.util.Set;

import lombok.Data;
/*
 * 前端提交測驗作答結果的 Request DTO
 * */
@Data
public class QuizSubmitRequest {
	
	//作答者名稱
	private String username;
	
	//作答清單
	private List<AnswerItem> answers;
	/*
	 * 單一題目的作答項目
	 * */	
	@Data
	public static class AnswerItem{
		//題目 ID
		private Long questionId;
		//使用者勾選的選項 ID 集合
		//使用Set<Long>可同時支援【單選題(1個ID)】與【多選題(多個ID)】
		private Set<Long> selectedOptionIds;
	}
}
