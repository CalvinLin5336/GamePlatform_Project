package com.example.demo.modules.board;

import com.example.demo.modules.board.entity.Member;
import com.example.demo.modules.board.repository.MemberRepository;
import com.example.demo.modules.board.service.BoardSessionService;
import com.example.demo.modules.user.dto.UserRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.security.JwtService;
import com.example.demo.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
class BoardSessionIntegrationTests {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        String url = "jdbc:sqlite:" + Files.createTempDirectory("board-session-test-").resolve("test.db");
        properties.add("spring.datasource.url", () -> url);
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @Autowired UserService users;
    @Autowired JwtService jwt;
    @Autowired MemberRepository members;
    @Autowired BoardSessionService sessions;
    MockMvc mvc;

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build(); }

    UserResponse user() {
        return users.create(new UserRequest("u" + UUID.randomUUID().toString().replace("-", ""), "SessionTest123", "平台會員", null, null, "PLAYER", "Active"), "TEST");
    }

    String bearer(UserResponse user) { return "Bearer " + jwt.generateToken(user.id(), user.account(), user.role()); }

    Member legacy(String account) {
        Member member = new Member();
        member.setAccount(account);
        member.setNickname("舊 Board 會員");
        member.setEmail(UUID.randomUUID() + "@example.invalid");
        member.setPassword("test-only");
        return members.save(member);
    }

    @Test void verifiesSessionAndMapsSeparateMemberIdsWithoutDuplicatingProfile() throws Exception {
        var user = user();
        // 先建立不同來源的資料，確保測試不依賴兩張表的編號碰巧相同。
        legacy("legacy-" + UUID.randomUUID());
        mvc.perform(get("/api/user/auth/me").header("Authorization", bearer(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("id").value(user.id()))
                .andExpect(jsonPath("username").value("平台會員"));
        mvc.perform(post("/board/auth/session").header("Authorization", bearer(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("platformUserId").value(user.id()))
                .andExpect(jsonPath("password").doesNotExist());
        Member member = sessions.currentMember(bearer(user));
        assertNotEquals(user.id(), member.getId());
        assertEquals(member.getId(), sessions.currentMember(bearer(user)).getId());
        assertEquals(user.account(), member.getAccount());
    }

    @Test void missingInvalidAndMismatchedTokensDoNotCreateBoardMembers() throws Exception {
        var user = user();
        long count = members.count();
        mvc.perform(get("/api/user/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/board/auth/session")).andExpect(status().isUnauthorized());
        mvc.perform(post("/board/auth/session").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/board/auth/session").header("Authorization", "Bearer " + jwt.generateToken(user.id(), "another-account", "PLAYER")))
                .andExpect(status().isUnauthorized());
        assertEquals(count, members.count());
    }

    @Test void disabledOrDeletedAccountCannotReusePreviouslyValidToken() throws Exception {
        var user = user();
        String token = bearer(user);
        users.update(user.id(), new UserRequest(user.account(), null, user.username(), null, null, "PLAYER", "Disabled"), "TEST");
        mvc.perform(get("/api/user/auth/me").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(post("/board/auth/session").header("Authorization", token)).andExpect(status().isForbidden());
        users.delete(user.id(), "TEST");
        mvc.perform(get("/api/user/auth/me").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test void matchingAccountNameDoesNotTakeOverLegacyBoardMembership() throws Exception {
        var user = user();
        Member old = legacy(user.account());
        mvc.perform(post("/board/auth/session").header("Authorization", bearer(user)))
                .andExpect(status().isConflict());
        assertNull(members.findById(old.getId()).orElseThrow().getPlatformUserId());
    }

    @Test void profileUpdateKeepsBoardMembershipAndOldTokenIsRejected() throws Exception {
        var user = user();
        String oldToken = bearer(user);
        Member before = sessions.currentMember(oldToken);
        var updated = users.update(user.id(), new UserRequest("renamed" + UUID.randomUUID().toString().replace("-", ""), null, "新暱稱", null, null, "PLAYER", "Active"), "TEST");
        mvc.perform(get("/api/user/auth/me").header("Authorization", oldToken)).andExpect(status().isUnauthorized());
        Member after = sessions.currentMember(bearer(updated));
        assertEquals(before.getId(), after.getId());
        assertEquals("新暱稱", after.getNickname());
    }
}
