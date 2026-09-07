package com.example.demo.modules.game.quiz.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.modules.game.quiz.model.Player;
import com.example.demo.modules.game.quiz.service.PlayerService;

@RestController
@RequestMapping("/api/quiz/players")
public class PlayerController {
	@Autowired
	PlayerService playersrv;	

	
	// 取得排行榜(Top 10)
	@GetMapping("/leaderboard")
	public ResponseEntity<List<Player>> getLeaderboard(){
		return ResponseEntity.ok(playersrv.getLeaderboard());
	}
	
}
