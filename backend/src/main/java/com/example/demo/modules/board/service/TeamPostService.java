package com.example.demo.modules.board.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.demo.modules.board.dto.TeamPostRequest;
import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamPostService {
	private final TeamPostRepository posts;
	private final MemberRepository members;

	public List<TeamPost> list(String keyword, PostStatus status) {
		return posts.search(keyword == null ? "" : keyword.trim(), status);
	}

	public TeamPost get(Long id) {
		return posts.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到公告"));
	}

	public List<TeamPost> mine(Long memberId) {
		return posts.findByCaptainIdOrderByCreatedAtDesc(memberId);
	}

	public TeamPost create(TeamPostRequest f) {
		TeamPost p = new TeamPost();
		apply(p, f);
		p.setCaptain(members.findById(f.getCaptainId()).orElseThrow(() -> new IllegalArgumentException("找不到會員")));
		return posts.save(p);
	}

	public TeamPost update(Long id, TeamPostRequest f) {
		TeamPost p = get(id);
		apply(p, f);
		return posts.save(p);
	}

	public void delete(Long id) {
		posts.deleteById(id);
	}

	private void apply(TeamPost p, TeamPostRequest f) {
		p.setTitle(f.getTitle());
		p.setGameName(f.getGameName());
		p.setActivityType(f.getActivityType());
		p.setStartTime(f.getStartTime());
		p.setEndTime(f.getEndTime());
		p.setMaxPlayers(f.getMaxPlayers());
		p.setVoiceRequired(Boolean.TRUE.equals(f.getVoiceRequired()));
		p.setRankRequirement(f.getRankRequirement());
		p.setDescription(f.getDescription());
		p.setTags(f.getTags());
		if (f.getStatus() != null)
			p.setStatus(f.getStatus());
	}
}
