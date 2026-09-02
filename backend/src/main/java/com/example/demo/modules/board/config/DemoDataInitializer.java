package com.example.demo.modules.board.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.repository.*;
import com.example.demo.modules.board.dto.TeamPostRequest;
import com.example.demo.modules.board.service.TeamPostService;
import com.example.demo.modules.game.management.service.GameManagementService;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer {
	private final MemberRepository members;
	private final TeamPostService posts;
	private final GameManagementService games;
	private final PasswordEncoder encoder;

	@EventListener(ApplicationReadyEvent.class)
	public void run() {
		if (members.count() > 0)
			return;
		Member tom = member("teamleader", "Tom", "tom@example.com", 50, "CAPTAIN");
		member("player02", "Amy", "amy@example.com", 45, "PLAYER");
		// GameDataInitializer 的 CommandLineRunner 完成後，才使用正式遊戲資料建立範例。
		games.findEnabledGames().forEach(game -> game.getModes().stream()
				.filter(mode -> mode.getMaxPlayers() > 1).forEach(mode -> {
			TeamPostRequest form = new TeamPostRequest();
			form.setCaptainId(tom.getId());
			form.setGameId(game.getGameId());
			form.setModeId(mode.getModeId());
			form.setTitle(game.getGameName() + " 隊友募集！");
			form.setActivityType("新手友善");
			form.setDescription("歡迎一起遊玩，隊長核准且滿員後自動建立房間！");
			form.setStartTime(LocalDateTime.now().plusDays(1));
			posts.create(form);
		}));
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

}
