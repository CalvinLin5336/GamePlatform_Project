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
	private final BoardNotificationService notices;
    private final BoardEvents events;
	private final TeamPostRepository posts;
	private final MemberRepository members;
	private final BoardRoomService rooms;

	@Transactional
	public JoinRequest join(Long postId, JoinRequestForm f) {
		TeamPost p = post(postId);
		if (p.getGameId() == null || p.getModeId() == null)
			throw new IllegalArgumentException("此舊公告尚未設定遊戲模式，請隊長重新建立隊伍");
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
		joins.save(j);
		notice(p.getCaptain(), "收到加入申請", m.getNickname() + " 想加入「" + p.getTitle() + "」"
                + (f.getMessage() == null || f.getMessage().isBlank() ? "" : "\n申請留言：" + f.getMessage()), j);
		events.postChanged(p.getId());
		return j;
	}

	public List<JoinRequest> myApplications(Long memberId) {
		return joins.findByApplicantIdOrderByCreatedAtDesc(memberId);
	}

	public List<JoinRequest> captainRequests(Long captainId) {
		return joins.findByPostCaptainIdOrderByCreatedAtDesc(captainId);
	}

	@Transactional
	public JoinRequest review(Long id, ApplicationStatus status, Long captainId) {
		JoinRequest j = joins.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到申請"));
		if (!j.getPost().getCaptain().getId().equals(captainId))
			throw new IllegalArgumentException("只有此隊伍的隊長可以審核");
		if (j.getStatus() != ApplicationStatus.PENDING)
			throw new IllegalArgumentException("此申請已完成審核");
		if (status != ApplicationStatus.APPROVED && status != ApplicationStatus.REJECTED)
			throw new IllegalArgumentException("不支援的審核狀態");
		if (status == ApplicationStatus.APPROVED) {
			TeamPost p = j.getPost();
			if (p.getStatus() != PostStatus.RECRUITING || p.getCurrentPlayers() >= p.getMaxPlayers())
				throw new IllegalArgumentException("隊伍已滿或停止招募");
			p.setCurrentPlayers(p.getCurrentPlayers() + 1);
			j.setStatus(status);
			joins.saveAndFlush(j);
			rooms.createWhenFull(p);
			posts.save(p);
		}
		j.setStatus(status);
		notice(j.getApplicant(), "申請結果", status == ApplicationStatus.APPROVED ? "隊長已同意你的申請" : "隊長已拒絕你的申請", j);
		events.postChanged(j.getPost().getId());
		return joins.save(j);
	}

	public com.example.demo.modules.board.dto.BoardPage<Comment> commentPage(Long postId, int page) {
        if (page < 0) throw new IllegalArgumentException("頁碼不可小於 0");
        post(postId);
        return com.example.demo.modules.board.dto.BoardPage.from(comments.findByPostIdOrderByCreatedAtAscIdAsc(postId, org.springframework.data.domain.PageRequest.of(page, 10)));
    }

    public List<Comment> comments(Long postId) {
		return comments.findByPostIdOrderByCreatedAtAsc(postId);
	}

	@Transactional
	public Comment addComment(Long postId, CommentRequest f) {
		Comment c = new Comment();
		c.setPost(post(postId));
		c.setMember(member(f.getMemberId()));
		c.setContent(f.getContent());
		comments.save(c);
        notices.commentAdded(c);
        events.commentsChanged(postId);
        return c;
	}

	@Transactional
    public void deleteComment(Long id, Long memberId) {
        Comment comment = comments.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到留言"));
        if (!comment.getMember().getId().equals(memberId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "只能刪除自己的留言");
        comments.delete(comment);
        events.commentsChanged(comment.getPost().getId());
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


	private TeamPost post(Long id) {
		return posts.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到公告"));
	}

	private Member member(Long id) {
		if (id == null)
			throw new IllegalArgumentException("會員資料不可為空");
		return members.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到會員"));
	}

	private void notice(Member m, String title, String message, JoinRequest application) {
        notices.send(m, application.getPost(), m.getId().equals(application.getPost().getCaptain().getId()) ? "CAPTAIN" : "APPLICANT",
                title, message, application.getId(), null);
	}
}
