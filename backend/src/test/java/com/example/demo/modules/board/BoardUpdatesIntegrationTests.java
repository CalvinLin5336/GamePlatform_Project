package com.example.demo.modules.board;

import com.example.demo.modules.board.dto.*;
import com.example.demo.modules.board.entity.*;
import com.example.demo.modules.board.repository.*;
import com.example.demo.modules.board.server.BoardWebSocketHandler;
import com.example.demo.modules.board.service.*;
import com.example.demo.modules.game.management.service.GameManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Files;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties="spring.jpa.show-sql=false")
class BoardUpdatesIntegrationTests {
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) throws Exception {
        String url="jdbc:sqlite:"+Files.createTempDirectory("board-updates-").resolve("test.db");
        properties.add("spring.datasource.url",()->url);
    }
    @Autowired TeamPostService posts;
    @Autowired TeamPostRepository postRepository;
    @Autowired MemberRepository members;
    @Autowired CommentRepository comments;
    @Autowired NotificationRepository notices;
    @Autowired InteractionService interactions;
    @Autowired BoardNotificationService notifications;
    @Autowired GameManagementService games;
    @Autowired Clock boardClock;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean BoardWebSocketHandler sockets;

    Member member() {
        Member m=new Member();m.setAccount("m"+UUID.randomUUID());m.setNickname("測試會員");m.setEmail(UUID.randomUUID()+"@example.invalid");m.setPassword("test-only");return members.save(m);
    }
    TeamPostRequest form(Member captain,String title,LocalDateTime start) {
        var game=games.findEnabledGames().stream().filter(g->"POKER".equals(g.getGameCode())).findFirst().orElseThrow();
        var mode=game.getModes().stream().filter(m->"PLAYER".equals(m.getModeCode())).findFirst().orElseThrow();
        TeamPostRequest f=new TeamPostRequest();f.setCaptainId(captain.getId());f.setGameId(game.getGameId());f.setModeId(mode.getModeId());f.setPlayerCount(2);f.setTitle(title);f.setDescription("測試公告");f.setStartTime(start);return f;
    }
    LocalDateTime future(){return LocalDateTime.now(boardClock).plusDays(2).withSecond(0).withNano(0);}
    Comment comment(TeamPost p, Member m, String text) {CommentRequest f=new CommentRequest();f.setMemberId(m.getId());f.setContent(text);return interactions.addComment(p.getId(),f);}

    @Test void preventsPastDatesAndBadRangesWhileAllowingUnchangedHistoricalStart() {
        Member captain=member();TeamPostRequest f=form(captain,"時間驗證",future());
        f.setStartTime(LocalDateTime.now(boardClock).minusMinutes(1));
        assertThrows(IllegalArgumentException.class,()->posts.create(f));
        f.setStartTime(null);assertThrows(IllegalArgumentException.class,()->posts.create(f));
        f.setStartTime(future());f.setEndTime(future().minusHours(1));assertThrows(IllegalArgumentException.class,()->posts.create(f));
        f.setEndTime(null);TeamPost p=posts.create(f);
        LocalDateTime old=LocalDateTime.now(boardClock).minusDays(1).withNano(0);p.setStartTime(old);postRepository.saveAndFlush(p);
        f.setStartTime(old);f.setTitle("只修改說明");assertEquals(old,posts.update(p.getId(),f).getStartTime());
        f.setStartTime(old.plusHours(1));assertThrows(IllegalArgumentException.class,()->posts.update(p.getId(),f));
    }

    @Test void filtersStartTimeAndPaginatesPostsWithoutDuplicates() {
        Member captain=member();String keyword="page"+UUID.randomUUID();LocalDateTime start=future();
        for(int i=0;i<23;i++)posts.create(form(captain,keyword+" "+i,start.plusMinutes(i)));
        var first=posts.page(keyword,null,null,null,null,null,0);
        var second=posts.page(keyword,null,null,null,null,null,1);
        var third=posts.page(keyword,null,null,null,null,null,2);
        assertEquals(23,first.totalElements());assertEquals(3,first.totalPages());assertEquals(10,first.content().size());assertEquals(10,second.content().size());assertEquals(3,third.content().size());
        Set<Long> ids=new HashSet<>();for(var page:List.of(first,second,third))page.content().forEach(p->assertTrue(ids.add(p.getId())));
        var filtered=posts.page(keyword,null,null,null,start.plusMinutes(5),start.plusMinutes(9),0);
        assertEquals(5,filtered.totalElements());
        assertThrows(IllegalArgumentException.class,()->posts.page(keyword,null,null,null,start,start.minusMinutes(1),0));
        assertThrows(IllegalArgumentException.class,()->posts.page(keyword,null,null,null,null,null,-1));
    }

    @Test void commentsNotifyCaptainApplicantsAndWatchersOnceAndPaginateByTen() {
        Member captain=member(), applicant=member(), watcher=member(), author=member();
        TeamPost post=posts.create(form(captain,"留言通知",future()));
        JoinRequestForm join=new JoinRequestForm();join.setMemberId(applicant.getId());join.setMessage("想加入");interactions.join(post.getId(),join);
        interactions.toggleFavorite(post.getId(),watcher.getId());
        // 同時留言和收藏也只會收到一份通知。
        comment(post,watcher,"關注這個遊戲");
        for(int i=0;i<11;i++)comment(post,author,"留言 "+i);
        var first=interactions.commentPage(post.getId(),0);var second=interactions.commentPage(post.getId(),1);
        assertEquals(12,first.totalElements());assertEquals(10,first.content().size());assertEquals(2,second.content().size());
        assertTrue(first.content().get(9).getId()<second.content().get(0).getId());
        assertEquals(12,notices.unreadComments(captain.getId()));assertEquals(12,notices.unreadComments(applicant.getId()));assertEquals(11,notices.unreadComments(watcher.getId()));assertEquals(0,notices.unreadComments(author.getId()));
        assertEquals(12,notifications.page(captain.getId(),NotificationCategory.CAPTAIN,0).totalElements()-1); // 另有一則加入申請
        assertEquals(12,notifications.page(applicant.getId(),NotificationCategory.APPLICANT,0).totalElements());
        assertEquals(11,notifications.page(watcher.getId(),NotificationCategory.WATCHING,0).totalElements());
        long watcherNotice=notifications.page(watcher.getId(),NotificationCategory.WATCHING,0).content().get(0).id();
        notifications.markRead(author.getId(),List.of(watcherNotice));assertEquals(11,notices.unreadComments(watcher.getId()));
        notifications.markRead(watcher.getId(),List.of(watcherNotice));assertEquals(10,notices.unreadComments(watcher.getId()));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->interactions.deleteComment(first.content().get(0).getId(),author.getId()));
        interactions.deleteComment(first.content().get(0).getId(),watcher.getId());assertEquals(11,interactions.commentPage(post.getId(),0).totalElements());
    }

    @Test void legacyNoticesRemainClassifiedAndReadStatePersists() {
        Member captain=member();TeamPost post=posts.create(form(captain,"舊通知",future()));
        Notification n=new Notification();n.setMember(captain);n.setPostId(post.getId());n.setTitle("舊版通知");n.setReadFlag(null);notices.save(n);
        var page=notifications.page(captain.getId(),NotificationCategory.CAPTAIN,0);
        assertEquals(1,page.totalElements());assertEquals("CAPTAIN",page.content().get(0).category());
        assertEquals(1,notices.unread(captain.getId(),"CAPTAIN"));
        notifications.markRead(captain.getId(),List.of(n.getId()));assertEquals(0,notices.unread(captain.getId(),null));
        assertTrue(notices.findById(n.getId()).orElseThrow().getReadFlag());
    }

    @Test void rollbackDoesNotPublishCommentsOrNotifications() {
        Member captain=member(),author=member();TeamPost post=posts.create(form(captain,"交易驗證",future()));
        clearInvocations(sockets);
        TransactionTemplate tx=new TransactionTemplate(transactions);
        tx.executeWithoutResult(status->{comment(post,author,"回滾訊息");status.setRollbackOnly();});
        assertEquals(0,interactions.commentPage(post.getId(),0).totalElements());assertEquals(0,notices.unread(captain.getId(),null));
        verify(sockets,never()).publish(any(),anyMap());
        comment(post,author,"確認提交");
        verify(sockets).publish(eq(captain.getId()),argThat(e->"NOTIFICATION".equals(e.get("type"))));
        verify(sockets).publish(isNull(),argThat(e->"COMMENTS_CHANGED".equals(e.get("type"))));
    }
}
