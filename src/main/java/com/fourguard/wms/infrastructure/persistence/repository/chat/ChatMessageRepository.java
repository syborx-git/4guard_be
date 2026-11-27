package com.fourguard.wms.infrastructure.persistence.repository.chat;

import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

    List<ChatMessageEntity> findByConversationIdAndParentMessageIdIsNullOrderByCreatedAtDesc(
            UUID conversationId,
            Pageable pageable
    );

    List<ChatMessageEntity> findByConversationIdAndParentMessageIdIsNullAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID conversationId,
            Instant before,
            Pageable pageable
    );

    List<ChatMessageEntity> findByConversationIdOrderByCreatedAtDesc(
            UUID conversationId,
            Pageable pageable
    );

    List<ChatMessageEntity> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID conversationId,
            Instant before,
            Pageable pageable
    );

    List<ChatMessageEntity> findByConversationIdAndIsPinnedTrueAndIsDeletedFalseOrderByPinnedAtDesc(
            UUID conversationId
    );

    List<ChatMessageEntity> findByConversationIdAndIsDeletedFalseAndContentContainingIgnoreCaseOrderByCreatedAtDesc(
            UUID conversationId,
            String query,
            Pageable pageable
    );

    @Query("SELECT m FROM ChatMessageEntity m WHERE m.conversationId = :convId AND m.isDeleted = false AND EXISTS (SELECT 1 FROM ChatAttachmentEntity a WHERE a.messageId = m.id) ORDER BY m.createdAt DESC")
    List<ChatMessageEntity> findEvidenceMessages(@Param("convId") UUID convId, Pageable pageable);

    List<ChatMessageEntity> findByParentMessageIdOrderByCreatedAtAsc(UUID parentMessageId);

    Optional<ChatMessageEntity> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    long countByConversationIdAndCreatedAtAfterAndSenderIdNot(
            UUID conversationId,
            Instant after,
            UUID excludeSenderId
    );

    @Query("""
            SELECT COUNT(m.id)
            FROM ChatMessageEntity m
            JOIN ChatConversationEntity c ON m.conversationId = c.id
            WHERE c.organizationId = :orgId
              AND m.createdAt >= :since
            """)
    long countByOrganizationAndCreatedAtAfter(@Param("orgId") UUID orgId, @Param("since") Instant since);

    @Query("""
            SELECT COUNT(m.id)
            FROM ChatMessageEntity m
            JOIN ChatConversationEntity c ON m.conversationId = c.id
            WHERE c.organizationId = :orgId
              AND m.isEdited = true
              AND COALESCE(m.editedAt, m.createdAt) >= :since
            """)
    long countEditedByOrganizationAndCreatedAtAfter(@Param("orgId") UUID orgId, @Param("since") Instant since);

    @Query("""
            SELECT COUNT(m.id)
            FROM ChatMessageEntity m
            JOIN ChatConversationEntity c ON m.conversationId = c.id
            WHERE c.organizationId = :orgId
              AND m.isDeleted = true
              AND COALESCE(m.deletedAt, m.createdAt) >= :since
            """)
    long countDeletedByOrganizationAndCreatedAtAfter(@Param("orgId") UUID orgId, @Param("since") Instant since);

    @Query("""
            SELECT m.conversationId, COUNT(m.id)
            FROM ChatMessageEntity m
            WHERE m.conversationId IN :conversationIds
            GROUP BY m.conversationId
            """)
    List<Object[]> countByConversationIds(@Param("conversationIds") List<UUID> conversationIds);

    @Query("""
            SELECT m FROM ChatMessageEntity m
            JOIN ChatConversationEntity c ON m.conversationId = c.id
            WHERE c.organizationId = :orgId
              AND (
                  (:type = 'EDITED' AND m.isEdited = true)
                  OR (:type = 'DELETED' AND m.isDeleted = true)
                  OR (:type = 'ALL' AND (m.isEdited = true OR m.isDeleted = true))
              )
              AND COALESCE(m.deletedAt, m.editedAt, m.createdAt) >= :since
            ORDER BY COALESCE(m.deletedAt, m.editedAt, m.createdAt) DESC
            """)
    Page<ChatMessageEntity> findGovernanceAuditMessages(
            @Param("orgId") UUID orgId,
            @Param("type") String type,
            @Param("since") Instant since,
            Pageable pageable
    );
}
