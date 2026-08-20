package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.TransferReason;
import com.fourguard.wms.domain.enums.TransferStatus;
import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for wms.warehouse_transfers.
 * Internal transfer and relocation header.
 */
@Entity
@Table(
    name = "warehouse_transfers",
    schema = "wms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_warehouse_transfer_folio", columnNames = {"folio"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class WarehouseTransferEntity extends BaseVersionedEntity {

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

    @Column(nullable = false, length = 30)
    private String folio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransferStatus status = TransferStatus.COMPLETED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_location_id", nullable = false)
    private LocationEntity originLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_location_id", nullable = false)
    private LocationEntity destinationLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forklift_operator_id")
    private ForkliftOperatorEntity forkliftOperator;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 30)
    private TransferReason reasonCode;

    @Column(name = "reason_label", length = 100)
    private String reasonLabel;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "total_pallets", nullable = false)
    @Builder.Default
    private Integer totalPallets = 0;

    @Column(name = "total_pieces", nullable = false)
    @Builder.Default
    private Double totalPieces = 0.0;

    @Column(name = "distinct_skus", nullable = false)
    @Builder.Default
    private Integer distinctSkus = 0;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WarehouseTransferItemEntity> items = new ArrayList<>();
}
