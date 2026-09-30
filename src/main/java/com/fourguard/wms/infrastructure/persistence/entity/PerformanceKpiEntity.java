package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "performance_kpis",
    schema = "wms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_perf_kpi_name_module", columnNames = {"organization_id", "module", "name"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class PerformanceKpiEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizationEntity organization;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String module;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "evaluation_type", nullable = false, length = 50)
    private String evaluationType;

    @Column(name = "target_threshold")
    private Double targetThreshold;

    @Column(name = "warning_threshold")
    private Double warningThreshold;

    @Column(name = "critical_threshold")
    private Double criticalThreshold;

    @Column(name = "range_low")
    private Double rangeLow;

    @Column(name = "range_high")
    private Double rangeHigh;

    @Column(name = "current_value")
    private Double currentValue;

    @Column(name = "last_measured_at")
    private OffsetDateTime lastMeasuredAt;

    @Column(length = 30)
    @Builder.Default
    private String status = "OPTIMAL";

    @Column(name = "source_process", length = 100)
    private String sourceProcess;

    @Column(name = "start_event", length = 100)
    private String startEvent;

    @Column(name = "end_event", length = 100)
    private String endEvent;

    @Column(name = "frequency_value")
    private Integer frequencyValue;

    @Column(name = "frequency_unit", length = 30)
    private String frequencyUnit;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "created_by_user", length = 100)
    private String createdByUser;

    @Column(name = "updated_by_user", length = 100)
    private String updatedByUser;
}
