package com.example.demo.modules.user.repository;

import com.example.demo.modules.user.dto.UserResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserPageRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserPageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserResponse> findAll() {
        ensureProfileTable();

        return jdbcTemplate.query("""
                SELECT u.id,
                       u.username AS account,
                       u.nickname AS display_username,
                       u.avatar_url,
                       p.description,
                       u.role,
                       COALESCE(p.status, 'Active') AS account_status,
                       p.last_login
                FROM users u
                LEFT JOIN user_profile p ON p.user_id = u.id
                ORDER BY u.id
                """, (rs, rowNum) -> toUserResponse(rs));
    }

    public UserResponse findById(Long id) {
        ensureProfileTable();

        List<UserResponse> users = jdbcTemplate.query("""
                SELECT u.id,
                       u.username AS account,
                       u.nickname AS display_username,
                       u.avatar_url,
                       p.description,
                       u.role,
                       COALESCE(p.status, 'Active') AS account_status,
                       p.last_login
                FROM users u
                LEFT JOIN user_profile p ON p.user_id = u.id
                WHERE u.id = ?
                """, (rs, rowNum) -> toUserResponse(rs), id);

        return users.isEmpty() ? null : users.get(0);
    }

    public UserResponse findByAccount(String account) {
        ensureProfileTable();

        List<UserResponse> users = jdbcTemplate.query("""
                SELECT u.id,
                       u.username AS account,
                       u.nickname AS display_username,
                       u.avatar_url,
                       p.description,
                       u.role,
                       COALESCE(p.status, 'Active') AS account_status,
                       p.last_login
                FROM users u
                LEFT JOIN user_profile p ON p.user_id = u.id
                WHERE u.username = ?
                """, (rs, rowNum) -> toUserResponse(rs), account);

        return users.isEmpty() ? null : users.get(0);
    }

    public LoginRow findLoginUser(String account) {
        ensureProfileTable();

        List<LoginRow> rows = jdbcTemplate.query("""
                SELECT u.id,
                       u.username AS account,
                       u.password,
                       u.nickname AS display_username,
                       u.role,
                       COALESCE(p.status, 'Active') AS account_status
                FROM users u
                LEFT JOIN user_profile p ON p.user_id = u.id
                WHERE u.username = ?
                """, (rs, rowNum) -> new LoginRow(
                rs.getLong("id"),
                rs.getString("account"),
                rs.getString("password"),
                rs.getString("display_username"),
                rs.getString("role"),
                rs.getString("account_status")), account);

        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insert(String account, String password, String username, String avatar,
                       String description, String role, String status, String now) {
        jdbcTemplate.update("""
                INSERT INTO users(username,password,nickname,avatar_url,role,created_at,updated_at)
                VALUES(?,?,?,?,?,?,?)
                """, account, password, username, avatar, role, now, now);

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?", Long.class, account);

        if (id == null) {
            throw new IllegalStateException("Created user could not be found");
        }

        upsertProfile(id, description, status, null);
    }

    public int updateWithoutPassword(Long id, String account, String username, String avatar,
                                     String description, String role, String status, String now) {
        int updated = jdbcTemplate.update("""
                UPDATE users
                SET username=?, nickname=?, avatar_url=?, role=?, updated_at=?
                WHERE id=?
                """, account, username, avatar, role, now, id);

        if (updated > 0) {
            upsertProfile(id, description, status, null);
        }
        return updated;
    }

    public int updateWithPassword(Long id, String account, String password, String username,
                                  String avatar, String description, String role, String status,
                                  String now) {
        int updated = jdbcTemplate.update("""
                UPDATE users
                SET username=?, password=?, nickname=?, avatar_url=?, role=?, updated_at=?
                WHERE id=?
                """, account, password, username, avatar, role, now, id);

        if (updated > 0) {
            upsertProfile(id, description, status, null);
        }
        return updated;
    }

    public void delete(Long id) {
        ensureProfileTable();
        jdbcTemplate.update("DELETE FROM user_profile WHERE user_id = ?", id);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    public void updateLastLogin(Long id, String now) {
        ensureProfileTable();
        jdbcTemplate.update("""
                INSERT INTO user_profile(user_id,status,last_login)
                VALUES(?, 'Active', ?)
                ON CONFLICT(user_id) DO UPDATE SET last_login=excluded.last_login
                """, id, now);
    }

    public boolean existsByAccount(String account, Long excludeId) {
        Integer count;
        if (excludeId == null) {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, account);
        } else {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = ? AND id <> ?",
                    Integer.class, account, excludeId);
        }
        return count != null && count > 0;
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

    private void upsertProfile(Long userId, String description, String status, String lastLogin) {
        ensureProfileTable();
        jdbcTemplate.update("""
                INSERT INTO user_profile(user_id,description,status,last_login)
                VALUES(?,?,?,?)
                ON CONFLICT(user_id) DO UPDATE SET
                    description=excluded.description,
                    status=excluded.status,
                    last_login=COALESCE(excluded.last_login, user_profile.last_login)
                """, userId, description, status, lastLogin);
    }

    private UserResponse toUserResponse(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new UserResponse(
                rs.getLong("id"),
                rs.getString("account"),
                rs.getString("display_username"),
                rs.getString("avatar_url"),
                rs.getString("description"),
                rs.getString("role"),
                rs.getString("account_status"),
                rs.getString("last_login")
        );
    }

    public record LoginRow(
            Long id,
            String account,
            String password,
            String username,
            String role,
            String status
    ) {}
}
