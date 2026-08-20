package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.*;
import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "alert_configurations", schema = "wms",
        uniqueConstraints = @UniqueConstraint(name = "uk_alert_name_org", columnNames = {"organization_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class AlertConfigEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AlertEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "channels", columnDefinition = "TEXT[]", nullable = false)
    @Builder.Default
    private List<String> channels = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "recipients", columnDefinition = "TEXT[]", nullable = false)
    @Builder.Default
    private List<String> recipients = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertCondition condition;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AlertRecurrence recurrence = AlertRecurrence.NEVER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AlertEscalation escalation = AlertEscalation.NONE;

    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
