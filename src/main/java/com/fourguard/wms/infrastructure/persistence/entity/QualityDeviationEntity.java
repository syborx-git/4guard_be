package com.fourguard.wms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(
    name = "quality_deviations",
    schema = "wms",
    indexes = {
        @Index(name = "idx_qm_deviations_org_branch", columnList = "organization_id, branch_id"),
        @Index(name = "idx_qm_deviations_date", columnList = "branch_id, deviation_date"),
        @Index(name = "idx_qm_deviations_sku", columnList = "sku_id"),
        @Index(name = "idx_qm_deviations_ua", columnList = "ua_code"),
        @Index(name = "idx_qm_deviations_root_cause", columnList = "root_cause_motive")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class QualityDeviationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(nullable = false, unique = true, length = 30)
    private String folio;

    @Column(name = "remision_number", nullable = false, length = 100)
    private String remisionNumber;

    @Column(name = "sku_id", nullable = false, length = 50)
    private String skuId;

    @Column(name = "sku_description", length = 255)
    private String skuDescription;

    @Column(name = "ua_code", nullable = false, length = 50)
    private String uaCode;

    @Column(name = "material_type", nullable = false, length = 50)
    private String materialType; // PRODUCTO_TERMINADO, EMBALAJES, CAFE_VERDE, OTRO

    @Column(name = "deviation_date", nullable = false)
    private LocalDate deviationDate;

    @Column(name = "deviation_time", nullable = false)
    private LocalTime deviationTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detected_by_id", nullable = false)
    private UserEntity detectedBy;

    @Column(name = "responsible_collaborator", length = 150)
    private String responsibleCollaborator;

    @Column(name = "bay_location_code", length = 50)
    private String bayLocationCode;

    @Column(name = "damaged_units", nullable = false)
    @Builder.Default
    private Integer damagedUnits = 0;

    @Column(name = "material_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal materialCost = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "MXN";

    @Column(name = "condition_deviation", nullable = false, length = 100)
    private String conditionDeviation;

    @Column(name = "root_cause_motive", nullable = false, length = 100)
    private String rootCauseMotive;

    @Column(name = "origin_area", nullable = false, length = 50)
    private String originArea;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_photo_urls", columnDefinition = "JSONB")
    private String evidencePhotoUrls;

    @Column(name = "action_taken", nullable = false, length = 100)
    private String actionTaken;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "is_resolved", nullable = false)
    @Builder.Default
    private Boolean isResolved = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (updatedAt == null) updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (deviationTime == null) deviationTime = LocalTime.now();
        if (damagedUnits == null) damagedUnits = 0;
        if (materialCost == null) materialCost = BigDecimal.ZERO;
        if (currency == null) currency = "MXN";
        if (isResolved == null) isResolved = false;
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
