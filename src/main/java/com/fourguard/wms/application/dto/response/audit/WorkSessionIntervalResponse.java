package com.fourguard.wms.application.dto.response.audit;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

/**
 * Representa un intervalo de sesión individual para el cálculo de horas trabajadas.
 */
@Value
@Builder
public class WorkSessionIntervalResponse {
    OffsetDateTime loginAt;
    OffsetDateTime logoutAt;
    long rawDurationMinutes;
    long deductedMinutes;
    long netDurationMinutes;
    String closeReason; // "ACTIVE", "LOGOUT", "TIMEOUT", "REVOKED"
    String ipAddress;
}

