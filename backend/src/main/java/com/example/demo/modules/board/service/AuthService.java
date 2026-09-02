package com.example.demo.modules.board.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.modules.board.dto.*;
import com.example.demo.modules.board.entity.Member;
import com.example.demo.modules.board.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final MemberRepository repo;
	private final PasswordEncoder encoder;

	public Member login(LoginRequest f) {
		return repo.findByAccount(f.getAccount()).filter(m -> m.getPlatformUserId() == null)
				.filter(m -> encoder.matches(f.getPassword(), m.getPassword()))
				.orElseThrow(() -> new IllegalArgumentException("帳號或密碼錯誤"));
	}

	public Member register(RegisterRequest f) {
		if (repo.existsByAccount(f.getAccount()))
			throw new IllegalArgumentException("帳號已存在");
		if (repo.existsByEmail(f.getEmail()))
			throw new IllegalArgumentException("Email 已存在");
		Member m = new Member();
		m.setAccount(f.getAccount());
		m.setPassword(encoder.encode(f.getPassword()));
		m.setNickname(f.getNickname());
		m.setEmail(f.getEmail());
		return repo.save(m);
	}
}
