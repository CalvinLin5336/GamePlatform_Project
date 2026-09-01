package com.example.demo.modules.game.quiz.dto;


import java.util.List;
import java.util.Set;

import com.example.demo.modules.game.quiz.model.Question;

import lombok.Builder;
import lombok.Data;
/*
 * 後端批改完成後回傳的Response DTO (測驗結果報告)
 * */
@Data
@Builder
public class QuizReportResponse {
	//總題數
	private int totalQuestions;
	//答對的題目數量
	private int correctCount;
	//最終得分(例如:80分)
	private int score;
	//每題的作答明細清單(供使用者檢驗題目)
	private List<QuestionDetail> details;		
	
	/*
	 * 單題批改結果明細
	 * */
	@Data
	@Builder
	public static class QuestionDetail{
		//題目本體實體(包含題目內容、選項、正確答案與解析等資訊
		private Question question;
		//使用者當時選擇的ID集合(用於與正確答案比對高量顯示)
		private Set<Long> userSelectedOptionIds;
		//此題是否答對(true:答對，false:答錯)
		private boolean isCorrect;
	}
}
