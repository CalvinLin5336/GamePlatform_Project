package com.example.demo.modules.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.modules.board.entity.*;

import java.util.List;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {
	java.util.Optional<JoinRequest> findByPostIdAndApplicantAccountAndStatus(Long postId, String account, ApplicationStatus status);
	boolean existsByPostId(Long postId);

	boolean existsByPostIdAndApplicantIdAndStatus(Long postId, Long memberId, ApplicationStatus status);
	boolean existsByPostIdAndApplicantId(Long postId, Long memberId);

	List<JoinRequest> findByApplicantIdOrderByCreatedAtDesc(Long memberId);

	List<JoinRequest> findByPostCaptainIdOrderByCreatedAtDesc(Long captainId);

	List<JoinRequest> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, ApplicationStatus status);
}
