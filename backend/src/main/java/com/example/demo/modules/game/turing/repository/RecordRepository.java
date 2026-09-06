package com.example.demo.modules.game.turing.repository;

import com.example.demo.modules.game.turing.model.GameRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<GameRecord, Integer> {
    
    // 取代 findAll() 中的 ORDER BY played_at DESC[cite: 12]
    List<GameRecord> findAllByOrderByPlayedAtDesc();
}