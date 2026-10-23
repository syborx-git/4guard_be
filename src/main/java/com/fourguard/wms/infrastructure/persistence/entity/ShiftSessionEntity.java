package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.ShiftSessionSource;
import com.fourguard.wms.domain.enums.ShiftSessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Instancia operativa de un turno para una sucursal y fecha de negocio.
 * Una sola sesión puede estar OPEN por sucursal (índice único parcial).
 */
@Entity
@Table(name = "shift_sessions", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ShiftSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private ShiftEntity shift;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftSessionSource source;

    @Column(name = "opened_by")
    private UUID openedBy;

    @Column(name = "closed_by")
    private UUID closedBy;

    @PrePersist
    protected void onPrePersist() {
        if (startedAt == null) {
            startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
        if (status == null) {
            status = ShiftSessionStatus.OPEN;
        }
        if (source == null) {
            source = ShiftSessionSource.AUTO;
        }
    }
}
