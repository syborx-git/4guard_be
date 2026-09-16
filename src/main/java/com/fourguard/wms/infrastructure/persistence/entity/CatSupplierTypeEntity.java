package com.fourguard.wms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * JPA entity for wms.cat_supplier_types.
 * Catalog table seeded in V9 migration. Not versioned (no optimistic lock
 * needed for catalogs).
 */
@Entity
@Table(name = "cat_supplier_types", schema = "wms")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatSupplierTypeEntity {

    @Id
    @Column(name = "code", length = 30, nullable = false, updatable = false)
    private String code;

    @Column(name = "label_es", length = 100, nullable = false)
    private String labelEs;

    @Column(name = "label_en", length = 100)
    private String labelEn;

    @Column(name = "is_service", nullable = false)
    private Boolean isService;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null)
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
