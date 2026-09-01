package com.example.demo.modules.board.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.repository.*;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {
	private final MemberRepository members;
	private final TeamPostRepository posts;
	private final PasswordEncoder encoder;

	public void run(String... args) {
		if (members.count() > 0)
			return;
		Member tom = member("teamleader", "Tom", "tom@example.com", 50, "CAPTAIN");
		member("player02", "Amy", "amy@example.com", 45, "PLAYER");
		post(tom, "撲克牌高手募集！", "撲克牌大對決", "四人競技牌局", 4, 2, PostStatus.RECRUITING, "喜歡策略牌局的玩家一起來，規則簡單，新手也能快速加入！",
				"撲克牌,策略,新手友善");
		post(tom, "知識王問答挑戰隊", "趣味問答王", "團隊知識挑戰", 5, 3, PostStatus.RECRUITING, "募集不同領域的知識夥伴，一起挑戰綜合題庫並爭取最高分！",
				"問答,知識,團隊合作");
		post(tom, "圖靈解密破譯小隊", "圖靈解密", "協力密碼破解", 4, 4, PostStatus.FULL, "透過邏輯、線索與程式概念合作破譯，歡迎喜歡推理的玩家。", "圖靈解密,邏輯,解謎");
	}

	private Member member(String a, String n, String e, int level, String role) {
		Member m = new Member();
		m.setAccount(a);
		m.setPassword(encoder.encode("password"));
		m.setNickname(n);
		m.setEmail(e);
		m.setLevel(level);
		m.setRole(role);
		return members.save(m);
	}

	private void post(Member m, String title, String game, String activity, int max, int current, PostStatus status,
			String description, String tags) {
		TeamPost p = new TeamPost();
		p.setCaptain(m);
		p.setTitle(title);
		p.setGameName(game);
		p.setActivityType(activity);
		p.setMaxPlayers(max);
		p.setCurrentPlayers(current);
		p.setStatus(status);
		p.setVoiceRequired(true);
		p.setRankRequirement("不限");
		p.setStartTime(LocalDateTime.now().plusDays(1));
		p.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
		p.setDescription(description);
		p.setTags(tags);
		posts.save(p);
	}
}
