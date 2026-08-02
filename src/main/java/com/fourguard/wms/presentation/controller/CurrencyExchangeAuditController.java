package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.response.CurrencyAuditResponse;
import com.fourguard.wms.domain.ports.in.CurrencyExchangeAuditUseCase;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller para Bitácora de Auditoría del Módulo Currency Exchange.
 * Base path: /currency-exchange
 */
@RestController
@RequestMapping("/currency-exchange")
@RequiredArgsConstructor
@Tag(name = "Auditoría de Divisas y Tipos de Cambio", description = "Endpoints para consulta de bitácora forense de auditoría del módulo")
public class CurrencyExchangeAuditController {

    private final CurrencyExchangeAuditUseCase auditUseCase;

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Consultar bitácora de auditoría del módulo", description = "Obtiene la bitácora inmutable de cambios de divisas y tipos de cambio.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bitácora de auditoría obtenida con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<ApiResponse<List<CurrencyAuditResponse>>> getAuditLogs(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        List<CurrencyAuditResponse> response = auditUseCase.getAuditLogs(organizationId, entityType, entityId, action, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Bitácora de auditoría obtenida con éxito", response));
    }
}
