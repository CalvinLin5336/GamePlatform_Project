package com.example.demo.modules.board.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.modules.board.dto.*;
import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InteractionService {
	private final JoinRequestRepository joins;
	private final CommentRepository comments;
	private final FavoriteRepository favorites;
	private final NotificationRepository notices;
	private final TeamPostRepository posts;
	private final MemberRepository members;

	@Transactional
	public JoinRequest join(Long postId, JoinRequestForm f) {
		TeamPost p = post(postId);
		Member m = member(f.getMemberId());
		if (p.getCaptain().getId().equals(m.getId()))
			throw new IllegalArgumentException("隊長不能申請加入自己的隊伍");
		if (p.getStatus() != PostStatus.RECRUITING || p.getCurrentPlayers() >= p.getMaxPlayers())
			throw new IllegalArgumentException("此隊伍目前無法加入");
		if (joins.existsByPostIdAndApplicantId(postId, f.getMemberId()))
			throw new IllegalArgumentException("已申請過此隊伍");
		JoinRequest j = new JoinRequest();
		j.setPost(p);
		j.setApplicant(m);
		j.setMessage(f.getMessage());
		notice(p.getCaptain(), "收到加入申請", m.getNickname() + " 想加入「" + p.getTitle() + "」");
		return joins.save(j);
	}

	public List<JoinRequest> myApplications(Long memberId) {
		return joins.findByApplicantIdOrderByCreatedAtDesc(memberId);
	}

	public List<JoinRequest> captainRequests(Long captainId) {
		return joins.findByPostCaptainIdOrderByCreatedAtDesc(captainId);
	}

	@Transactional
	public JoinRequest review(Long id, ApplicationStatus status) {
		JoinRequest j = joins.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到申請"));
		if (j.getStatus() != ApplicationStatus.PENDING)
			throw new IllegalArgumentException("此申請已完成審核");
		if (status != ApplicationStatus.APPROVED && status != ApplicationStatus.REJECTED)
			throw new IllegalArgumentException("不支援的審核狀態");
		if (status == ApplicationStatus.APPROVED) {
			TeamPost p = j.getPost();
			if (p.getStatus() != PostStatus.RECRUITING || p.getCurrentPlayers() >= p.getMaxPlayers())
				throw new IllegalArgumentException("隊伍已滿或停止招募");
			p.setCurrentPlayers(p.getCurrentPlayers() + 1);
			if (p.getCurrentPlayers() >= p.getMaxPlayers())
				p.setStatus(PostStatus.FULL);
			posts.save(p);
		}
		j.setStatus(status);
		notice(j.getApplicant(), "申請結果", status == ApplicationStatus.APPROVED ? "隊長已同意你的申請" : "隊長已拒絕你的申請");
		return joins.save(j);
	}

	public List<Comment> comments(Long postId) {
		return comments.findByPostIdOrderByCreatedAtAsc(postId);
	}

	public Comment addComment(Long postId, CommentRequest f) {
		Comment c = new Comment();
		c.setPost(post(postId));
		c.setMember(member(f.getMemberId()));
		c.setContent(f.getContent());
		return comments.save(c);
	}

	public void deleteComment(Long id) {
		comments.deleteById(id);
	}

	@Transactional
	public boolean toggleFavorite(Long postId, Long memberId) {
		var old = favorites.findByPostIdAndMemberId(postId, memberId);
		if (old.isPresent()) {
			favorites.delete(old.get());
			return false;
		}
		Favorite f = new Favorite();
		f.setPost(post(postId));
		f.setMember(member(memberId));
		favorites.save(f);
		return true;
	}

	public List<Favorite> favoriteList(Long memberId) {
		return favorites.findByMemberId(memberId);
	}

	public List<Notification> notifications(Long memberId) {
		return notices.findByMemberIdOrderByCreatedAtDesc(memberId);
	}

	private TeamPost post(Long id) {
		return posts.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到公告"));
	}

	private Member member(Long id) {
		if (id == null)
			throw new IllegalArgumentException("會員資料不可為空");
		return members.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到會員"));
	}

	private void notice(Member m, String title, String message) {
		Notification n = new Notification();
		n.setMember(m);
		n.setTitle(title);
		n.setMessage(message);
		notices.save(n);
	}
}
