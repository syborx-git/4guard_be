package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products_sku", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ProductSkuEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 3)
    private BigDecimal weight;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
}
