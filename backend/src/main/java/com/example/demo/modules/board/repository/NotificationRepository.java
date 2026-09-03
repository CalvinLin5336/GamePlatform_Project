package com.example.demo.modules.board.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import com.example.demo.modules.board.entity.Notification;
import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    String FILTER = " from Notification n left join TeamPost p on p.id=n.postId where n.member.id=:memberId and (:category is null or coalesce(n.category, case when p.captain.id=:memberId then 'CAPTAIN' when n.applicationId is not null then 'APPLICANT' else 'WATCHING' end)=:category)";
    List<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    @Query(value="select n" + FILTER + " order by n.createdAt desc, n.id desc", countQuery="select count(n)" + FILTER)
    Page<Notification> page(@Param("memberId") Long memberId, @Param("category") String category, Pageable pageable);
    @Query("select count(n)" + FILTER + " and (n.readFlag=false or n.readFlag is null)")
    long unread(@Param("memberId") Long memberId, @Param("category") String category);
    @Query("select count(n) from Notification n where n.member.id=:memberId and n.commentId is not null and (n.readFlag=false or n.readFlag is null)")
    long unreadComments(@Param("memberId") Long memberId);
}
