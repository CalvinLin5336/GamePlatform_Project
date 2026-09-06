package com.example.demo.modules.game.turing.repository;

import com.example.demo.modules.game.turing.model.TuringQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TuringQuestionRepository extends JpaRepository<TuringQuestion, Integer> {

    /**
     * 🟢 針對 SQLite 修正：將表格名稱改為正確的 turing_questions
     */
    @Query(value = "SELECT * FROM turing_questions WHERE k_count = :kCount ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<TuringQuestion> findRandomQuestionByKCount(@Param("kCount") int kCount);

    /**
     * 🟢 針對 SQLite 修正：將表格名稱改為正確的 turing_questions
     */
    @Query(value = "SELECT * FROM turing_questions ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<TuringQuestion> findRandomQuestion();
}