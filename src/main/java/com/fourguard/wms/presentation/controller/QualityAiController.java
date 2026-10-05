package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.quality.ai.QualityAiChatRequest;
import com.fourguard.wms.application.dto.request.quality.ai.QualityAiRuleEvaluationRequest;
import com.fourguard.wms.application.dto.request.quality.ai.SamplingCalculationRequest;
import com.fourguard.wms.application.dto.response.quality.ai.QualityAiChatResponse;
import com.fourguard.wms.application.dto.response.quality.ai.QualityAiEvaluationResponse;
import com.fourguard.wms.application.dto.response.quality.ai.SamplingCalculationResponse;
import com.fourguard.wms.domain.ports.in.QualityAiUseCase;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/quality/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Asistente IA de Calidad QM", description = "Endpoints para evaluación en memoria de tolerancias físicas, soporte normativo y cálculo de muestreos")
public class QualityAiController {

    private static final UUID DEFAULT_BRANCH_ID = UUID.fromString("b73f0907-9fa5-4bdf-87db-2eb5e7683936");

    private final QualityAiUseCase qualityAiUseCase;
    private final SecurityAuditHelper securityAuditHelper;
    private final UserRepositoryPort userRepositoryPort;

    @PostMapping("/evaluate-rule")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('QUALITY_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Evaluación determinista de reglas físicas de calidad",
               description = "Evalúa en < 1ms tolerancias de inclinación, daños en empaque, uso de Diurex, roturas de tarima y límites de humedad bajo IT01, IT02 e IT01-8.6-02.")
    public ResponseEntity<ApiResponse<QualityAiEvaluationResponse>> evaluateRule(
            @Valid @RequestBody QualityAiRuleEvaluationRequest request) {

        QualityAiEvaluationResponse response = qualityAiUseCase.evaluateRule(request);
        return ResponseEntity.ok(ApiResponse.ok("Reglas de calidad evaluadas exitosamente", response));
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('QUALITY_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Consultar al Asistente IA de Calidad",
               description = "Responde dudas técnicas y normativas fundamentadas en los instructivos oficiales de 4GUARD con fallback a motor de reglas.")
    public ResponseEntity<ApiResponse<QualityAiChatResponse>> askAssistant(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgHeader,
            @RequestHeader(value = "X-Branch-Id", required = false) UUID branchHeader,
            @RequestParam(value = "organizationId", required = false) UUID orgParam,
            @RequestParam(value = "branchId", required = false) UUID branchParam,
            @Valid @RequestBody QualityAiChatRequest request) {

        UserEntity currentUser = resolveCurrentUser();
        UUID orgId = resolveOrgId(orgHeader, orgParam, currentUser);
        UUID branchId = resolveBranchId(branchHeader, branchParam, currentUser);

        QualityAiChatResponse response = qualityAiUseCase.askAssistant(orgId, branchId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Respuesta del Asistente de Calidad generada", response));
    }

    @PostMapping("/sampling-calc")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('QUALITY_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Calcular plan oficial de muestreo",
               description = "Calcula la cantidad exacta de muestra y el protocolo de extracción por tipo de material o café verde bajo IT01-PO-GC-8.6-04 Rev. 02.")
    public ResponseEntity<ApiResponse<SamplingCalculationResponse>> calculateSampling(
            @Valid @RequestBody SamplingCalculationRequest request) {

        SamplingCalculationResponse response = qualityAiUseCase.calculateSampling(request);
        return ResponseEntity.ok(ApiResponse.ok("Plan de muestreo calculado exitosamente", response));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESOLUTORES AUXILIARES CON CONTROL DE ACCESO (DEFENSA BOLA/IDOR Y AUTENTICACIÓN)
    // ══════════════════════════════════════════════════════════════════════════

    private UserEntity resolveCurrentUser() {
        String username = securityAuditHelper.getCurrentUsername();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username) || "SYSTEM".equalsIgnoreCase(username)) {
            throw new org.springframework.security.access.AccessDeniedException("Sesión no válida o no autenticada");
        }
        return userRepositoryPort.findByUsername(username)
                .or(() -> userRepositoryPort.findByEmail(username))
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Usuario no encontrado en el sistema: " + username));
    }

    private UUID resolveOrgId(UUID header, UUID param, UserEntity user) {
        UUID targetOrg = header != null ? header : (param != null ? param : (user != null && user.getOrganization() != null ? user.getOrganization().getId() : null));
        if (user != null && user.getRole() != null && "SUPER_ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            return targetOrg;
        }
        if (user != null && user.getOrganization() != null) {
            UUID userOrgId = user.getOrganization().getId();
            if (targetOrg != null && !targetOrg.equals(userOrgId)) {
                log.warn("Security Alert: User {} attempted cross-tenant access to org {}", user.getUsername(), targetOrg);
                throw new org.springframework.security.access.AccessDeniedException("No tiene permisos para acceder a una organización ajena");
            }
            return userOrgId;
        }
        return targetOrg;
    }

    private UUID resolveBranchId(UUID header, UUID param, UserEntity user) {
        UUID targetBranch = header != null ? header : (param != null ? param : (user != null && user.getBranch() != null ? user.getBranch().getId() : DEFAULT_BRANCH_ID));
        if (user != null && user.getRole() != null && ("SUPER_ADMIN".equalsIgnoreCase(user.getRole().getName()) || "ADMIN".equalsIgnoreCase(user.getRole().getName()))) {
            return targetBranch;
        }
        if (user != null && user.getBranch() != null) {
            UUID userBranchId = user.getBranch().getId();
            if (targetBranch != null && !targetBranch.equals(userBranchId)) {
                log.warn("Security Alert: User {} attempted cross-branch access to branch {}", user.getUsername(), targetBranch);
                throw new org.springframework.security.access.AccessDeniedException("No tiene permisos para operar en una sucursal no asignada");
            }
            return userBranchId;
        }
        return targetBranch != null ? targetBranch : DEFAULT_BRANCH_ID;
    }
}
