package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.ForkliftOperatorStatus;
import com.fourguard.wms.domain.enums.LicenseStatus;
import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA Entity for the {@code wms.forklift_operators} table.
 * Represents a certified forklift operator in the WMS catalog (HU-142).
 * Extends {@link BaseVersionedEntity} for optimistic locking and audit fields.
 */
@Entity
@Table(
    name = "forklift_operators",
    schema = "wms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_forklift_operator_code", columnNames = {"organization_id", "code"}),
        @UniqueConstraint(name = "uk_forklift_operator_dc3",  columnNames = {"organization_id", "license_number_dc3"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ForkliftOperatorEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    /** Operational code, auto-generated as MC-XXX per organization. */
    @Column(nullable = false, length = 30)
    private String code;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name_paternal", nullable = false, length = 100)
    private String lastNamePaternal;

    @Column(name = "last_name_maternal", nullable = false, length = 100)
    private String lastNameMaternal;

    /** Denormalized concatenation: firstName + lastNamePaternal + lastNameMaternal. Maintained by the service. */
    @Column(name = "full_name", nullable = false, length = 310)
    private String fullName;

    /** STPS / DC-3 certification number. Must be unique within the organization. */
    @Column(name = "license_number_dc3", nullable = false, length = 50)
    private String licenseNumberDc3;

    @Column(name = "license_expiration_date", nullable = false)
    private LocalDate licenseExpirationDate;

    /**
     * Computed license validity status. Recalculated on every save by {@code ForkliftOperatorService}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "license_status", nullable = false, length = 20)
    @Builder.Default
    private LicenseStatus licenseStatus = LicenseStatus.VIGENTE;

    /** FK to wms.wms_shifts. Nullable — operator may not have an assigned shift yet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private ShiftEntity shift;

    /** Denormalized shift display name for quick rendering (avoids join on read-heavy queries). */
    @Column(name = "shift_name", length = 150)
    private String shiftName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ForkliftOperatorStatus status = ForkliftOperatorStatus.ACTIVO;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
}
