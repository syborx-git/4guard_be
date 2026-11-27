package com.fourguard.wms.infrastructure.persistence.repository.chat;

import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatMessageEntity;
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
}
