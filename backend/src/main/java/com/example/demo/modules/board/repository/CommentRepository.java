package com.example.demo.modules.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.modules.board.entity.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
	List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    org.springframework.data.domain.Page<Comment> findByPostIdOrderByCreatedAtAscIdAsc(Long postId, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Query("select distinct c.member from Comment c where c.post.id=:postId")
    List<com.example.demo.modules.board.entity.Member> participants(@org.springframework.data.repository.query.Param("postId") Long postId);
}
