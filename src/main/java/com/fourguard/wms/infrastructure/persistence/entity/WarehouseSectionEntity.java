package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.WarehouseSectionStatus;
import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "warehouse_sections", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class WarehouseSectionEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(length = 100)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(name = "pos_fijas")
    private Integer posFijas;

    @Column(name = "capacidad_tarimas")
    private Integer capacidadTarimas;

    @Column(name = "factor_estiba", length = 50)
    private String factorEstiba;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "polygon_points", columnDefinition = "TEXT")
    private String polygonPoints;

    @Column(name = "label_x", precision = 6, scale = 2)
    private BigDecimal labelX;

    @Column(name = "label_y", precision = 6, scale = 2)
    private BigDecimal labelY;

    @Column(name = "sublabel_x", precision = 6, scale = 2)
    private BigDecimal sublabelX;

    @Column(name = "sublabel_y", precision = 6, scale = 2)
    private BigDecimal sublabelY;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WarehouseSectionStatus status = WarehouseSectionStatus.ACTIVE;

    @OneToMany(mappedBy = "section", fetch = FetchType.LAZY)
    @Builder.Default
    private List<LocationEntity> locations = new ArrayList<>();
}


