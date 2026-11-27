package com.fourguard.wms.infrastructure.persistence.repository.chat;

import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatConversationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversationEntity, UUID> {

    @Query("""
            SELECT c FROM ChatConversationEntity c
            JOIN ChatParticipantEntity p ON p.conversationId = c.id
            WHERE c.organizationId = :orgId
              AND p.userId = :userId
              AND p.isActive = true
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC
            """)
    List<ChatConversationEntity> findActiveByOrganizationAndUser(
            @Param("orgId") UUID orgId,
            @Param("userId") UUID userId
    );

    @Query("""
            SELECT c FROM ChatConversationEntity c
            WHERE c.organizationId = :orgId
              AND c.type = 'DM'
              AND EXISTS (SELECT 1 FROM ChatParticipantEntity p1 WHERE p1.conversationId = c.id AND p1.userId = :userA AND p1.isActive = true)
              AND EXISTS (SELECT 1 FROM ChatParticipantEntity p2 WHERE p2.conversationId = c.id AND p2.userId = :userB AND p2.isActive = true)
            """)
    Optional<ChatConversationEntity> findDirectMessageBetween(
            @Param("orgId") UUID orgId,
            @Param("userA") UUID userA,
            @Param("userB") UUID userB
    );

    Optional<ChatConversationEntity> findFirstByOrganizationIdAndEntityTypeAndEntityId(
            UUID organizationId,
            String entityType,
            String entityId
    );

    long countByOrganizationIdAndTypeAndStatus(UUID organizationId, String type, String status);

    long countByOrganizationIdAndType(UUID organizationId, String type);

    @Query("""
            SELECT c FROM ChatConversationEntity c
            WHERE c.organizationId = :orgId
              AND c.type = 'SPACE'
              AND (:status = '' OR UPPER(c.status) = :status)
              AND (:searchPattern = ''
                   OR LOWER(COALESCE(c.name, '')) LIKE :searchPattern
                   OR LOWER(COALESCE(c.description, '')) LIKE :searchPattern
                   OR LOWER(COALESCE(c.category, '')) LIKE :searchPattern)
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC
            """)
    Page<ChatConversationEntity> findGovernanceSpaces(
            @Param("orgId") UUID orgId,
            @Param("status") String status,
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );
}
