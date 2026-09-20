package com.fourguard.wms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

/**
 * JPA entity for wms.cat_currencies.
 * ISO 4217 currency catalog seeded in V9 migration.
 */
@Entity
@Table(name = "cat_currencies", schema = "wms")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatCurrencyEntity {

    @Id
    @Column(name = "code", length = 3, nullable = false, updatable = false)
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.CHAR)
    private String code;

    @Column(name = "label", length = 60, nullable = false)
    private String label;

    @Column(name = "symbol", length = 5)
    private String symbol;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
