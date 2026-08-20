package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.PalletType;
import com.fourguard.wms.shared.audit.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * JPA Entity for wms.warehouse_reception_pallets.
 * Detail record for an individual pallet/UA scanned in a reception.
 */
@Entity
@Table(
    name = "warehouse_reception_pallets",
    schema = "wms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_reception_pallet_code", columnNames = {"reception_id", "pallet_code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class WarehouseReceptionPalletEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reception_id", nullable = false)
    private WarehouseReceptionEntity reception;

    @Column(name = "pallet_number", nullable = false)
    @Builder.Default
    private Integer palletNumber = 1;

    @Column(name = "pallet_code", nullable = false, length = 50)
    private String palletCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSkuEntity sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private SupplierEntity supplier;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal pieces = java.math.BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "pallet_type", nullable = false, length = 30)
    @Builder.Default
    private PalletType palletType = PalletType.MADERA_ESTANDAR;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItemEntity inventoryItem;
}
