package com.example.demo.modules.board.service;
import com.example.demo.modules.board.dto.BoardPage;
import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BoardNotificationService {
    private final NotificationRepository notices;
    private final TeamPostRepository posts;
    private final JoinRequestRepository joins;
    private final FavoriteRepository favorites;
    private final CommentRepository comments;
    private final BoardEvents events;
    public record NoticeView(Long id, String title, String message, Long postId, Long commentId,
                             String category, boolean readFlag, LocalDateTime createdAt) {}
    @Transactional
    public Notification send(Member member, TeamPost post, String category, String title, String message, Long applicationId, Long commentId) {
        Notification notice = new Notification();
        notice.setMember(member); notice.setPostId(post.getId()); notice.setCategory(category);
        notice.setTitle(title); notice.setMessage(message); notice.setApplicationId(applicationId); notice.setCommentId(commentId);
        notices.save(notice);
        events.publish(member.getId(), Map.of("type", "NOTIFICATION", "postId", post.getId(), "noticeId", notice.getId(), "title", title, "message", message));
        return notice;
    }
    @Transactional
    public void commentAdded(Comment comment) {
        TeamPost post = comment.getPost();
        Map<Long, Member> recipients = new LinkedHashMap<>();
        Map<Long, String> categories = new HashMap<>();
        comments.participants(post.getId()).forEach(m -> recipients.put(m.getId(), m));
        favorites.findByPostId(post.getId()).forEach(f -> recipients.put(f.getMember().getId(), f.getMember()));
        joins.findByPostId(post.getId()).stream().filter(j -> j.getStatus()==ApplicationStatus.PENDING || j.getStatus()==ApplicationStatus.APPROVED)
            .forEach(j -> { recipients.put(j.getApplicant().getId(), j.getApplicant()); categories.put(j.getApplicant().getId(), "APPLICANT"); });
        recipients.put(post.getCaptain().getId(), post.getCaptain());
        categories.put(post.getCaptain().getId(), "CAPTAIN");
        recipients.remove(comment.getMember().getId());
        String message = comment.getMember().getNickname() + " 在「" + post.getTitle() + "」留言：" + comment.getContent();
        recipients.forEach((id, member) -> send(member, post, categories.getOrDefault(id, "WATCHING"), "收到新留言", message, null, comment.getId()));
    }
    @Transactional(readOnly=true)
    public BoardPage<NoticeView> page(Long memberId, NotificationCategory category, int page) {
        if (page < 0) throw new IllegalArgumentException("頁碼不可小於 0");
        return BoardPage.from(notices.page(memberId, category == null ? null : category.name(), PageRequest.of(page, 10)).map(n -> {
            String type = n.getCategory();
            if (type == null) type = posts.findById(n.getPostId() == null ? -1L : n.getPostId())
                .filter(p -> p.getCaptain().getId().equals(memberId)).isPresent() ? "CAPTAIN" : n.getApplicationId() != null ? "APPLICANT" : "WATCHING";
            return new NoticeView(n.getId(), n.getTitle(), n.getMessage(), n.getPostId(), n.getCommentId(), type, Boolean.TRUE.equals(n.getReadFlag()), n.getCreatedAt());
        }));
    }
    @Transactional(readOnly=true)
    public Map<String, Object> summary(Long memberId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (NotificationCategory category : NotificationCategory.values()) counts.put(category.name(), notices.unread(memberId, category.name()));
        return Map.of("unread", notices.unread(memberId, null), "unreadComments", notices.unreadComments(memberId), "categories", counts);
    }
    @Transactional
    public void markRead(Long memberId, List<Long> ids) {
        if (ids == null || ids.size() > 100) throw new IllegalArgumentException("一次最多標記 100 則通知");
        notices.findAllById(ids).stream().filter(n -> n.getMember().getId().equals(memberId)).forEach(n -> n.setReadFlag(true));
        events.publish(memberId, Map.of("type", "NOTIFICATIONS_READ"));
    }
}
