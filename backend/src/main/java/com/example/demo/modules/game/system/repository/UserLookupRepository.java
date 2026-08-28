package com.example.demo.modules.game.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.shared.entity.User;

public interface UserLookupRepository extends JpaRepository<User, Long> {
}
