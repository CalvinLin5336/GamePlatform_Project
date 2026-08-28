package com.example.demo.modules.user.repository;

import com.example.demo.modules.user.dto.OperationLogResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OperationLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public OperationLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String account, String action, Long targetId, String role,
                     String description, String createdAt) {
        ensureTable();

        jdbcTemplate.update("""
                INSERT INTO operation_logs(account,action,target_id,role,description,created_at)
                VALUES(?,?,?,?,?,?)
                """, account, action, targetId, role, description, createdAt);
    }

    public List<OperationLogResponse> findAll() {
        ensureTable();

        return jdbcTemplate.query("""
                SELECT id,account,action,target_id,role,description,created_at
                FROM operation_logs
                ORDER BY id DESC
                """, (rs, rowNum) -> new OperationLogResponse(
                rs.getLong("id"),
                rs.getString("account"),
                rs.getString("action"),
                rs.getObject("target_id", Long.class),
                rs.getString("role"),
                rs.getString("description"),
                rs.getString("created_at")));
    }

    public long countToday() {
        ensureTable();

        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM operation_logs
                WHERE date(created_at) = date('now','localtime')
                """, Long.class);

        return count == null ? 0L : count;
    }

    private void ensureTable() {
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
