package com.example.demo.modules.board;
import com.example.demo.modules.board.entity.Member;
import com.example.demo.modules.board.server.BoardWebSocketHandler;
import com.example.demo.modules.board.service.BoardSessionService;
import com.example.demo.modules.user.service.LoginSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BoardWebSocketTests {
    WebSocketSession socket(String id) {
        WebSocketSession socket=mock(WebSocketSession.class);when(socket.getId()).thenReturn(id);when(socket.isOpen()).thenReturn(true);return socket;
    }
    Member member(long id){Member m=new Member();m.setId(id);return m;}
    @Test void privateEventsReachOnlyAuthenticatedRecipientWhilePublicChangesReachGuests() throws Exception {
        var sessions=mock(BoardSessionService.class);var logins=mock(LoginSessionService.class);
        var handler=new BoardWebSocketHandler(sessions,logins);
        var captain=socket("captain");var other=socket("other");var guest=socket("guest");
        when(sessions.requireMember("Bearer captain")).thenReturn(member(1));when(sessions.requireMember("Bearer other")).thenReturn(member(2));
        for(var socket:new WebSocketSession[]{captain,other,guest})handler.afterConnectionEstablished(socket);
        handler.handleMessage(captain,new TextMessage("{\"type\":\"AUTH\",\"token\":\"captain\"}"));
        handler.handleMessage(other,new TextMessage("{\"type\":\"AUTH\",\"token\":\"other\"}"));
        handler.handleMessage(guest,new TextMessage("{\"type\":\"SUBSCRIBE\"}"));
        clearInvocations(captain,other,guest);
        handler.publish(1L,Map.of("type","NOTIFICATION","message","私人留言"));
        verify(captain).sendMessage(argThat(m->m.getPayload().toString().contains("私人留言")));
        verify(other,never()).sendMessage(any());verify(guest,never()).sendMessage(any());
        handler.publish(null,Map.of("type","POST_CHANGED","postId",42));
        verify(guest).sendMessage(argThat(m->m.getPayload().toString().contains("POST_CHANGED")));
    }
    @Test void invalidTokenCannotSubscribeToPrivateEvents() throws Exception {
        var sessions=mock(BoardSessionService.class);var handler=new BoardWebSocketHandler(sessions,mock(LoginSessionService.class));var socket=socket("invalid");
        when(sessions.requireMember(anyString())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        handler.afterConnectionEstablished(socket);handler.handleMessage(socket,new TextMessage("{\"type\":\"AUTH\",\"token\":\"invalid\",\"memberId\":1}"));
        handler.publish(1L,Map.of("type","NOTIFICATION"));
        verify(socket).close(CloseStatus.POLICY_VIOLATION);verify(socket,never()).sendMessage(any());
    }
    @Test void disabledOrExpiredUserIsDisconnectedBeforeReceivingAnotherEvent() throws Exception {
        var sessions=mock(BoardSessionService.class);var logins=mock(LoginSessionService.class);var handler=new BoardWebSocketHandler(sessions,logins);var socket=socket("disabled");
        when(sessions.requireMember("Bearer valid")).thenReturn(member(1));
        handler.afterConnectionEstablished(socket);handler.handleMessage(socket,new TextMessage("{\"type\":\"AUTH\",\"token\":\"valid\"}"));
        clearInvocations(socket);when(logins.requireUser("Bearer valid")).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        handler.publish(1L,Map.of("type","NOTIFICATION"));
        verify(socket).close(CloseStatus.POLICY_VIOLATION);verify(socket,never()).sendMessage(any());
    }
}
