package com.example.demo.modules.game.quiz.service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.example.demo.modules.game.quiz.model.*;

import com.example.demo.modules.game.quiz.repository.PlayerRepository;
import com.example.demo.modules.game.quiz.repository.QuestionRepository;

import jakarta.transaction.Transactional;

@Service
public class PlayerService {
	@Autowired
	PlayerRepository playerepo;
	
	//---排行管理---
	public Player updatePlayerScore(String username,int newScore) {
		Player player = playerepo.findByUsername(username)
				.orElseGet(()->{
					Player p = new Player();
					p.setUsername(username);
					p.setHighScore(0);
					return p;
				});
			//僅當新分數高於歷史最高分時更新
			if(newScore > player.getHighScore()) {
				player.setHighScore(newScore);
			}
			return playerepo.save(player);
	}
	
	public List<Player>getLeaderboard(){
		return playerepo.findTop10ByOrderByHighScoreDesc();
	}
}
