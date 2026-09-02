package com.example.demo.modules.board.service;

import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.repository.*;
import com.example.demo.modules.game.management.service.GameManagementService;
import com.example.demo.modules.game.poker.dto.JoinResult;
import com.example.demo.modules.game.poker.service.PokerGameService;
import com.example.demo.modules.lobby.entity.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BoardTeamService {
    private final TeamPostRepository posts;
    private final JoinRequestRepository joins;
    private final MemberRepository members;
    private final NotificationRepository notices;
    private final BoardRoomService rooms;
    private final GameManagementService games;
    private final PokerGameService poker;

    @Transactional
    public TeamPost kick(Long postId, Long captainId, String account) {
        TeamPost post = captainPost(postId, captainId);
        if (post.getStatus() != PostStatus.RECRUITING && post.getStatus() != PostStatus.FULL)
            throw new IllegalArgumentException("遊戲已開始或隊伍已結束，不能踢除隊員");
        if (post.getCaptain().getAccount().equals(account)) throw new IllegalArgumentException("不能踢除隊長本人");
        JoinRequest application = joins.findByPostIdAndApplicantAccountAndStatus(postId, account, ApplicationStatus.APPROVED)
                .orElseThrow(() -> new IllegalArgumentException("這位玩家不在已核准隊員名單中"));
        post.setCurrentPlayers(post.getCurrentPlayers() - 1);
        post.setStatus(PostStatus.RECRUITING);
        application.setStatus(ApplicationStatus.CANCELLED);
        posts.saveAndFlush(post);
        rooms.removePlayer(post, account);
        Notification notice = new Notification();
        notice.setMember(application.getApplicant());
        notice.setPostId(postId);
        notice.setApplicationId(application.getId());
        notice.setTitle("已被移出隊伍");
        notice.setMessage("隊長已將你移出「" + post.getTitle() + "」，可以尋找其他隊伍。");
        notices.save(notice);
        return post;
    }

    @Transactional
    public Map<String, String> start(Long postId, Long captainId) {
        TeamPost post = captainPost(postId, captainId);
        if (post.getStatus() == PostStatus.STARTING) return access(post, captainId);
        if (post.getStatus() != PostStatus.FULL || post.getRoomId() == null
                || !post.getCurrentPlayers().equals(post.getMaxPlayers()))
            throw new IllegalArgumentException("請先核准隊員並達到選定人數，再開始遊戲");
        post.setStatus(PostStatus.STARTING);
        posts.saveAndFlush(post);
        rooms.start(post);
        rooms.notifyRoster(post, "遊戲已開始", "「" + post.getTitle() + "」已開始，請按進入遊戲。");
        return access(post, captainId);
    }

    @Transactional(readOnly = true)
    public Map<String, String> access(Long postId, Long memberId) {
        return access(post(postId), memberId);
    }

    private Map<String, String> access(TeamPost post, Long memberId) {
        Member member = requireMember(post, memberId);
        if (post.getRoomId() == null) throw new IllegalArgumentException("隊伍尚未滿員，房間尚未建立");
        Room room = rooms.room(post.getRoomId());
        if (!room.getPlayers().contains(member.getAccount())) throw new IllegalArgumentException("你已不在房間名單中");
        Map<String, String> result = new LinkedHashMap<>();
        result.put("roomId", room.getId());
        result.put("status", room.getStatus());
        if ("PLAYING".equals(room.getStatus())) {
            String path = games.findGame(post.getGameId(), false).getFrontendPath();
            String url = UriComponentsBuilder.fromUriString(path)
                    .queryParam("roomId", room.getId()).queryParam("modeId", post.getModeId())
                    .queryParam("modeCode", post.getModeCode()).queryParam("boardPostId", post.getId())
                    .queryParam("memberId", member.getId()).build().encode().toUriString();
            result.put("gameUrl", url);
        }
        return result;
    }

    /** Board 會員與 User 模組 ID 不可混用；驗證名單後交給原有遊戲 Service。 */
    @Transactional(readOnly = true)
    public JoinResult joinGame(Long postId, Long memberId) {
        TeamPost post = post(postId);
        Member member = requireMember(post, memberId);
        Map<String, String> entry = access(post, memberId);
        if (!"PLAYING".equals(entry.get("status"))) throw new IllegalArgumentException("請等待隊長開始遊戲");
        rooms.validateMode(post);
        rooms.validateRoom(post, rooms.room(post.getRoomId()));
        if (!"POKER".equals(games.findGame(post.getGameId(), false).getGameCode()))
            throw new IllegalArgumentException("此遊戲尚未提供 Board 遊戲入口");
        return poker.join(post.getRoomId(), post.getModeCode(), member.getId(), member.getNickname());
    }

    private Member requireMember(TeamPost post, Long memberId) {
        if (!post.getCaptain().getId().equals(memberId)
                && !joins.existsByPostIdAndApplicantIdAndStatus(post.getId(), memberId, ApplicationStatus.APPROVED))
            throw new IllegalArgumentException("只有隊長與已核准隊員可以進入遊戲");
        return members.findById(memberId).orElseThrow(() -> new IllegalArgumentException("找不到會員"));
    }

    private TeamPost captainPost(Long id, Long captainId) {
        TeamPost post = post(id);
        if (!post.getCaptain().getId().equals(captainId)) throw new IllegalArgumentException("只有隊長可以管理隊伍");
        return post;
    }

    private TeamPost post(Long id) {
        return posts.findById(id).orElseThrow(() -> new IllegalArgumentException("找不到公告"));
    }
}
