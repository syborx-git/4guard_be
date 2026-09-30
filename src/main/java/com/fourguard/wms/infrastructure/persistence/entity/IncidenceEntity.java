package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.IncidenceSeverity;
import com.fourguard.wms.domain.enums.IncidenceStatus;
import com.fourguard.wms.domain.enums.IncidenceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Incidence JPA entity — quality events related to inventory items.
 *
 * <p>The {@code folio} field is a PostgreSQL SERIAL (auto-generated integer sequence).
 * It is mapped with {@code insertable=false, updatable=false} so the DB generates it.
 * Use {@code saveAndFlush()} + {@code findById()} in the adapter to retrieve the folio
 * after insert.</p>
 */
@Entity
@Table(name = "incidences", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class IncidenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    /**
     * Auto-generated sequential folio by the DB SERIAL sequence.
     * Not set by application code — read back after INSERT.
     */
    @Column(name = "folio", insertable = false, updatable = false)
    private Integer folio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItemEntity item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncidenceType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidenceSeverity severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id")
    private UserEntity reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private IncidenceStatus status = IncidenceStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 30)
    private com.fourguard.wms.domain.enums.DetectionStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "defect_category", length = 40)
    private com.fourguard.wms.domain.enums.DefectCategory defectCategory;

    @Column(name = "damaged_qty", precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal damagedQty = java.math.BigDecimal.ZERO;

    @Column(name = "lost_qty", precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal lostQty = java.math.BigDecimal.ZERO;

    @Column(name = "associated_cost", precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal associatedCost = java.math.BigDecimal.ZERO;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "MXN";

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "criteria_metadata", columnDefinition = "jsonb")
    private String criteriaMetadata;

    @Column(name = "evidence_metadata", columnDefinition = "jsonb")
    private String evidenceMetadata;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
