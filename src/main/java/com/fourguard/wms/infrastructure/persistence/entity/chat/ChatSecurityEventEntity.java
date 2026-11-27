package com.fourguard.wms.infrastructure.persistence.entity.chat;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_security_events", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSecurityEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private String severity = "MEDIUM";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.severity == null || this.severity.isBlank()) this.severity = "MEDIUM";
    }
}
