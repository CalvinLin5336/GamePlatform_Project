package com.example.demo.modules.user.repository;

import com.example.demo.modules.user.dto.OperationLogResponse;
import com.example.demo.modules.user.entity.OperationLog;
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
        jdbcTemplate.update("""
                INSERT INTO operation_logs(account, action, target_id, role, description, created_at)
                VALUES(?,?,?,?,?,?)
                """, account, action, targetId, role, description, createdAt);
    }

    public List<OperationLogResponse> findAll() {

        return jdbcTemplate.query("""
                SELECT id, account, action, target_id, role, description, created_at
                FROM operation_logs
                ORDER BY id DESC
                """, (rs, rowNum) -> {
            OperationLog log = new OperationLog();
            log.setId(rs.getLong("id"));
            log.setAccount(rs.getString("account"));
            log.setAction(rs.getString("action"));
            log.setTargetId(rs.getObject("target_id", Long.class));
            log.setRole(rs.getString("role"));
            log.setDescription(rs.getString("description"));
            log.setCreatedAt(rs.getString("created_at"));

            return new OperationLogResponse(
                    log.getId(),
                    log.getAccount(),
                    log.getAction(),
                    log.getTargetId(),
                    log.getRole(),
                    log.getDescription(),
                    log.getCreatedAt()
            );
        });
    }

    public long countToday() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM operation_logs
                WHERE date(created_at) = date('now','localtime')
                """, Long.class);
        return count == null ? 0L : count;
    }
}
