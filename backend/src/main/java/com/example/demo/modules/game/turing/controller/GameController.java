package com.example.demo.modules.game.turing.controller;

import com.example.demo.modules.game.turing.dto.VerifyCardRequest; // 修正大小寫 dto
import com.example.demo.modules.game.turing.dto.FinalGuessRequest; // 💡 補上這個 DTO
import com.example.demo.modules.game.turing.model.*;
import com.example.demo.modules.game.turing.repository.TuringQuestionRepository;
import com.example.demo.modules.game.turing.service.impl.GameService; // 修正 import 位置

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
public class GameController {

	@Autowired
    private TuringQuestionRepository turingQuestionRepository; // 確保有注入對應的 Repository
	
	
    @Autowired
    private GameService gameService;

    @GetMapping("/new")
    public ResponseEntity<Puzzle> startNewGame(@RequestParam String difficulty) {
        Puzzle puzzle = gameService.createNewPuzzle(difficulty);
        return ResponseEntity.ok(puzzle);
    }

    @PostMapping("/verify")
    public ResponseEntity<Boolean> testVerifierCard(@RequestBody VerifyCardRequest request) {
        boolean result = gameService.verifyProposal(request.getProposal(), request.getCardId(), request.getSecret());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/submit")
    public ResponseEntity<GameResultDto> submitFinalGuess(@RequestBody FinalGuessRequest request) {
        // 1. 根據前端傳來的 puzzleId 從資料庫安全地撈出該題的正解
        TuringQuestion question = turingQuestionRepository.findById(request.getPuzzleId())
                .orElseThrow(() -> new RuntimeException("找不到對應的題目資料"));

        // 2. 建立玩家的猜測密碼與資料庫正解進行比對
        Code proposal = new Code(request.getB(), request.getY(), request.getP());
        Code secret = new Code(question.getAnsB(), question.getAnsY(), question.getAnsP());
        
        boolean isCorrect = gameService.checkWin(proposal, secret);

        int rewardTokens = isCorrect ? 10 : 0; // 答對發放代幣，答錯為 0
        
        GameResultDto result = new GameResultDto(
                isCorrect,
                isCorrect ? "🎉 恭喜你成功破譯密碼！" : "💥 密碼錯誤，破譯失敗！",
                rewardTokens
        );

        // 3. 紀錄戰績
        GameRecord record = new GameRecord(
                request.getPlayerName(),
                String.valueOf(request.getPuzzleId()),
                request.getCurrentRound(),
                request.getTotalTests(),
                secret.toString(),
                isCorrect,
                rewardTokens
        );

        gameService.saveRecord(record);
        
        return ResponseEntity.ok(result);
    }
}