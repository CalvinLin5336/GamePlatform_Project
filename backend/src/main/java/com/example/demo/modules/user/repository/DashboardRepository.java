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
        return count("""
                SELECT COUNT(*)
                FROM users u
                JOIN statuses s ON s.id = u.status_id
                WHERE LOWER(s.status_name) = 'active'
                """);
    }

    public long countDisabledUsers() {
        return count("""
                SELECT COUNT(*)
                FROM users u
                JOIN statuses s ON s.id = u.status_id
                WHERE LOWER(s.status_name) = 'disabled'
                """);
    }

    public long countAdmins() {
        return count("""
                SELECT COUNT(*)
                FROM users u
                JOIN roles r ON r.id = u.role_id
                WHERE UPPER(r.role_name) = 'ADMIN'
                """);
    }

    public long countTodayOperations() {
        return count("""
                SELECT COUNT(*) FROM operation_logs
                WHERE date(created_at) = date('now','localtime')
                """);
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }
}
