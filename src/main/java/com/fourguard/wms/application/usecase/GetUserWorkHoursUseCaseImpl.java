package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.response.audit.UserWorkHoursResponse;
import com.fourguard.wms.application.dto.response.audit.WorkSessionIntervalResponse;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.in.GetUserWorkHoursUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del caso de uso para consultar las horas trabajadas netas de todos los usuarios.
 * Restringido con RBAC estricto exclusivamente para Dirección General (ADMIN / CEO).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetUserWorkHoursUseCaseImpl implements GetUserWorkHoursUseCase {

    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    private static final ZoneId WMS_TIMEZONE = ZoneId.of("America/Mexico_City");
    private static final int TIMEOUT_DEDUCTION_MINUTES = 15;
    private static final List<String> SESSION_ACTIONS = List.of(
            "LOGIN", "LOGOUT", "SESSION_TIMEOUT_LOGOUT", "REVOKE_SESSION"
    );

    @Override
    @Transactional(readOnly = true)
    public List<UserWorkHoursResponse> getWorkHours(
            LocalDate date,
            UUID organizationId,
            UUID branchId,
            String roleFilter,
            Principal principal) {

        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }

        log.info("[AUDIT-WORK-HOURS] Solicitud de reporte de horas trabajadas por: {}", principal.getName());

        // 1. RBAC estricto: Solo Dirección General (ADMIN / CEO)
        UserEntity requestingUser = userRepositoryPort.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario solicitante no encontrado: " + principal.getName()));

        String requestingRole = requestingUser.getRole() != null ? requestingUser.getRole().getName() : "";
        boolean isDirectorGeneral = "ADMIN".equalsIgnoreCase(requestingRole) || "CEO".equalsIgnoreCase(requestingRole);

        if (!isDirectorGeneral) {
            log.warn("[SECURITY] Acceso denegado a reporte de horas trabajadas para rol: {}", requestingRole);
            throw new AccessDeniedException("Acceso restringido: El reporte de horas trabajadas es de uso exclusivo de Dirección General.");
        }

        // 2. Definición del rango del día en la zona horaria operativa
        LocalDate targetDate = (date != null) ? date : LocalDate.now(WMS_TIMEZONE);
        OffsetDateTime startOfDay = targetDate.atStartOfDay(WMS_TIMEZONE).toOffsetDateTime();
        OffsetDateTime endOfDay = targetDate.plusDays(1).atStartOfDay(WMS_TIMEZONE).toOffsetDateTime();
        boolean isToday = targetDate.equals(LocalDate.now(WMS_TIMEZONE));
        OffsetDateTime nowUtc = OffsetDateTime.now();

        // 3. Obtener todos los eventos de sesión del día
        List<AuditLogEntity> sessionLogs = auditLogRepositoryPort.findSessionsInDateRange(
                startOfDay, endOfDay, SESSION_ACTIONS
        );

        // Agrupar eventos de auditoría por userId
        Map<UUID, List<AuditLogEntity>> logsByUser = sessionLogs.stream()
                .collect(Collectors.groupingBy(AuditLogEntity::getUserId));

        // 4. Obtener catálogo de usuarios filtrados por organización y sucursal
        UUID targetOrgId = organizationId != null ? organizationId : requestingUser.getOrganization().getId();
        List<UserEntity> users = userRepositoryPort.findAll();

        return users.stream()
                .filter(u -> targetOrgId == null || (u.getOrganization() != null && targetOrgId.equals(u.getOrganization().getId())))
                .filter(u -> branchId == null || (u.getBranch() != null && branchId.equals(u.getBranch().getId())))
                .filter(u -> roleFilter == null || roleFilter.isBlank() ||
                        (u.getRole() != null && u.getRole().getName().equalsIgnoreCase(roleFilter.trim())))
                .map(user -> processUserWorkHours(user, logsByUser.getOrDefault(user.getId(), Collections.emptyList()), targetDate, isToday, nowUtc))
                .sorted(Comparator.comparing(UserWorkHoursResponse::getTotalMinutesActive).reversed())
                .collect(Collectors.toList());
    }

    private UserWorkHoursResponse processUserWorkHours(
            UserEntity user,
            List<AuditLogEntity> userLogs,
            LocalDate businessDate,
            boolean isToday,
            OffsetDateTime nowUtc) {

        List<WorkSessionIntervalResponse> intervals = new ArrayList<>();
        long totalMinutes = 0;
        int timeoutCount = 0;
        OffsetDateTime firstLogin = null;
        OffsetDateTime lastLogout = null;
        String currentStatus = "OFFLINE";

        if (!userLogs.isEmpty()) {
            List<AuditLogEntity> sortedLogs = userLogs.stream()
                    .sorted(Comparator.comparing(AuditLogEntity::getCreatedAt))
                    .collect(Collectors.toList());

            AuditLogEntity openLogin = null;

            for (AuditLogEntity event : sortedLogs) {
                String action = event.getAction();

                if ("LOGIN".equals(action)) {
                    if (firstLogin == null) {
                        firstLogin = event.getCreatedAt();
                    }
                    if (openLogin != null) {
                        // Cierre implícito previo por nuevo login consecutivo
                        WorkSessionIntervalResponse implicitInterval = buildInterval(
                                openLogin.getCreatedAt(),
                                event.getCreatedAt(),
                                "LOGOUT",
                                openLogin.getIpAddress(),
                                false
                        );
                        intervals.add(implicitInterval);
                        totalMinutes += implicitInterval.getNetDurationMinutes();
                    }
                    openLogin = event;
                } else if (openLogin != null && ("LOGOUT".equals(action) || "SESSION_TIMEOUT_LOGOUT".equals(action) || "REVOKE_SESSION".equals(action))) {
                    boolean isTimeout = "SESSION_TIMEOUT_LOGOUT".equals(action);
                    if (isTimeout) {
                        timeoutCount++;
                    }

                    WorkSessionIntervalResponse interval = buildInterval(
                            openLogin.getCreatedAt(),
                            event.getCreatedAt(),
                            isTimeout ? "TIMEOUT" : action,
                            openLogin.getIpAddress(),
                            isTimeout
                    );
                    intervals.add(interval);
                    totalMinutes += interval.getNetDurationMinutes();
                    lastLogout = event.getCreatedAt();
                    openLogin = null;
                }
            }

            // Si hay un login aún abierto al final de la secuencia
            if (openLogin != null) {
                if (isToday) {
                    // La sesión sigue activa en este momento
                    long rawMinutes = Math.max(0, Duration.between(openLogin.getCreatedAt(), nowUtc).toMinutes());
                    WorkSessionIntervalResponse activeInterval = WorkSessionIntervalResponse.builder()
                            .loginAt(openLogin.getCreatedAt())
                            .logoutAt(null)
                            .rawDurationMinutes(rawMinutes)
                            .deductedMinutes(0)
                            .netDurationMinutes(rawMinutes)
                            .closeReason("ACTIVE")
                            .ipAddress(openLogin.getIpAddress())
                            .build();

                    intervals.add(activeInterval);
                    totalMinutes += rawMinutes;
                    currentStatus = "ONLINE";
                } else {
                    // Sesión pasada sin logout registrado (apagado forzoso): cerrar al final del turno/día con tope razonable
                    long cappedMinutes = Math.min(480, Math.max(0, Duration.between(openLogin.getCreatedAt(), openLogin.getCreatedAt().plusHours(8)).toMinutes()));
                    WorkSessionIntervalResponse closedPastInterval = WorkSessionIntervalResponse.builder()
                            .loginAt(openLogin.getCreatedAt())
                            .logoutAt(openLogin.getCreatedAt().plusMinutes(cappedMinutes))
                            .rawDurationMinutes(cappedMinutes)
                            .deductedMinutes(0)
                            .netDurationMinutes(cappedMinutes)
                            .closeReason("LOGOUT")
                            .ipAddress(openLogin.getIpAddress())
                            .build();

                    intervals.add(closedPastInterval);
                    totalMinutes += cappedMinutes;
                    lastLogout = openLogin.getCreatedAt().plusMinutes(cappedMinutes);
                }
            }
        }

        String formattedHours = String.format("%dh %02dm", totalMinutes / 60, totalMinutes % 60);

        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "");
        fullName = fullName.trim().isEmpty() ? user.getUsername() : fullName.trim();

        return UserWorkHoursResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(fullName)
                .email(user.getEmail())
                .roleName(user.getRole() != null ? user.getRole().getName() : "SIN_ROL")
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .branchName(user.getBranch() != null ? user.getBranch().getName() : null)
                .businessDate(businessDate)
                .status(currentStatus)
                .firstLoginAt(firstLogin)
                .lastLogoutAt(lastLogout)
                .totalMinutesActive(totalMinutes)
                .formattedHours(formattedHours)
                .sessionCount(intervals.size())
                .timeoutLogoutCount(timeoutCount)
                .isCurrentShiftMet(totalMinutes >= 480)
                .intervals(intervals)
                .build();
    }

    private WorkSessionIntervalResponse buildInterval(
            OffsetDateTime loginAt,
            OffsetDateTime logoutAt,
            String closeReason,
            String ipAddress,
            boolean isTimeout) {

        long rawMinutes = Math.max(0, Duration.between(loginAt, logoutAt).toMinutes());
        long deductedMinutes = isTimeout ? Math.min(rawMinutes, TIMEOUT_DEDUCTION_MINUTES) : 0;
        long netMinutes = Math.max(0, rawMinutes - deductedMinutes);

        return WorkSessionIntervalResponse.builder()
                .loginAt(loginAt)
                .logoutAt(logoutAt)
                .rawDurationMinutes(rawMinutes)
                .deductedMinutes(deductedMinutes)
                .netDurationMinutes(netMinutes)
                .closeReason(closeReason)
                .ipAddress(ipAddress)
                .build();
    }
}

