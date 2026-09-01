package com.example.demo.modules.board.repository;import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.modules.board.entity.Comment;

import java.util.List;public interface CommentRepository extends JpaRepository<Comment,Long>{List<Comment>findByPostIdOrderByCreatedAtAsc(Long postId);}
