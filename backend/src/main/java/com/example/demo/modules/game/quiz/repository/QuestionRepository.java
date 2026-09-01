package com.example.demo.modules.game.quiz.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.modules.game.quiz.model.Question;

/*
 * 題目資料存取介面
 * 繼承 JpaRepository<Question,Long> 即具備CRUP與分頁功能(主鍵型態為Long)
 * */

public interface QuestionRepository extends JpaRepository<Question,Long> {
	/*
	 * 隨機抽指定數量的題目(出題功能)
	 * 
	 * @Query 說明:
	 * -value:原生SQL語法，利用資料庫RAND() 函數將所有題目隨機排序，並限制取出筆數
	 * -nativeQuery = true: 聲明此為原生SQL(如 MySQL/MariaDB)，而非JPQL
	 * 
	 * @Param("limit")說明:
	 * -將方法參數 limit 綁定到SQL中的命名參數: limit。
	 * 
	 * @param limit 要抽取的題目數量(例如:10題)
	 * @return 隨機挑選出的 Question 實體清單
	 * */	
	//使用 MySQL 原生 SQL 隨機抽取指定數量的題目(預設20題)	
	@Query(value = "SELECT * FROM questions ORDER BY RAND() LIMIT :count", nativeQuery=true)
	List<Question> findRandomQuestions(@Param("count") int count);
}
