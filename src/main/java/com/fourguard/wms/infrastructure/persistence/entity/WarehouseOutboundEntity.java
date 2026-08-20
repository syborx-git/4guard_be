package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.OutboundStatus;
import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for wms.warehouse_outbounds.
 * Header record for outbound dispatches (Salidas de Almacén F03).
 */
@Entity
@Table(
    name = "warehouse_outbounds",
    schema = "wms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_warehouse_outbound_folio", columnNames = {"folio"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class WarehouseOutboundEntity extends BaseVersionedEntity {

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
    private OutboundStatus status = OutboundStatus.COMPLETED;

    // ── Cliente y Destino ───────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private ClientDestinationEntity destination;

    @Column(name = "destination_name", length = 200)
    private String destinationName;

    @Column(name = "destination_address", columnDefinition = "TEXT")
    private String destinationAddress;

    // ── Transporte y Chofer ─────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id")
    private CarrierEntity carrier;

    @Column(name = "transport_type", nullable = false, length = 30)
    @Builder.Default
    private String transportType = "TRAILER";

    @Column(name = "driver_name", nullable = false, length = 150)
    private String driverName;

    @Column(name = "economic_number", length = 30)
    private String economicNumber;

    @Column(name = "tractor_plates", nullable = false, length = 20)
    private String tractorPlates;

    @Column(name = "box_plates", nullable = false, length = 20)
    private String boxPlates;

    @Column(name = "seal_number", nullable = false, length = 50)
    private String sealNumber;

    @Column(name = "remision_no", nullable = false, length = 60)
    private String remisionNo;

    // ── Totales Desnormalizados ─────────────────────────────────────────────
    @Column(name = "total_pallets", nullable = false)
    @Builder.Default
    private Integer totalPallets = 0;

    @Column(name = "total_pieces", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal totalPieces = java.math.BigDecimal.ZERO;

    @Column(name = "distinct_skus", nullable = false)
    @Builder.Default
    private Integer distinctSkus = 0;

    // ── Cancelación ─────────────────────────────────────────────────────────
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @OneToMany(mappedBy = "outbound", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WarehouseOutboundItemEntity> items = new ArrayList<>();
}
