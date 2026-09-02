package com.example.demo.modules.board;

import com.example.demo.modules.board.dto.JoinRequestForm;
import com.example.demo.modules.board.dto.TeamPostRequest;
import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.repository.*;
import com.example.demo.modules.board.service.InteractionService;
import com.example.demo.modules.board.service.TeamPostService;
import com.example.demo.modules.game.management.dto.GameView;
import com.example.demo.modules.game.management.service.GameManagementService;
import com.example.demo.modules.lobby.controller.LobbyController;
import com.example.demo.modules.lobby.entity.Room;
import com.example.demo.modules.lobby.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;

/** 使用獨立 SQLite 驗證真正的 Board → Lobby → rooms / room_players 交易。 */
@SpringBootTest(properties = {"app.demo-data.enabled=false", "spring.jpa.show-sql=false"})
class BoardRoomIntegrationTests {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws IOException, java.sql.SQLException {
        String url = "jdbc:sqlite:" + Files.createTempDirectory("board-room-test-").resolve("test.db");
        // 模擬升級前資料庫，驗證 ddl-auto=update 能保留舊公告並補上新欄位。
        try (var connection = java.sql.DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("create table team_posts (id bigint primary key, title varchar(100) not null, game_name varchar(80) not null)");
            statement.execute("insert into team_posts (id, title, game_name) values (999999, '升級前公告', '舊遊戲')");
            statement.execute("create table team_posts_seq (next_val bigint)");
            statement.execute("insert into team_posts_seq values (1000001)");
        }
        properties.add("spring.datasource.url", () -> url);
    }

    @Autowired TeamPostService posts;
    @Autowired InteractionService interactions;
    @Autowired com.example.demo.modules.board.service.BoardTeamService teams;
    @Autowired NotificationRepository notices;
    @Autowired MemberRepository members;
    @Autowired TeamPostRepository postRepository;
    @Autowired JoinRequestRepository joins;
    @Autowired RoomRepository rooms;
    @Autowired GameManagementService games;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean LobbyController lobby;

    Member captain;
    Member player;
    GameView game;

    @BeforeEach
    void setup() {
        captain = member();
        player = member();
        game = games.findEnabledGames().get(0);
    }

    @Test
    void captainChoosesPlayerCountWithinModeAndRankIsOptional() {
        var gameRequest = new com.example.demo.modules.game.management.dto.GameRequest();
        gameRequest.setGameCode("TEST_" + UUID.randomUUID());
        gameRequest.setGameName("支援多人測試遊戲");
        gameRequest.setFrontendPath("/test-game.html");
        gameRequest.setBackendPath("/test-game");
        gameRequest.setEnabled(true);
        GameView flexibleGame = games.createGame(gameRequest);
        var modeRequest = new com.example.demo.modules.game.management.dto.GameModeRequest();
        modeRequest.setModeCode("PLAYER");
        modeRequest.setModeName("二至四人模式");
        modeRequest.setMinPlayers(2);
        modeRequest.setMaxPlayers(4);
        modeRequest.setComputerPlayers(0);
        modeRequest.setEnabled(true);
        var mode = games.createMode(flexibleGame.getGameId(), modeRequest);
        TeamPostRequest request = form("PLAYER");
        request.setGameId(flexibleGame.getGameId());
        request.setModeId(mode.getModeId());
        request.setPlayerCount(3);
        request.setRankRequirement("  ");
        TeamPost post = posts.create(request);
        assertNull(post.getRankRequirement());
        assertEquals(3, post.getMaxPlayers());
        assertEquals(4, post.getModeMaxPlayers());
        JoinRequest first = apply(post, player);
        interactions.review(first.getId(), ApplicationStatus.APPROVED, captain.getId());
        assertNull(posts.get(post.getId()).getRoomId());
        JoinRequest second = apply(post, member());
        interactions.review(second.getId(), ApplicationStatus.APPROVED, captain.getId());
        TeamPost full = posts.get(post.getId());
        assertEquals(3, rooms.findById(full.getRoomId()).orElseThrow().getMaxPlayers());
        assertEquals(3, roster(full.getRoomId()).size());
        request.setPlayerCount(1);
        assertThrows(IllegalArgumentException.class, () -> posts.create(request));
        request.setPlayerCount(5);
        assertThrows(IllegalArgumentException.class, () -> posts.create(request));
        request.setPlayerCount(2);
        assertEquals(2, posts.create(request).getMaxPlayers());
        assertThrows(IllegalArgumentException.class, () -> posts.update(post.getId(), request));
    }

