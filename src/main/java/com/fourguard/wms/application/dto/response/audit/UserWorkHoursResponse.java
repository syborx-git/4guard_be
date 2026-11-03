package com.fourguard.wms.application.dto.response.audit;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO que consolida las métricas e intervalos de horas trabajadas por usuario.
 * Exclusivo para consulta de Dirección General (HU-Jornadas).
 */
@Value
@Builder
public class UserWorkHoursResponse {
    UUID userId;
    String username;
    String fullName;
    String email;
    String roleName;
    UUID organizationId;
    String organizationName;
    UUID branchId;
    String branchName;
    LocalDate businessDate;
    String status; // "ONLINE", "OFFLINE"
    OffsetDateTime firstLoginAt;
    OffsetDateTime lastLogoutAt;
    long totalMinutesActive;
    String formattedHours;
    int sessionCount;
    int timeoutLogoutCount;
    boolean isCurrentShiftMet; // e.g. >= 480 minutos (8h)
    List<WorkSessionIntervalResponse> intervals;
}

