package com.fourguard.wms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * JPA Entity for wms.warehouse_reception_seals.
 * Security seals attached to transport vehicle during reception.
 */
@Entity
@Table(
    name = "warehouse_reception_seals",
    schema = "wms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_reception_seal", columnNames = {"reception_id", "seal_number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WarehouseReceptionSealEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reception_id", nullable = false)
    private WarehouseReceptionEntity reception;

    @Column(name = "seal_number", nullable = false, length = 50)
    private String sealNumber;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private OffsetDateTime registeredAt;

    @PrePersist
    protected void onPrePersist() {
        if (registeredAt == null) {
            registeredAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
