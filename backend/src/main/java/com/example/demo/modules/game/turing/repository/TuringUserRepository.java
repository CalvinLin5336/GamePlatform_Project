package com.example.demo.modules.game.turing.repository;

import com.example.demo.modules.game.turing.model.TuringUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TuringUserRepository extends JpaRepository<TuringUser, Integer> {

    Optional<TuringUser> findByUsername(String username);

    // 🟢 確保這裡的 UPDATE 對象是 TuringUser
    @Modifying
    @Transactional
    @Query("UPDATE TuringUser u SET u.tokens = :tokens WHERE u.id = :id")
    void updateTokens(int id, int tokens);

    // 🟢 確保這裡的 UPDATE 對象是 TuringUser
    @Modifying
    @Transactional
    @Query("UPDATE TuringUser u SET u.blocked = :blocked WHERE u.id = :id")
    void updateBlockedStatus(int id, boolean blocked);

    // 這裡因為是靠方法名稱自動生成 SQL，所以不需要動
    List<TuringUser> findTop10ByBlockedFalseOrderByTokensDesc();

    // 🟢 核心修正：確保 SELECT 對象是 TuringUser
    @Query("SELECT u FROM TuringUser u WHERE u.role != 'ADMIN' ORDER BY u.tokens DESC")
    List<TuringUser> findTopRichUsers();
}