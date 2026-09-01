package com.example.demo.modules.game.management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.modules.game.management.model.Game;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByEnabledTrueOrderByGameIdAsc();
    Optional<Game> findByGameCode(String gameCode);
}
