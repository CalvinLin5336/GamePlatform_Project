package com.example.demo.modules.board.repository;import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.modules.board.entity.Notification;

import java.util.List;public interface NotificationRepository extends JpaRepository<Notification,Long>{List<Notification>findByMemberIdOrderByCreatedAtDesc(Long memberId);}
