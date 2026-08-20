package com.fourguard.wms.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for a Forklift Operator catalog record (HU-142).
 * Returned by all read and write endpoints of {@code ForkliftOperatorController}.
 */
@Getter
@Builder
public class ForkliftOperatorResponse {

    private final UUID   id;
    private final UUID   organizationId;
    private final String organizationName;
    private final UUID   branchId;
    private final String branchName;

    /** Operational code: MC-001, MC-002, etc. */
    private final String code;

    private final String firstName;
    private final String lastNamePaternal;
    private final String lastNameMaternal;

    /** Denormalized: "firstName lastNamePaternal lastNameMaternal" */
    private final String fullName;

    private final String    licenseNumberDc3;
    private final LocalDate licenseExpirationDate;

    /** VIGENTE | POR_VENCER | VENCIDA — recomputed on every save. */
    private final String licenseStatus;

    /** UUID of the assigned shift. */
    private final UUID   shiftId;
    /** Display name of the assigned shift (e.g. "Turno Matutino A — 06:00-14:00"). */
    private final String shiftName;

    /** ACTIVO | INACTIVO */
    private final String status;

    private final Long version;
    private final String createdBy;
    private final String updatedBy;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}
