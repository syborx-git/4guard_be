package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.response.audit.ActiveSessionResponse;
import com.fourguard.wms.application.dto.response.audit.UserActivityAuditResponse;
import com.fourguard.wms.domain.ports.in.GetActiveSessionsUseCase;
import com.fourguard.wms.domain.ports.in.GetUserActivityUseCase;
import com.fourguard.wms.domain.ports.in.RevokeUserSessionUseCase;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for audit log queries and active sessions.
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "Endpoints para la consulta de bitácoras y sesiones activas")
public class AuditController {

    private final GetActiveSessionsUseCase getActiveSessionsUseCase;
    private final GetUserActivityUseCase getUserActivityUseCase;
    private final RevokeUserSessionUseCase revokeUserSessionUseCase;

    @GetMapping("/active-sessions")
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(
            summary = "Consultar sesiones activas",
            description = "Devuelve el listado de usuarios con sesión activa (logueados en las últimas 24 horas y sin logout posterior).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sesiones activas obtenidas con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado o permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<ActiveSessionResponse>>> getActiveSessions(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            Principal principal) {

        List<ActiveSessionResponse> activeSessions =
                getActiveSessionsUseCase.getActiveSessions(organizationId, branchId, principal);

        return ResponseEntity.ok(
                ApiResponse.ok("Sesiones activas recuperadas con éxito", activeSessions));
    }

    @GetMapping("/user-activity")
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(
            summary = "Consultar actividad global de usuarios",
            description = "Devuelve la bitácora de actividad general permitiendo filtros opcionales por usuario, acción y rango de fechas.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de actividad recuperado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado o permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<UserActivityAuditResponse>>> getUserActivity(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            Principal principal) {

        List<UserActivityAuditResponse> activityLogs =
                getUserActivityUseCase.getUserActivityLogs(userId, action, fromDate, toDate, principal);

        return ResponseEntity.ok(
                ApiResponse.ok("Historial de actividad recuperado con éxito", activityLogs));
    }

    @DeleteMapping("/active-sessions/{userId}")
    @PreAuthorize("hasAuthority('AUDIT_WRITE') or hasRole('OPERATIONS_MANAGER') or hasRole('ADMIN')")
    @Operation(
            summary = "Revocar sesión de usuario",
            description = "Revoca inmediatamente la sesión activa del usuario especificado, expulsándolo del sistema y registrando la acción en auditoría.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sesión revocada correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado o permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> revokeActiveSession(
            @PathVariable UUID userId,
            Principal principal) {

        revokeUserSessionUseCase.revokeUserSession(userId, principal);

        return ResponseEntity.ok(
                ApiResponse.ok("Sesión revocada correctamente", null));
    }
}
