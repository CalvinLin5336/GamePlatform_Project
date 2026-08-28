package com.example.demo.modules.user.service;

import com.example.demo.modules.user.dto.OperationLogResponse;
import com.example.demo.modules.user.repository.OperationLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OperationLogService {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    public void log(String account, String action, Long targetId,
                    String role, String description) {
        operationLogRepository.save(
                account, action, targetId, role, description,
                LocalDateTime.now().format(FORMATTER)
        );
    }

    public List<OperationLogResponse> findAll() {
        return operationLogRepository.findAll();
    }
}
