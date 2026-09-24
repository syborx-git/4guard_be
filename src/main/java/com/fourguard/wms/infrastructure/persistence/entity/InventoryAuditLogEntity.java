package com.fourguard.wms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity for wms.inventory_audit_log.
 * Granular audit log recording every lifecycle milestone of a pallet (Tree of Life).
 */
@Entity
@Table(
    name = "inventory_audit_log",
    schema = "wms",
    indexes = {
        @Index(name = "idx_inv_audit_org", columnList = "organization_id"),
        @Index(name = "idx_inv_audit_pallet_code", columnList = "pallet_code"),
        @Index(name = "idx_inv_audit_remision", columnList = "remision_folio"),
        @Index(name = "idx_inv_audit_event_type", columnList = "event_type"),
        @Index(name = "idx_inv_audit_performed_at", columnList = "performed_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class InventoryAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pallet_id")
    private WarehouseReceptionPalletEntity pallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItemEntity inventoryItem;

    @Column(name = "pallet_code", nullable = false, length = 60)
    private String palletCode;

    @Column(name = "remision_folio", length = 60)
    private String remisionFolio;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "source_location", length = 100)
    private String sourceLocation;

    @Column(name = "target_location", length = 100)
    private String targetLocation;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private OffsetDateTime performedAt;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (performedAt == null) performedAt = now;
        if (createdAt == null) createdAt = now;
    }
}
