package com.example.demo.modules.game.turing.repository;

import com.example.demo.modules.game.turing.model.GameConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameConfigRepository extends JpaRepository<GameConfig, Character> {

    // 取代原本的 getAllConfigs()，並且依照難度字元排序[cite: 14]
    List<GameConfig> findAllByOrderByDifficultyCharAsc();
    
    // getConfigValue() 的邏輯，現在你只要呼叫 findById(diffChar).get().getRewardTokens() 即可。
}