package com.example.demo.modules.game.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.modules.game.system.model.GameMode;

public interface GameModeRepository extends JpaRepository<GameMode, Long> {
    List<GameMode> findByGameIdAndEnabledTrueOrderByModeIdAsc(Long gameId);
    List<GameMode> findByGameIdOrderByModeIdAsc(Long gameId);
    Optional<GameMode> findByGameIdAndModeCode(Long gameId, String modeCode);
    void deleteByGameId(Long gameId);
}