    @Test
    void notificationsReferenceApplicationAndKickingReopensSameRoomForReplacement() {
        TeamPost post = posts.create(form("PLAYER"));
        JoinRequest application = apply(post, player);
        assertTrue(notices.findByMemberIdOrderByCreatedAtDesc(captain.getId()).stream()
                .anyMatch(n -> post.getId().equals(n.getPostId()) && application.getId().equals(n.getApplicationId())));
        interactions.review(application.getId(), ApplicationStatus.APPROVED, captain.getId());
        String roomId = posts.get(post.getId()).getRoomId();
        long roomCount = rooms.count();
        assertThrows(IllegalArgumentException.class, () -> teams.kick(post.getId(), player.getId(), captain.getAccount()));
        teams.kick(post.getId(), captain.getId(), player.getAccount());
        assertEquals(ApplicationStatus.CANCELLED, joins.findById(application.getId()).orElseThrow().getStatus());
        assertEquals(PostStatus.RECRUITING, posts.get(post.getId()).getStatus());
        assertEquals(1, posts.get(post.getId()).getCurrentPlayers());
        assertEquals(List.of(captain.getAccount()), roster(roomId));
        assertThrows(IllegalArgumentException.class, () -> teams.access(post.getId(), player.getId()));
        assertThrows(IllegalArgumentException.class, () -> teams.start(post.getId(), captain.getId()));
        assertThrows(IllegalArgumentException.class, () -> teams.kick(post.getId(), captain.getId(), player.getAccount()));

        Member replacement = member();
        JoinRequest next = apply(post, replacement);
        interactions.review(next.getId(), ApplicationStatus.APPROVED, captain.getId());
        assertEquals(roomCount, rooms.count());
        assertEquals(roomId, posts.get(post.getId()).getRoomId());
        assertEquals(PostStatus.FULL, posts.get(post.getId()).getStatus());
        assertEquals(List.of(captain.getAccount(), replacement.getAccount()), roster(roomId));
    }

    @Test
    void captainStartsAndBoardMembersEnterPokerWithoutUsingOtherModulesUserIds() {
        TeamPost post = posts.create(form("PLAYER"));
        JoinRequest application = apply(post, player);
        interactions.review(application.getId(), ApplicationStatus.APPROVED, captain.getId());
        assertThrows(IllegalArgumentException.class, () -> teams.joinGame(post.getId(), player.getId()));
        assertThrows(IllegalArgumentException.class, () -> teams.start(post.getId(), player.getId()));
        Map<String, String> entry = teams.start(post.getId(), captain.getId());
        assertEquals(PostStatus.STARTING, posts.get(post.getId()).getStatus());
        assertEquals("PLAYING", rooms.findById(entry.get("roomId")).orElseThrow().getStatus());
        assertTrue(entry.get("gameUrl").contains("boardPostId=" + post.getId()));
        assertTrue(entry.get("gameUrl").contains("memberId=" + captain.getId()));
        assertEquals(entry, teams.start(post.getId(), captain.getId()));
        assertThrows(IllegalArgumentException.class, () -> teams.kick(post.getId(), captain.getId(), player.getAccount()));
        assertThrows(IllegalArgumentException.class, () -> teams.joinGame(post.getId(), member().getId()));
        var captainJoin = teams.joinGame(post.getId(), captain.getId());
        var playerJoin = teams.joinGame(post.getId(), player.getId());
        assertNotEquals(captainJoin.getSeat(), playerJoin.getSeat());
        assertEquals("PLAYING", playerJoin.getGame().getStatus());
        assertTrue(teams.access(post.getId(), player.getId()).get("gameUrl").contains("memberId=" + player.getId()));
    }

