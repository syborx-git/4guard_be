package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.LoadVerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(
    name = "load_verifications",
    schema = "wms",
    indexes = {
        @Index(name = "idx_qm_verif_org", columnList = "organization_id, branch_id"),
        @Index(name = "idx_qm_verif_remision", columnList = "remision_number"),
        @Index(name = "idx_qm_verif_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoadVerificationEntity {

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

    @Column(name = "control_number", nullable = false, length = 50)
    @Builder.Default
    private String controlNumber = "F01-PO-GC-8.6-03";

    @Column(name = "revision_number", nullable = false, length = 10)
    @Builder.Default
    private String revisionNumber = "03";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_id")
    private WarehouseOutboundEntity outbound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reception_id")
    private WarehouseReceptionEntity reception;

    @Column(name = "remision_number", nullable = false, length = 60)
    private String remisionNumber;

    @Column(name = "product_description", nullable = false, length = 255)
    private String productDescription;

    @Column(name = "client_name", nullable = false, length = 150)
    private String clientName;

    @Column(name = "verification_date", nullable = false)
    private LocalDate verificationDate;

    @Column(name = "verification_time", nullable = false)
    private LocalTime verificationTime;

    @Column(name = "ramp_code", nullable = false, length = 30)
    private String rampCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoadVerificationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_criteria", columnDefinition = "JSONB", nullable = false)
    private String productCriteria;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transport_criteria", columnDefinition = "JSONB", nullable = false)
    private String transportCriteria;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "signatures", columnDefinition = "JSONB", nullable = false)
    private String signatures;

    @Column(name = "general_observations", columnDefinition = "TEXT")
    private String generalObservations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_metadata", columnDefinition = "JSONB")
    private String evidenceMetadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
