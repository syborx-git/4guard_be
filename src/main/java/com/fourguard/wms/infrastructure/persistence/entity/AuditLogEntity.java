package com.fourguard.wms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Audit Log JPA entity — WORM (Write Once, Read Many).
 * Relational model without JSONB fields.
 */
@Entity
@Table(name = "audit_logs", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID logId;

    @Column(name = "organization_id", nullable = false, columnDefinition = "UUID")
    private UUID organizationId;

    @Column(name = "branch_id", columnDefinition = "UUID")
    private UUID branchId;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, columnDefinition = "UUID")
    private UUID entityId;

    @OneToMany(mappedBy = "log", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<AuditLogDetailEntity> details = new ArrayList<>();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "shift_session_id", columnDefinition = "UUID")
    private UUID shiftSessionId;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
