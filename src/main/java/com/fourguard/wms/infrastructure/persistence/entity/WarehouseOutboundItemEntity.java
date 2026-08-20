package com.fourguard.wms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * JPA Entity for wms.warehouse_outbound_items.
 * Items/UAs dispatched in an outbound movement.
 */
@Entity
@Table(name = "warehouse_outbound_items", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WarehouseOutboundItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outbound_id", nullable = false)
    private WarehouseOutboundEntity outbound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItemEntity item;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal pieces = java.math.BigDecimal.ZERO;

    @Column(name = "pallet_code", length = 50)
    private String palletCode;

    @Column(name = "lot_number", length = 50)
    private String lotNumber;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "location_code", length = 50)
    private String locationCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
