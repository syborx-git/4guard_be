package com.fourguard.wms.infrastructure.persistence.repository.chat;

import com.fourguard.wms.application.dto.chat.AttachmentMetadataRow;
import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatAttachmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachmentEntity, UUID> {

    List<ChatAttachmentEntity> findByMessageIdIn(List<UUID> messageIds);

    @Query("""
            SELECT COUNT(a.id)
            FROM ChatAttachmentEntity a
            JOIN ChatMessageEntity m ON a.messageId = m.id
            JOIN ChatConversationEntity c ON m.conversationId = c.id
            WHERE c.organizationId = :orgId
            """)
    long countByOrganizationId(@Param("orgId") UUID orgId);

    @Query("""
            SELECT COALESCE(SUM(a.fileSize), 0)
            FROM ChatAttachmentEntity a
            JOIN ChatMessageEntity m ON a.messageId = m.id
            JOIN ChatConversationEntity c ON m.conversationId = c.id
            WHERE c.organizationId = :orgId
            """)
    long sumFileSizeByOrganizationId(@Param("orgId") UUID orgId);

    @Query("""
            SELECT new com.fourguard.wms.application.dto.chat.AttachmentMetadataRow(
                a.id,
                a.messageId,
                m.conversationId,
                c.name,
                c.type,
                m.senderId,
                a.fileName,
                a.contentType,
                a.fileSize,
                a.createdAt
            )
            FROM ChatAttachmentEntity a
            JOIN ChatMessageEntity m ON a.messageId = m.id
            JOIN ChatConversationEntity c ON m.conversationId = c.id
            WHERE c.organizationId = :orgId
              AND a.createdAt >= :since
            ORDER BY a.createdAt DESC
            """)
    Page<AttachmentMetadataRow> findMetadataByOrganizationIdAndCreatedAtAfter(
            @Param("orgId") UUID orgId,
            @Param("since") Instant since,
            Pageable pageable
    );

    @Query("""
            SELECT a.messageId, COUNT(a.id)
            FROM ChatAttachmentEntity a
            WHERE a.messageId IN :messageIds
            GROUP BY a.messageId
            """)
    List<Object[]> countByMessageIdIn(@Param("messageIds") List<UUID> messageIds);
}
