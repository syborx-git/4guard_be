package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.CurrencyAuditAction;
import com.fourguard.wms.domain.enums.CurrencyAuditEntityType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "currency_exchange_audit", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyExchangeAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "organization_id", nullable = false, columnDefinition = "UUID")
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 50, nullable = false)
    private CurrencyAuditEntityType entityType;

    @Column(name = "entity_id", nullable = false, columnDefinition = "UUID")
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 50, nullable = false)
    private CurrencyAuditAction action;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_value", columnDefinition = "JSONB")
    private String previousValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "JSONB")
    private String newValue;

    @Column(name = "performed_by", length = 100, nullable = false)
    private String performedBy;

    @Column(name = "performed_at", updatable = false)
    private OffsetDateTime performedAt;

    @PrePersist
    protected void onCreate() {
        if (performedAt == null) performedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
