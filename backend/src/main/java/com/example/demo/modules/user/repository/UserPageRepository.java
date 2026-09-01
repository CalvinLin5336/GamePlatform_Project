package com.example.demo.modules.user.repository;

import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.entity.User;
import com.example.demo.modules.user.entity.Role;
import com.example.demo.modules.user.entity.Status;
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
        return jdbcTemplate.query(userSelectSql() + " ORDER BY u.id", (rs, rowNum) -> toUserResponse(toUser(rs)));
    }

    public UserResponse findById(Long id) {
        List<UserResponse> users = jdbcTemplate.query(
                userSelectSql() + " WHERE u.id = ?",
                (rs, rowNum) -> toUserResponse(toUser(rs)), id);
        return users.isEmpty() ? null : users.get(0);
    }

    public UserResponse findByAccount(String account) {
        List<UserResponse> users = jdbcTemplate.query(
                userSelectSql() + " WHERE u.account = ?",
                (rs, rowNum) -> toUserResponse(toUser(rs)), account);
        return users.isEmpty() ? null : users.get(0);
    }

    public LoginRow findLoginUser(String account) {
        List<LoginRow> rows = jdbcTemplate.query("""
                SELECT u.id, u.account, u.password, u.username,
                       r.role_name AS role, s.status_name AS status
                FROM users u
                JOIN roles r ON r.id = u.role_id
                JOIN statuses s ON s.id = u.status_id
                WHERE u.account = ?
                """, (rs, rowNum) -> new LoginRow(
                rs.getLong("id"),
                rs.getString("account"),
                rs.getString("password"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("status")), account);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insert(String account, String password, String username, String avatar,
                       String description, String role, String status, String now) {
        Role roleEntity = findRole(role);
        Status statusEntity = findStatus(status);
        Long roleId = roleEntity.getId();
        Long statusId = statusEntity.getId();

        jdbcTemplate.update("""
                INSERT INTO users(account,password,username,avatar,description,role_id,status_id,last_login,created_at,updated_at)
                VALUES(?,?,?,?,?,?,?,NULL,?,?)
                """, account, password, username, avatar, description, roleId, statusId, now, now);
    }

    public int updateWithoutPassword(Long id, String account, String username, String avatar,
                                     String description, String role, String status, String now) {
        int updated = jdbcTemplate.update("""
                UPDATE users
                SET account=?, username=?, avatar=?, description=?, role_id=?, status_id=?, updated_at=?
                WHERE id=?
                """, account, username, avatar, description, findRole(role).getId(), findStatus(status).getId(), now, id);
        return updated;
    }

    public int updateWithPassword(Long id, String account, String password, String username,
                                  String avatar, String description, String role, String status,
                                  String now) {
        int updated = jdbcTemplate.update("""
                UPDATE users
                SET account=?, password=?, username=?, avatar=?, description=?, role_id=?, status_id=?, updated_at=?
                WHERE id=?
                """, account, password, username, avatar, description,
                findRole(role).getId(), findStatus(status).getId(), now, id);
        return updated;
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    public void updateLastLogin(Long id, String now) {
        jdbcTemplate.update("UPDATE users SET last_login=?, updated_at=? WHERE id=?", now, now, id);
    }

    public boolean existsByAccount(String account, Long excludeId) {
        Integer count = excludeId == null
                ? jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE account = ?", Integer.class, account)
                : jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE account = ? AND id <> ?", Integer.class, account, excludeId);
        return count != null && count > 0;
    }

    private Role findRole(String roleName) {
        return jdbcTemplate.queryForObject(
                "SELECT id, role_name FROM roles WHERE UPPER(role_name) = UPPER(?)",
                (rs, rowNum) -> new Role(rs.getLong("id"), rs.getString("role_name")),
                roleName);
    }

    private Status findStatus(String statusName) {
        return jdbcTemplate.queryForObject(
                "SELECT id, status_name FROM statuses WHERE LOWER(status_name) = LOWER(?)",
                (rs, rowNum) -> new Status(rs.getLong("id"), rs.getString("status_name")),
                statusName);
    }

    private String userSelectSql() {
        return """
                SELECT u.id, u.account, u.password, u.username, u.avatar, u.description,
                       u.role_id, u.status_id, u.last_login, u.created_at, u.updated_at,
                       r.role_name AS role, s.status_name AS account_status
                FROM users u
                JOIN roles r ON r.id = u.role_id
                JOIN statuses s ON s.id = u.status_id
                """;
    }

    private User toUser(java.sql.ResultSet rs) throws java.sql.SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setAccount(rs.getString("account"));
        user.setPassword(rs.getString("password"));
        user.setUsername(rs.getString("username"));
        user.setAvatar(rs.getString("avatar"));
        user.setDescription(rs.getString("description"));
        user.setRoleId(rs.getLong("role_id"));
        user.setStatusId(rs.getLong("status_id"));
        user.setLastLogin(rs.getString("last_login"));
        user.setCreatedAt(rs.getString("created_at"));
        user.setUpdatedAt(rs.getString("updated_at"));
        return user;
    }

    private UserResponse toUserResponse(User user) {
        Role role = jdbcTemplate.queryForObject(
                "SELECT id, role_name FROM roles WHERE id = ?",
                (rs, rowNum) -> new Role(rs.getLong("id"), rs.getString("role_name")),
                user.getRoleId());
        Status status = jdbcTemplate.queryForObject(
                "SELECT id, status_name FROM statuses WHERE id = ?",
                (rs, rowNum) -> new Status(rs.getLong("id"), rs.getString("status_name")),
                user.getStatusId());

        return new UserResponse(user.getId(), user.getAccount(), user.getUsername(), user.getAvatar(),
                user.getDescription(), role.getRoleName(), status.getStatusName(), user.getLastLogin());
    }

    public record LoginRow(Long id, String account, String password, String username, String role, String status) {}
}
