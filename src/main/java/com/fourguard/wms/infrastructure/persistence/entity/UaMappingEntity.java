package com.fourguard.wms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * JPA Entity for wms.ua_mappings.
 * Immutable audit mapping between original supplier UA and internal 4Guard SSCC GS1-128.
 */
@Entity
@Table(
    name = "ua_mappings",
    schema = "wms",
    indexes = {
        @Index(name = "idx_ua_mappings_reception", columnList = "reception_id"),
        @Index(name = "idx_ua_mappings_pallet", columnList = "pallet_id"),
        @Index(name = "idx_ua_mappings_internal_ua", columnList = "internal_ua_code"),
        @Index(name = "idx_ua_mappings_supplier_ua", columnList = "supplier_ua_code")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UaMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reception_id", nullable = false)
    private WarehouseReceptionEntity reception;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pallet_id", nullable = false)
    private WarehouseReceptionPalletEntity pallet;

    @Column(name = "supplier_ua_code", nullable = false, length = 60)
    private String supplierUaCode;

    @Column(name = "internal_ua_code", nullable = false, length = 60)
    private String internalUaCode;

    @Column(name = "relabelled_by", nullable = false, length = 100)
    private String relabelledBy;

    @Column(name = "relabelled_at", nullable = false)
    private OffsetDateTime relabelledAt;

    @Column(length = 255)
    @Builder.Default
    private String reason = "Re-etiquetado selectivo a estándar 4Guard SSCC GS1-128";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (relabelledAt == null) relabelledAt = now;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
