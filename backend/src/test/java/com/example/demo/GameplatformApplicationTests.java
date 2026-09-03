package com.example.demo;

import com.example.demo.modules.user.database.UserDatabaseInitializer;
import com.example.demo.modules.user.dto.UserRequest;
import com.example.demo.modules.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
class GameplatformApplicationTests {

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry properties) throws Exception {
		String url = "jdbc:sqlite:" + Files.createTempDirectory("platform-startup-test-").resolve("test.db");
		properties.add("spring.datasource.url", () -> url);
	}

	@Autowired JdbcTemplate jdbc;
	@Autowired UserService users;
	@Autowired UserDatabaseInitializer initializer;

	@Test
	void contextLoads() {
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM members", Integer.class));
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM team_posts", Integer.class));
		assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM roles", Integer.class));
		assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM statuses", Integer.class));

		// 模擬已有管理員後重新啟動：保留帳號與密碼，不再插入固定密碼的測試 admin。
		var admin = users.create(new UserRequest("admin", "StartupTest123", "平台管理員", null, null, "ADMIN", "Active"), "TEST");
		String password = jdbc.queryForObject("SELECT password FROM users WHERE id = ?", String.class, admin.id());
		initializer.initialize();
		assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
		assertEquals(password, jdbc.queryForObject("SELECT password FROM users WHERE id = ?", String.class, admin.id()));
		assertEquals("平台管理員", users.findById(admin.id()).username());
	}

}
