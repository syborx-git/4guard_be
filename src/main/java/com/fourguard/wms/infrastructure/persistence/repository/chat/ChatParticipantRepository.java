package com.fourguard.wms.infrastructure.persistence.repository.chat;

import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipantEntity, UUID> {

    List<ChatParticipantEntity> findByConversationIdAndIsActiveTrue(UUID conversationId);

    Optional<ChatParticipantEntity> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    boolean existsByConversationIdAndUserIdAndIsActiveTrue(UUID conversationId, UUID userId);

    List<ChatParticipantEntity> findByConversationIdInAndIsActiveTrue(List<UUID> conversationIds);

    @Query("""
            SELECT p.conversationId, COUNT(p.id)
            FROM ChatParticipantEntity p
            WHERE p.conversationId IN :conversationIds
              AND p.isActive = true
            GROUP BY p.conversationId
            """)
    List<Object[]> countActiveByConversationIds(@Param("conversationIds") List<UUID> conversationIds);

    @Query("""
            SELECT p.userId, COUNT(p.id)
            FROM ChatParticipantEntity p
            JOIN ChatConversationEntity c ON p.conversationId = c.id
            WHERE p.userId IN :userIds
              AND p.isActive = true
              AND c.type = 'SPACE'
            GROUP BY p.userId
            """)
    List<Object[]> countActiveSpacesByUserIds(@Param("userIds") List<UUID> userIds);
}
