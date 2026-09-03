package com.example.demo.modules.user.database;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserDatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;
    public UserDatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("PRAGMA foreign_keys = ON");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS roles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    role_name VARCHAR(20) NOT NULL UNIQUE
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS statuses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    status_name VARCHAR(20) NOT NULL UNIQUE
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(500) NOT NULL,
                    username VARCHAR(100) NOT NULL,
                    avatar VARCHAR(500),
                    description VARCHAR(500),
                    role_id INTEGER NOT NULL,
                    status_id INTEGER NOT NULL,
                    last_login VARCHAR(19),
                    created_at VARCHAR(19) NOT NULL,
                    updated_at VARCHAR(19) NOT NULL,
                    FOREIGN KEY (role_id) REFERENCES roles(id),
                    FOREIGN KEY (status_id) REFERENCES statuses(id)
                )
                """);

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

        jdbcTemplate.update("INSERT OR IGNORE INTO roles(id, role_name) VALUES(1, 'PLAYER')");
        jdbcTemplate.update("INSERT OR IGNORE INTO roles(id, role_name) VALUES(2, 'ADMIN')");
        jdbcTemplate.update("INSERT OR IGNORE INTO statuses(id, status_name) VALUES(1, 'Active')");
        jdbcTemplate.update("INSERT OR IGNORE INTO statuses(id, status_name) VALUES(2, 'Disabled')");
    }
}
