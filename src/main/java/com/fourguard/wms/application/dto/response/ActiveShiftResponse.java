package com.fourguard.wms.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Turno vigente resuelto para una sucursal.
 *
 * <p>En la Fase 2 la resolución es de solo lectura: {@code sessionId} viaja
 * {@code null} y no se persiste ninguna sesión. La Fase 3 agregará
 * {@code shift_sessions} y poblará {@code sessionId}.</p>
 */
@Getter
@Builder(toBuilder = true)
public class ActiveShiftResponse {

    /** Sesión operativa persistida. Null en Fase 2. */
    private final UUID sessionId;

    /** Turno del catálogo que aplica en este momento. Null si no hay turno vigente. */
    private final UUID shiftId;
    private final String shiftCode;
    private final String shiftName;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Boolean isOvernight;
    private final Integer toleranceMinutes;

    /** Fecha de negocio calculada en la zona horaria de la sucursal. */
    private final LocalDate businessDate;

    private final UUID branchId;
    private final String branchName;
    private final String timezone;

    /** Indica si la sucursal tiene turnos activos configurados. */
    private final boolean configured;

    /** AUTO = hay turno vigente; FALLBACK = sin turno vigente o sin configuración. */
    private final String source;

    private final OffsetDateTime resolvedAt;
}
