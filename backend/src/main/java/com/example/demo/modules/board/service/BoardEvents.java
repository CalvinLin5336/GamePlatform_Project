package com.example.demo.modules.board.service;
import com.example.demo.modules.board.server.BoardWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class BoardEvents {
    private final BoardWebSocketHandler sockets;
    public void postChanged(Long postId) { publish(null, Map.of("type", "POST_CHANGED", "postId", postId)); }
    public void commentsChanged(Long postId) { publish(null, Map.of("type", "COMMENTS_CHANGED", "postId", postId)); }
    public void publish(Long memberId, Map<String, Object> event) {
        Runnable send = () -> sockets.publish(memberId, event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { send.run(); }
            });
        } else send.run();
    }
}
