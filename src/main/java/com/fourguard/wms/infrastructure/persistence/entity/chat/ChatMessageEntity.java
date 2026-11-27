package com.fourguard.wms.infrastructure.persistence.entity.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "parent_message_id")
    private UUID parentMessageId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "message_type", nullable = false, length = 30)
    @Builder.Default
    private String messageType = "TEXT"; // TEXT, SYSTEM, ENTITY_REF

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private Boolean isEdited = false;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @Column(name = "pinned_by")
    private UUID pinnedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.isEdited == null) this.isEdited = false;
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.isPinned == null) this.isPinned = false;
    }
}
