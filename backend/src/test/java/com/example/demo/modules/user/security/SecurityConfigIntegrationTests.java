package com.example.demo.modules.user.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;

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

@SpringBootTest(properties = "spring.jpa.show-sql=false")
class SecurityConfigIntegrationTests {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        String url = "jdbc:sqlite:" + Files.createTempDirectory("security-config-test-").resolve("test.db");
        properties.add("spring.datasource.url", () -> url);
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @Autowired JwtService jwt;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    @Test
    void staticPagesRemainPublic() throws Exception {
        mvc.perform(get("/pages/Chat/chatclient.html"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedApisRequireAuthentication() throws Exception {
        mvc.perform(get("/api/user/auth/me"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/lobby/rooms"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/board/auth/session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void playerCannotAccessAdminApis() throws Exception {
        String token = jwt.generateToken(999999L, "security-test-player", "PLAYER");
        mvc.perform(get("/api/user/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void removedChatTestLoginIsNotExposed() throws Exception {
        String token = jwt.generateToken(999999L, "security-test-player", "PLAYER");
        mvc.perform(post("/api/auth")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
