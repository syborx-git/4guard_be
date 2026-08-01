package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.ShiftScopeType;
import com.fourguard.wms.domain.enums.ShiftStatus;
import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "wms_shifts", schema = "wms",
        uniqueConstraints = @UniqueConstraint(name = "uk_wms_shifts_code_branch", columnNames = {"code", "branch_id"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ShiftEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "rest_break_minutes")
    @Builder.Default
    private Integer restBreakMinutes = 0;

    @Column(name = "tolerance_minutes")
    @Builder.Default
    private Integer toleranceMinutes = 0;

    @Column(name = "is_overnight")
    @Builder.Default
    private Boolean isOvernight = false;

    @Column(name = "net_duration_minutes", insertable = false, updatable = false)
    private Integer netDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ShiftStatus status = ShiftStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    @Builder.Default
    private ShiftScopeType scopeType = ShiftScopeType.BRANCH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_section_id")
    private WarehouseSectionEntity warehouseSection;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shift_operating_days", schema = "wms",
            joinColumns = @JoinColumn(name = "shift_id"))
    @Column(name = "day_of_week")
    @Builder.Default
    private Set<String> operatingDays = new HashSet<>();

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
