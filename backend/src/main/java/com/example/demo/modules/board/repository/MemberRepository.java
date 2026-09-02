package com.example.demo.modules.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.modules.board.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByPlatformUserId(Long platformUserId);
	Optional<Member> findByAccount(String account);

	boolean existsByAccount(String account);

	boolean existsByEmail(String email);
}
