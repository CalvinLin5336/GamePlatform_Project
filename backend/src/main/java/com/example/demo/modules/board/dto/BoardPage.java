package com.example.demo.modules.board.dto;
import org.springframework.data.domain.Page;
import java.util.List;
public record BoardPage<T>(List<T> content, int page, int totalPages, long totalElements) {
    public static <T> BoardPage<T> from(Page<T> page) {
        return new BoardPage<>(page.getContent(), page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }
}
