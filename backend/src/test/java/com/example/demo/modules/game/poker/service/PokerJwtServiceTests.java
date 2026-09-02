package com.example.demo.modules.game.poker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.modules.game.poker.exception.GameException;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.security.JwtService;
import com.example.demo.modules.user.service.UserService;

class PokerJwtServiceTests {
    private PokerJwtService service;
    private JwtService jwtService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        service=new PokerJwtService();
        jwtService=mock(JwtService.class);
        userService=mock(UserService.class);
        ReflectionTestUtils.setField(service, "jwtService", jwtService);
        ReflectionTestUtils.setField(service, "userService", userService);
    }

    @Test
    void validJwtLoadsTheUserFromTheBackend() {
        UserResponse user=new UserResponse(8L, "player8", "玩家八", null, null,
                "PLAYER", "Active", null);
        when(jwtService.isTokenValid("valid-jwt")).thenReturn(true);
        when(jwtService.extractUserId("valid-jwt")).thenReturn(8L);
        when(jwtService.extractAccount("valid-jwt")).thenReturn("player8");
        when(userService.findById(8L)).thenReturn(user);

        assertSame(user, service.requireUser("Bearer valid-jwt"));
    }

    @Test
    void missingJwtIsRejected() {
        GameException error=assertThrows(GameException.class,
                () -> service.requireUser(null));
        assertEquals("LOGIN_REQUIRED", error.getCode());
    }

    @Test
    void jwtAccountMustMatchTheDatabaseUser() {
        UserResponse user=new UserResponse(8L, "player8", "玩家八", null, null,
                "PLAYER", "Active", null);
        when(jwtService.isTokenValid("wrong-account")).thenReturn(true);
        when(jwtService.extractUserId("wrong-account")).thenReturn(8L);
        when(jwtService.extractAccount("wrong-account")).thenReturn("other");
        when(userService.findById(8L)).thenReturn(user);

        GameException error=assertThrows(GameException.class,
                () -> service.requireUser("Bearer wrong-account"));
        assertEquals("INVALID_LOGIN", error.getCode());
    }
}
