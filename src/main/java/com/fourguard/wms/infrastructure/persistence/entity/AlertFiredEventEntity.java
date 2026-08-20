package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.FiredEventStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_fired_events", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class AlertFiredEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_configuration_id", nullable = false)
    private AlertConfigEntity alertConfiguration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @Column(name = "triggered_at")
    @Builder.Default
    private OffsetDateTime triggeredAt = OffsetDateTime.now();

    @Column(name = "entity_reference", length = 255)
    private String entityReference;

    @Column(name = "evaluated_value", precision = 12, scale = 2)
    private BigDecimal evaluatedValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private FiredEventStatus status = FiredEventStatus.FIRED;

    @Column(name = "acknowledged_by", length = 255)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;
}
