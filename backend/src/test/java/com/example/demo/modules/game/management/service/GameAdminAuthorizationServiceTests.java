package com.example.demo.modules.game.management.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.service.LoginSessionService;

class GameAdminAuthorizationServiceTests {
    @Test
    void activeAdminCanManageGames() {
        LoginSessionService sessions = mock(LoginSessionService.class);
        Authentication authentication = mock(Authentication.class);
        when(sessions.requireUserFromAuthentication(authentication))
                .thenReturn(user("admin", "ADMIN", "Active"));
        GameAdminAuthorizationService service = new GameAdminAuthorizationService(sessions);

        assertDoesNotThrow(() -> service.requireAdmin(authentication));
    }

    @Test
    void playerCannotManageGames() {
        LoginSessionService sessions = mock(LoginSessionService.class);
        Authentication authentication = mock(Authentication.class);
        when(sessions.requireUserFromAuthentication(authentication))
                .thenReturn(user("player1", "PLAYER", "Active"));
        GameAdminAuthorizationService service = new GameAdminAuthorizationService(sessions);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.requireAdmin(authentication));

        assertEquals(403, error.getStatusCode().value());
    }

    private UserResponse user(String account, String role, String status) {
        return new UserResponse(1L, account, account, null, null, role, status, null);
    }
}
