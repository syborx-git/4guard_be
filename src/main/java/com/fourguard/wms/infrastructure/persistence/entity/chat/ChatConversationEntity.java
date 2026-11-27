package com.fourguard.wms.infrastructure.persistence.entity.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "type", nullable = false, length = 30)
    private String type; // DM, SPACE, THREAD

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "icon", length = 60)
    private String icon;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, FROZEN, ARCHIVED

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Column(name = "retention_days")
    private Integer retentionDays;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.status == null || this.status.isBlank()) this.status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