    @Test
    void failedStartRollsBackBothBoardAndLobby() {
        TeamPost post = posts.create(form("COMPUTER"));
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalArgumentException("測試開始失敗");
        }).when(lobby).startGame(org.mockito.ArgumentMatchers.eq(post.getRoomId()), anyMap());
        assertThrows(IllegalArgumentException.class, () -> teams.start(post.getId(), captain.getId()));
        assertEquals(PostStatus.FULL, posts.get(post.getId()).getStatus());
        assertEquals("WAITING", rooms.findById(post.getRoomId()).orElseThrow().getStatus());
    }

    @Test
    void upgradingExistingDatabaseKeepsOldPostAndAddsNullableRoomAndVersion() {
        TeamPost legacy = posts.get(999999L);
        assertEquals("升級前公告", legacy.getTitle());
        assertEquals(0, legacy.getVersion());
        assertNull(legacy.getRoomId());
        assertNull(legacy.getGameId());
        assertNull(legacy.getModeId());
    }

    @Test
    void computerModeImmediatelyCreatesRoomAndPreservesItOnEdit() {
        long before = rooms.count();
        TeamPost post = posts.create(form("COMPUTER"));
        assertEquals(PostStatus.FULL, post.getStatus());
        assertEquals(1, post.getCurrentPlayers());
        assertEquals(1, post.getMaxPlayers());
        assertEquals(1, post.getComputerPlayers());
        assertEquals(game.getGameName(), post.getGameName());
        assertEquals(List.of(captain.getAccount()), roster(post.getRoomId()));
        assertEquals(post.getRoomId(), posts.roomId(post.getId(), captain.getId()));
        assertThrows(IllegalArgumentException.class, () -> posts.roomId(post.getId(), player.getId()));

        TeamPostRequest edited = form("COMPUTER");
        edited.setTitle("更新說明");
        TeamPost updated = posts.update(post.getId(), edited);
        assertEquals(post.getRoomId(), updated.getRoomId());
        assertEquals(PostStatus.FULL, updated.getStatus());
        assertEquals(before + 1, rooms.count());
    }

    @Test
    void lastApprovalCreatesOneRoomForCaptainAndApprovedPlayer() {
        long before = rooms.count();
        TeamPost post = posts.create(form("PLAYER"));
        assertNull(post.getRoomId());
        assertEquals(PostStatus.RECRUITING, post.getStatus());
        assertEquals(before, rooms.count());
        JoinRequest application = apply(post, player);
        JoinRequest extra = apply(post, member());

        interactions.review(application.getId(), ApplicationStatus.APPROVED, captain.getId());
        TeamPost full = posts.get(post.getId());
        assertEquals(PostStatus.FULL, full.getStatus());
        assertEquals(2, full.getCurrentPlayers());
        assertNotNull(full.getRoomId());
        Room room = rooms.findById(full.getRoomId()).orElseThrow();
        assertEquals(full.getGameId(), room.getGameId());
        assertEquals(full.getModeId(), room.getModeId());
        assertEquals(2, room.getMaxPlayers());
        assertEquals(0, room.getComputerPlayers());
        assertEquals(List.of(captain.getAccount(), player.getAccount()), roster(room.getId()));
        assertEquals(room.getId(), posts.roomId(post.getId(), player.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> interactions.review(application.getId(), ApplicationStatus.APPROVED, captain.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> interactions.review(extra.getId(), ApplicationStatus.APPROVED, captain.getId()));
        assertEquals(before + 1, rooms.count());
        assertEquals(ApplicationStatus.PENDING, joins.findById(extra.getId()).orElseThrow().getStatus());
    }

    @Test
    void lobbyJoinFailureRollsBackRoomApprovalAndCountThenAllowsRetry() {
        TeamPost post = posts.create(form("PLAYER"));
        JoinRequest application = apply(post, player);
        long before = rooms.count();
        doReturn(ResponseEntity.badRequest().body(Map.of("success", false, "message", "測試入房失敗")))
                .doCallRealMethod().when(lobby).joinRoom(anyMap());

        assertThrows(IllegalArgumentException.class,
                () -> interactions.review(application.getId(), ApplicationStatus.APPROVED, captain.getId()));
        assertEquals(before, rooms.count());
        assertEquals(1, posts.get(post.getId()).getCurrentPlayers());
        assertNull(posts.get(post.getId()).getRoomId());
        assertEquals(ApplicationStatus.PENDING, joins.findById(application.getId()).orElseThrow().getStatus());

        interactions.review(application.getId(), ApplicationStatus.APPROVED, captain.getId());
        assertEquals(before + 1, rooms.count());
        assertEquals(2, roster(posts.get(post.getId()).getRoomId()).size());
    }

    @Test
    void rejectsMismatchedOrDisabledModeAndKeepsExistingApplicationsConsistent() {
        TeamPostRequest invalid = form("PLAYER");
        invalid.setModeId(Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> posts.create(invalid));
        var disabled = games.findAllGames().stream().filter(g -> !g.isEnabled()).findFirst().orElseThrow();
        invalid.setModeId(disabled.getModes().get(0).getModeId());
        assertThrows(IllegalArgumentException.class, () -> posts.create(invalid));
        invalid.setGameId(disabled.getGameId());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> posts.create(invalid));

        TeamPost post = posts.create(form("PLAYER"));
        JoinRequest application = apply(post, player);
        assertThrows(IllegalArgumentException.class, () -> posts.update(post.getId(), form("COMPUTER")));
        assertThrows(IllegalArgumentException.class,
                () -> interactions.review(application.getId(), ApplicationStatus.APPROVED, player.getId()));
        interactions.review(application.getId(), ApplicationStatus.REJECTED, captain.getId());
        assertEquals(1, posts.get(post.getId()).getCurrentPlayers());
        assertNull(posts.get(post.getId()).getRoomId());
    }

    @Test
    void gameAndModeFiltersReturnMatchingPostsAndOldPostsCannotCreateWrongRooms() {
        TeamPost multiplayer = posts.create(form("PLAYER"));
        TeamPost computer = posts.create(form("COMPUTER"));
        var filtered = posts.list(captain.getAccount(), null, game.getGameId(), multiplayer.getModeId());
        assertEquals(List.of(multiplayer.getId()), filtered.stream().map(TeamPost::getId).toList());
        assertTrue(posts.list(captain.getAccount(), PostStatus.RECRUITING, game.getGameId(), computer.getModeId()).isEmpty());

        TeamPost legacy = new TeamPost();
        legacy.setTitle("舊公告");
        legacy.setGameName("舊遊戲");
        legacy.setMaxPlayers(4);
        legacy.setCaptain(captain);
        postRepository.save(legacy);
        assertThrows(IllegalArgumentException.class, () -> apply(legacy, player));
    }

    @Test
    void staleConcurrentPostUpdateCannotOverwriteFullTeamOrCreateAnotherRoom() {
        TeamPost post = posts.create(form("PLAYER"));
        TeamPost stale = posts.get(post.getId());
        JoinRequest application = apply(post, player);
        interactions.review(application.getId(), ApplicationStatus.APPROVED, captain.getId());
        String roomId = posts.get(post.getId()).getRoomId();
        stale.setTitle("過時的編輯");
        assertThrows(org.springframework.dao.OptimisticLockingFailureException.class,
                () -> postRepository.saveAndFlush(stale));
        assertEquals(roomId, posts.get(post.getId()).getRoomId());
        assertEquals(2, posts.get(post.getId()).getCurrentPlayers());
    }

    private Member member() {
        Member member = new Member();
        member.setAccount(UUID.randomUUID().toString());
        member.setNickname("測試玩家");
        member.setPassword("test-password");
        member.setEmail(member.getAccount() + "@example.com");
        return members.save(member);
    }

    private TeamPostRequest form(String code) {
        TeamPostRequest request = new TeamPostRequest();
        request.setCaptainId(captain.getId());
        request.setGameId(game.getGameId());
        request.setModeId(game.getModes().stream().filter(m -> code.equals(m.getModeCode())).findFirst().orElseThrow().getModeId());
        request.setTitle(captain.getAccount() + " " + code);
        request.setDescription("測試組隊");
        return request;
    }

    private JoinRequest apply(TeamPost post, Member member) {
        JoinRequestForm request = new JoinRequestForm();
        request.setMemberId(member.getId());
        return interactions.join(post.getId(), request);
    }

    private List<String> roster(String roomId) {
        return new TransactionTemplate(transactions).execute(status ->
                List.copyOf(rooms.findById(roomId).orElseThrow().getPlayers()));
    }
}
