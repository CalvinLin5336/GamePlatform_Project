package com.example.demo.modules.board.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.example.demo.modules.board.entity.*;

import java.util.List;

public interface TeamPostRepository extends JpaRepository<TeamPost, Long> {
	@Query("select p from TeamPost p where (:keyword='' or lower(p.title) like lower(concat('%',:keyword,'%')) or lower(p.gameName) like lower(concat('%',:keyword,'%')) or lower(p.modeName) like lower(concat('%',:keyword,'%'))) and (:status is null or p.status=:status) and (:gameId is null or p.gameId=:gameId) and (:modeId is null or p.modeId=:modeId) order by p.createdAt desc")
	List<TeamPost> search(@Param("keyword") String keyword, @Param("status") PostStatus status,
			@Param("gameId") Long gameId, @Param("modeId") Long modeId);

	List<TeamPost> findByCaptainIdOrderByCreatedAtDesc(Long memberId);
    @Query("select p from TeamPost p where (:keyword='' or lower(p.title) like lower(concat('%',:keyword,'%')) or lower(p.gameName) like lower(concat('%',:keyword,'%')) or lower(p.modeName) like lower(concat('%',:keyword,'%'))) and (:status is null or p.status=:status) and (:gameId is null or p.gameId=:gameId) and (:modeId is null or p.modeId=:modeId) and (:startFrom is null or p.startTime>=:startFrom) and (:startTo is null or p.startTime<=:startTo) order by p.createdAt desc, p.id desc")
    org.springframework.data.domain.Page<TeamPost> searchPage(@Param("keyword") String keyword, @Param("status") PostStatus status,
            @Param("gameId") Long gameId, @Param("modeId") Long modeId,
            @Param("startFrom") java.time.LocalDateTime startFrom, @Param("startTo") java.time.LocalDateTime startTo,
            org.springframework.data.domain.Pageable pageable);
}
