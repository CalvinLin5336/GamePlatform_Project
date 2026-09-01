package com.example.demo.modules.game.quiz.controller;


import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.modules.game.quiz.dto.QuizReportResponse;
import com.example.demo.modules.game.quiz.dto.QuizSubmitRequest;
import com.example.demo.modules.game.quiz.model.Player;
import com.example.demo.modules.game.quiz.model.Question;
import com.example.demo.modules.game.quiz.service.QuizService;

@RestController
@RequestMapping("/api/quiz/questions")
@CrossOrigin(origins="*")  //允許所有來源進行 API 呼叫
public class QuestionsController {
	@Autowired
	QuizService quizsrv;
	
	
	// 隨機抽取 20題作為一卷考題
	@GetMapping("/exam")
	public ResponseEntity<List<Question>> getExamPaper(){
		return ResponseEntity.ok(quizsrv.getRandomExamQuestions(20));
	}
	
	// 取得所有題目
	@GetMapping
	public ResponseEntity<List<Question>>getAllQuestions(){
		return ResponseEntity.ok(quizsrv.getAllQuestions());
	}
	
	// 取得單一題目
	@GetMapping("/{id}")
	public ResponseEntity<Question> getQuestionById(@PathVariable Long id){
		return ResponseEntity.ok(quizsrv.getQuestionById(id));
	}
	
	// 新增題目
	@PostMapping
	public ResponseEntity<Question> createQuestion(@RequestBody Question question){
		return ResponseEntity.ok(quizsrv.createQuestion(question));
	}
	// 修改題目
	@PutMapping("/{id}")
	public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question question){
		return ResponseEntity.ok(quizsrv.updateQuestion(id, question));
	}
	
	//刪除題目
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteQuestion(@PathVariable Long id){
		quizsrv.deleteQuestion(id);
		return ResponseEntity.noContent().build();
	}
	
	// 提交整卷測驗，計算分數並回傳成績單與解答說明
	@PostMapping("/submit")
	public ResponseEntity<QuizReportResponse> submitQuiz(@RequestBody QuizSubmitRequest request){
		return ResponseEntity.ok(quizsrv.processQuizSubmission(request));
	}


}
