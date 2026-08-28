package com.example.demo.modules.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countUsers() {
        return count("SELECT COUNT(*) FROM users");
    }

    public long countActiveUsers() {
        ensureProfileTable();
        return count("""
                SELECT COUNT(*)
                FROM users u
                LEFT JOIN user_profile p ON p.user_id = u.id
                WHERE COALESCE(p.status, 'Active') = 'Active'
                """);
    }

    public long countDisabledUsers() {
        ensureProfileTable();
        return count("""
                SELECT COUNT(*)
                FROM users u
                JOIN user_profile p ON p.user_id = u.id
                WHERE p.status = 'Disabled'
                """);
    }

    public long countAdmins() {
        return count("""
                SELECT COUNT(*)
                FROM users
                WHERE UPPER(role) = 'ADMIN'
                """);
    }

    public long countTodayOperations() {
        ensureOperationLogTable();
        return count("""
                SELECT COUNT(*) FROM operation_logs
                WHERE date(created_at) = date('now','localtime')
                """);
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }

    private void ensureProfileTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_profile (
                    user_id INTEGER PRIMARY KEY,
                    description VARCHAR(500),
                    status VARCHAR(20) NOT NULL DEFAULT 'Active',
                    last_login VARCHAR(19),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
    }

    private void ensureOperationLogTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS operation_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account VARCHAR(50) NOT NULL,
                    action VARCHAR(30) NOT NULL,
                    target_id INTEGER,
                    role VARCHAR(20),
                    description VARCHAR(500),
                    created_at VARCHAR(19) NOT NULL
                )
                """);
    }
}
