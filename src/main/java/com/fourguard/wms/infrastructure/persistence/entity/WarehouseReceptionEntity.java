package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.PalletType;
import com.fourguard.wms.domain.enums.ReceptionStatus;
import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for wms.warehouse_receptions.
 * Header table for Inbound Receptions (F01).
 */
@Entity
@Table(
    name = "warehouse_receptions",
    schema = "wms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_warehouse_reception_folio", columnNames = {"folio"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class WarehouseReceptionEntity extends BaseVersionedEntity {

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
    private ReceptionStatus status = ReceptionStatus.REGISTERED;

    // ── CheckIn Caseta ──────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id")
    private CarrierEntity carrier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramp_id")
    private LocationEntity ramp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forklift_operator_id")
    private ForkliftOperatorEntity forkliftOperator;

    @Column(name = "doc_number", nullable = false, length = 60)
    private String docNumber;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @Column(name = "reception_time", nullable = false)
    private LocalTime receptionTime;

    @Column(name = "driver_name", nullable = false, length = 150)
    private String driverName;

    @Column(name = "tractor_plates", nullable = false, length = 20)
    private String tractorPlates;

    @Column(name = "box_plates", nullable = false, length = 20)
    private String boxPlates;

    // ── Parámetros de Descarga / Lote ────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id")
    private ProductSkuEntity sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private SupplierEntity supplier;

    @Column(name = "lot_number", length = 50)
    private String lotNumber;

    @Column(name = "elaboration_date")
    private LocalDate elaborationDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "pieces_per_pallet", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal piecesPerPallet = java.math.BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "pallet_type", length = 30)
    @Builder.Default
    private PalletType palletType = PalletType.MADERA_ESTANDAR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id")
    private LocationEntity storageLocation;

    @Column(columnDefinition = "TEXT")
    private String observations;

    // ── Cierre y Cancelación ────────────────────────────────────────────────
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "leader_authorized_by", length = 100)
    private String leaderAuthorizedBy;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    // ── Relaciones Hijas ────────────────────────────────────────────────────
    @OneToMany(mappedBy = "reception", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WarehouseReceptionPalletEntity> pallets = new ArrayList<>();

    @OneToMany(mappedBy = "reception", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WarehouseReceptionSealEntity> seals = new ArrayList<>();
}
