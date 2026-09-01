package com.example.demo.modules.game.quiz.repository;

import com.example.demo.modules.game.quiz.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
/*
 * 玩家資料存取介面
 * 繼承 JpaRepository<Player, Long> (實體為Player，主鍵型態為Long)
 * */
public interface PlayerRepository  extends JpaRepository<Player,Long>{
	/*
	 * 依據使用者名稱精準查詢玩家
	 * 
	 * 解析:
	 * -findBy + Username : Spring Data JPA 會自動解析為 SQL:
	 *  "SELECT * FROM player WHERE username=?"
	 * */
	Optional<Player> findByUsername(String username);
	
	//取得前N名排行榜(依最高分倒序排序)
	List<Player> findTop10ByOrderByHighScoreDesc();
}
