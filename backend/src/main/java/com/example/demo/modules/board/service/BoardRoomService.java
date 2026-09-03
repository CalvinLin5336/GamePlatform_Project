package com.example.demo.modules.board.service;

import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.repository.JoinRequestRepository;
import com.example.demo.modules.board.repository.NotificationRepository;
import com.example.demo.modules.game.management.dto.GameModeView;
import com.example.demo.modules.game.management.service.GameManagementService;
import com.example.demo.modules.lobby.controller.LobbyController;
import com.example.demo.modules.lobby.entity.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Board 經由 Lobby 既有入口操作房間，與公告共用同一個 SQLite 交易。 */
@Service
@RequiredArgsConstructor
public class BoardRoomService {
    private final LobbyController lobby;
    private final GameManagementService games;
    private final JoinRequestRepository joins;
    private final BoardNotificationService notices;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createWhenFull(TeamPost post) {
        if (post.getRoomId() == null && post.getCurrentPlayers() < post.getMaxPlayers()) return;
        validateMode(post);
        List<Member> roster = roster(post);
        if (roster.size() != post.getCurrentPlayers() || roster.size() > post.getMaxPlayers()
                || roster.stream().map(Member::getAccount).distinct().count() != roster.size())
            throw new IllegalArgumentException("隊伍人數與核准名單不一致，無法建立房間");

        boolean newlyFull = post.getStatus() != PostStatus.FULL;
        if (post.getRoomId() == null) {
            Map<String, Object> result = requireSuccess(lobby.createRoom(Map.of(
                    "hostAccount", post.getCaptain().getAccount(), "gameId", post.getGameId(),
                    "modeCode", post.getModeCode(), "playerCount", post.getMaxPlayers())));
            Object value = result.get("roomId");
            if (!(value instanceof String roomId) || roomId.isBlank())
                throw new IllegalArgumentException("Lobby 未回傳房號，請重新操作");
            post.setRoomId(roomId);
            newlyFull = true;
        }
        Room room = room(post.getRoomId());
        requireWaiting(room);
        validateRoom(post, room);
        List<String> accounts = roster.stream().map(Member::getAccount).toList();
        if (!accounts.containsAll(room.getPlayers()))
            throw new IllegalArgumentException("房間名單與隊伍不一致，請先整理房間成員");
        for (Member member : roster) {
            // 已存在的房間在踢人後繼續使用，補招的隊員只加入一次。
            if (!room.getPlayers().contains(member.getAccount()))
                requireSuccess(lobby.joinRoom(Map.of("roomId", post.getRoomId(), "playerAccount", member.getAccount())));
        }
        boolean full = post.getCurrentPlayers().equals(post.getMaxPlayers());
        post.setStatus(full ? PostStatus.FULL : PostStatus.RECRUITING);
        if (full && newlyFull) notifyRoster(post, "隊伍已滿，可以開始遊戲",
                "「" + post.getTitle() + "」房號：" + post.getRoomId() + "，等待隊長開始遊戲。");
    }

    public List<Member> roster(TeamPost post) {
        List<Member> result = new ArrayList<>();
        result.add(post.getCaptain());
        joins.findByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), ApplicationStatus.APPROVED)
                .forEach(j -> result.add(j.getApplicant()));
        return result;
    }

    public void validateMode(TeamPost post) {
        if (post.getGameId() == null || post.getModeId() == null)
            throw new IllegalArgumentException("舊公告尚未設定遊戲模式，請重新建立隊伍");
        GameModeView mode = games.findMode(post.getModeId(), false);
        int originalMax = post.getModeMaxPlayers() == null ? post.getMaxPlayers() : post.getModeMaxPlayers();
        if (!Objects.equals(mode.getGameId(), post.getGameId())
                || !Objects.equals(mode.getModeCode(), post.getModeCode())
                || !Objects.equals(mode.getMinPlayers(), post.getMinPlayers())
                || mode.getMaxPlayers() != originalMax
                || post.getMaxPlayers() < mode.getMinPlayers() || post.getMaxPlayers() > mode.getMaxPlayers()
                || !Objects.equals(mode.getComputerPlayers(), post.getComputerPlayers()))
            throw new IllegalArgumentException("遊戲模式設定已變更，請重新建立隊伍");
    }

    public void validateRoom(TeamPost post, Room room) {
        if (!Objects.equals(room.getGameId(), post.getGameId()) || !Objects.equals(room.getModeId(), post.getModeId())
                || !Objects.equals(room.getHostAccount(), post.getCaptain().getAccount())
                || room.getMaxPlayers() != post.getMaxPlayers() || room.getMinPlayers() != post.getMinPlayers()
                || room.getComputerPlayers() != post.getComputerPlayers())
            throw new IllegalArgumentException("房間設定與公告不一致，請先確認遊戲模式及人數");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void start(TeamPost post) {
        Room room = room(post.getRoomId());
        requireWaiting(room);
        validateMode(post);
        validateRoom(post, room);
        List<String> accounts = roster(post).stream().map(Member::getAccount).toList();
        if (accounts.size() != post.getMaxPlayers() || room.getPlayers().size() != accounts.size()
                || !accounts.containsAll(room.getPlayers()))
            throw new IllegalArgumentException("隊伍尚未達到選定人數，或房間名單不一致，無法開始遊戲");
        requireSuccess(lobby.startGame(post.getRoomId(), Map.of("hostAccount", post.getCaptain().getAccount())));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void removePlayer(TeamPost post, String account) {
        if (post.getRoomId() == null) return;
        requireWaiting(room(post.getRoomId()));
        requireSuccess(lobby.kickPlayer(post.getRoomId(), Map.of(
                "hostAccount", post.getCaptain().getAccount(), "targetAccount", account)));
    }

    public void requireWaiting(Room room) {
        if (!"WAITING".equals(room.getStatus())) throw new IllegalArgumentException("遊戲已開始或結束，不能變更隊員");
    }

    public void requireRoom(String roomId) { room(roomId); }

    public Room room(String roomId) {
        Map<String, Object> result = requireSuccess(lobby.getRoomDetail(roomId));
        if (!(result.get("room") instanceof Room room)) throw new IllegalArgumentException("Lobby 未回傳房間資料");
        return room;
    }

    public void notifyRoster(TeamPost post, String title, String message) {
        for (Member member : roster(post)) {
            notices.send(member, post, member.getId().equals(post.getCaptain().getId()) ? "CAPTAIN" : "APPLICANT", title, message, null, null);
        }
    }

    private Map<String, Object> requireSuccess(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null || !Boolean.TRUE.equals(body.get("success")))
            throw new IllegalArgumentException(body != null && body.get("message") != null
                    ? "房間操作失敗：" + body.get("message") : "房間操作失敗，請稍後重試");
        return body;
    }
}
