package com.example.demo.modules.game.turing.repository;

import com.example.demo.modules.game.turing.model.GameRecord; // 依照你的實體類別調整
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<GameRecord, Integer> {

    // 取得最新報表日期 (SQLite 支援 MAX 語法)
    @Query(value = "SELECT MAX(summary_date) FROM daily_platform_summary", nativeQuery = true)
    Date getLatestReportDate();

    // 🟢 核心修正 1：MySQL 的 ON DUPLICATE KEY 轉換為 SQLite 的 INSERT OR IGNORE
    @Modifying
    @Transactional
    @Query(value = "INSERT OR IGNORE INTO daily_platform_summary " +
                   "(summary_date, new_users_count, active_users_count, total_games_played, tokens_minted_game, " +
                   " easy_games, easy_wins, std_games, std_wins, hard_games, hard_wins, " +
                   " easy_reward_wins, std_reward_wins, hard_reward_wins) " +
                   "VALUES (:targetDate, " +
                   "  (SELECT COUNT(*) FROM users WHERE DATE(created_at) = :targetDate), " + 
                   "  (SELECT COUNT(DISTINCT player_name) FROM game_records WHERE DATE(played_at) = :targetDate), " + 
                   "  (SELECT COUNT(*) FROM game_records WHERE DATE(played_at) = :targetDate), " + 
                   "  (SELECT IFNULL(SUM(tokens_rewarded), 0) FROM game_records WHERE DATE(played_at) = :targetDate), " +
                   "  (SELECT COUNT(*) FROM game_records WHERE DATE(played_at) = :targetDate AND puzzle_id LIKE '#A%'), " +
                   "  (SELECT COUNT(*) FROM game_records WHERE DATE(played_at) = :targetDate AND puzzle_id LIKE '#A%' AND is_won = 1), " +
                   "  (SELECT COUNT(*) FROM game_records WHERE DATE(played_at) = :targetDate AND puzzle_id LIKE '#B%'), " +
                   "  (SELECT COUNT(*) FROM game_records WHERE DATE(played_at) = :targetDate AND puzzle_id LIKE '#B%' AND is_won = 1), " +
                   "  (SELECT COUNT(*) FROM game_records WHERE DATE(played_at) = :targetDate AND puzzle_id LIKE '#C%'), " +
                   "  (SELECT COUNT(*) FROM game_records WHERE DATE(played_at) = :targetDate AND puzzle_id LIKE '#C%' AND is_won = 1), " +
                   "  (SELECT COUNT(*) FROM game_records WHERE DATE(played_at) = :targetDate AND puzzle_id LIKE '#A%' AND is_won = 1 AND tokens_rewarded > 0), " + 
                   "  (SELECT COUNT(*) FROM game_records WHERE DATE(played_at) = :targetDate AND puzzle_id LIKE '#B%' AND is_won = 1 AND tokens_rewarded > 0), " + 
                   "  (SELECT COUNT(*) FROM game_records WHERE DATE(played_at) = :targetDate AND puzzle_id LIKE '#C%' AND is_won = 1 AND tokens_rewarded > 0) " +  
                   ")", 
           nativeQuery = true)
    void generateDailySummary(@Param("targetDate") Date targetDate);

    // 取得日報表列表
    @Query(value = "SELECT * FROM daily_platform_summary ORDER BY summary_date DESC LIMIT 365", nativeQuery = true)
    List<Object[]> getDailyReports();

    // 🟢 核心修正 2：MySQL 的 DATE_FORMAT 轉換為 SQLite 的 STRFTIME
    @Query(value = "SELECT STRFTIME('%Y-%m', summary_date) AS month, " +
                   "SUM(new_users_count), SUM(active_users_count), SUM(total_games_played), SUM(tokens_minted_game), " +
                   "SUM(easy_games), SUM(easy_wins), SUM(std_games), SUM(std_wins), SUM(hard_games), SUM(hard_wins), " +
                   "SUM(easy_reward_wins), SUM(std_reward_wins), SUM(hard_reward_wins) " +
                   "FROM daily_platform_summary GROUP BY month ORDER BY month DESC", 
           nativeQuery = true)
    List<Object[]> getMonthlyReports();
}