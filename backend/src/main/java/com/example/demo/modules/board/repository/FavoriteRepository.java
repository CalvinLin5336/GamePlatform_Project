package com.example.demo.modules.board.repository;import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.modules.board.entity.Favorite;

import java.util.*;public interface FavoriteRepository extends JpaRepository<Favorite,Long>{Optional<Favorite>findByPostIdAndMemberId(Long postId,Long memberId);List<Favorite>findByMemberId(Long memberId);}
