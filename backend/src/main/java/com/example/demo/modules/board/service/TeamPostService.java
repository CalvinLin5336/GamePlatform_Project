package com.example.demo.modules.board.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.modules.board.dto.TeamPostRequest;
import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.repository.*;
import com.example.demo.modules.game.management.dto.GameView;
import com.example.demo.modules.game.management.dto.GameModeView;
import com.example.demo.modules.game.management.service.GameManagementService;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TeamPostService {
	private final TeamPostRepository posts;
	private final MemberRepository members;
	private final JoinRequestRepository joins;
	private final GameManagementService games;
	private final BoardRoomService rooms;

	public List<TeamPost> list(String keyword, PostStatus status, Long gameId, Long modeId) {
		return posts.search(keyword == null ? "" : keyword.trim(), status, gameId, modeId);
	}

	public TeamPost get(Long id) {
		return posts.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到公告"));
	}

	public List<TeamPost> mine(Long memberId) {
		return posts.findByCaptainIdOrderByCreatedAtDesc(memberId);
	}

	@Transactional
	public TeamPost create(TeamPostRequest f) {
		TeamPost p = new TeamPost();
		selectMode(p, f);
		apply(p, f);
		p.setCaptain(members.findById(f.getCaptainId()).orElseThrow(() -> new IllegalArgumentException("找不到會員")));
		posts.save(p);
		rooms.createWhenFull(p);
		return posts.save(p);
	}

	@Transactional
	public TeamPost update(Long id, TeamPostRequest f) {
		TeamPost p = get(id);
		if (!p.getCaptain().getId().equals(f.getCaptainId()))
			throw new IllegalArgumentException("只有隊長可以編輯公告");
		boolean changed = !Objects.equals(p.getGameId(), f.getGameId()) || !Objects.equals(p.getModeId(), f.getModeId());
		changed = changed || (f.getPlayerCount() != null && !Objects.equals(f.getPlayerCount(), p.getMaxPlayers()));
		if (changed) {
			if (p.getRoomId() != null || p.getCurrentPlayers() > 1 || joins.existsByPostId(id))
				throw new IllegalArgumentException("已有申請、隊員或房間，不能變更遊戲、模式或人數；請重新建立隊伍");
			selectMode(p, f);
		}
		apply(p, f);
		if (p.getStatus() == PostStatus.RECRUITING || p.getStatus() == PostStatus.FULL)
			rooms.createWhenFull(p);
		return posts.save(p);
	}

	@Transactional(readOnly = true)
	public String roomId(Long id, Long memberId) {
		TeamPost p = get(id);
		if (!p.getCaptain().getId().equals(memberId)
				&& !joins.existsByPostIdAndApplicantIdAndStatus(id, memberId, ApplicationStatus.APPROVED))
			throw new IllegalArgumentException("只有隊長與已核准隊員可以進入房間");
		if (p.getRoomId() == null)
			throw new IllegalArgumentException("隊伍尚未滿員，房間尚未建立");
		rooms.requireRoom(p.getRoomId());
		return p.getRoomId();
	}

	public void delete(Long id) {
		posts.deleteById(id);
	}

	private void apply(TeamPost p, TeamPostRequest f) {
		if (f.getStartTime() != null && f.getEndTime() != null && f.getEndTime().isBefore(f.getStartTime()))
			throw new IllegalArgumentException("結束時間不能早於開始時間");
		p.setTitle(f.getTitle());
		p.setActivityType(f.getActivityType());
		p.setStartTime(f.getStartTime());
		p.setEndTime(f.getEndTime());
		p.setVoiceRequired(Boolean.TRUE.equals(f.getVoiceRequired()));
		p.setRankRequirement(f.getRankRequirement() == null || f.getRankRequirement().isBlank()
				? null : f.getRankRequirement().trim());
		p.setDescription(f.getDescription());
		p.setTags(f.getTags());
	}

	private void selectMode(TeamPost p, TeamPostRequest f) {
		GameView game = games.findGame(f.getGameId(), false);
		GameModeView mode = game.getModes().stream()
				.filter(m -> Objects.equals(m.getModeId(), f.getModeId()) && m.isEnabled()
						&& Objects.equals(m.getGameId(), game.getGameId()))
				.findFirst().orElseThrow(() -> new IllegalArgumentException("此遊戲沒有該模式，或模式已停用"));
		if (mode.getMinPlayers() < 1 || mode.getMaxPlayers() < mode.getMinPlayers() || mode.getComputerPlayers() < 0)
			throw new IllegalArgumentException("遊戲模式的人數設定不正確");
		int playerCount = f.getPlayerCount() == null ? mode.getMaxPlayers() : f.getPlayerCount();
		if (playerCount < mode.getMinPlayers() || playerCount > mode.getMaxPlayers())
			throw new IllegalArgumentException("遊玩人數必須介於 " + mode.getMinPlayers() + "～" + mode.getMaxPlayers() + " 人（含隊長）");
		p.setGameId(game.getGameId());
		p.setGameName(game.getGameName());
		p.setModeId(mode.getModeId());
		p.setModeCode(mode.getModeCode());
		p.setModeName(mode.getModeName());
		p.setMinPlayers(mode.getMinPlayers());
		p.setModeMaxPlayers(mode.getMaxPlayers());
		p.setMaxPlayers(playerCount);
		p.setComputerPlayers(mode.getComputerPlayers());
	}
}
