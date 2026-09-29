package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.quality.*;
import com.fourguard.wms.application.dto.response.map.CatBlockReasonResponse;
import com.fourguard.wms.application.dto.response.quality.*;
import com.fourguard.wms.domain.ports.in.QualityUseCase;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.repository.CatBlockReasonJpaRepository;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/quality")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Control de Calidad (QM)", description = "Endpoints para la gestión de producto no conforme, dictamen de liberaciones, verificación de carga F01 y reclamos")
public class QualityController {

    private static final UUID DEFAULT_BRANCH_ID = UUID.fromString("b73f0907-9fa5-4bdf-87db-2eb5e7683936");

    private final QualityUseCase qualityUseCase;
    private final SecurityAuditHelper securityAuditHelper;
    private final UserRepositoryPort userRepositoryPort;
    private final CatBlockReasonJpaRepository catBlockReasonRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // 1. SUBMÓDULO: BLOQUEOS Y PRODUCTO NO CONFORME (PNC)
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/blocks")
    @PreAuthorize("hasAuthority('QUALITY_UPDATE') or hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Registrar bloqueo de calidad / PNC",
               description = "Coloca una tarima o lote en estado IN_QUALITY (20), asienta movimiento QUARANTINE en Kardex y genera folio BLQ-2026-XXXX.")
    public ResponseEntity<ApiResponse<QualityBlockResponse>> createBlock(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgHeader,
            @RequestHeader(value = "X-Branch-Id", required = false) UUID branchHeader,
            @RequestParam(value = "organizationId", required = false) UUID orgParam,
            @RequestParam(value = "branchId", required = false) UUID branchParam,
            @Valid @RequestBody CreateQualityBlockRequest request) {

        UserEntity currentUser = resolveCurrentUser();
        UUID orgId = resolveOrgId(orgHeader, orgParam, currentUser);
        UUID branchId = resolveBranchId(branchHeader, branchParam, currentUser);

        QualityBlockResponse response = qualityUseCase.createBlock(orgId, branchId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Bloqueo de calidad registrado exitosamente", response));
    }

    @GetMapping("/blocks")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Listar bloqueos activos de calidad",
               description = "Retorna la lista de bloqueos vigentes con filtros opcionales de etapa y estado.")
    public ResponseEntity<ApiResponse<List<QualityBlockResponse>>> getActiveBlocks(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgHeader,
            @RequestHeader(value = "X-Branch-Id", required = false) UUID branchHeader,
            @RequestParam(value = "organizationId", required = false) UUID orgParam,
            @RequestParam(value = "branchId", required = false) UUID branchParam,
            @RequestParam(value = "stage", required = false) String stage,
            @RequestParam(value = "status", required = false) String status) {

        UserEntity currentUser = resolveCurrentUser();
        UUID orgId = resolveOrgId(orgHeader, orgParam, currentUser);
        UUID branchId = resolveBranchId(branchHeader, branchParam, currentUser);

        List<QualityBlockResponse> response = qualityUseCase.getActiveBlocks(orgId, branchId, stage, status);
        return ResponseEntity.ok(ApiResponse.ok("Bloqueos de calidad obtenidos con éxito", response));
    }

    @GetMapping("/blocks/{id}")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Obtener detalle de bloqueo por ID", description = "Retorna el detalle completo de un bloqueo, criterios marcados y evidencias adjuntas.")
    public ResponseEntity<ApiResponse<QualityBlockResponse>> getBlockById(@PathVariable UUID id) {
        QualityBlockResponse response = qualityUseCase.getBlockById(id);
        return ResponseEntity.ok(ApiResponse.ok("Bloqueo obtenido con éxito", response));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. SUBMÓDULO: DICTAMEN DE LIBERACIONES Y DESTINOS
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/releases")
    @PreAuthorize("hasAuthority('QUALITY_AUTHORIZE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR')")
    @Operation(summary = "Dictaminar liberación formal de lote",
               description = "Emite dictamen con soporte documental y conmuta inventario según destino (DISTRIBUTION, DESTRUCTION, RETURN).")
    public ResponseEntity<ApiResponse<QualityReleaseResponse>> releaseBlock(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgHeader,
            @RequestHeader(value = "X-Branch-Id", required = false) UUID branchHeader,
            @RequestParam(value = "organizationId", required = false) UUID orgParam,
            @RequestParam(value = "branchId", required = false) UUID branchParam,
            @Valid @RequestBody CreateQualityReleaseRequest request) {

        UserEntity currentUser = resolveCurrentUser();
        UUID orgId = resolveOrgId(orgHeader, orgParam, currentUser);
        UUID branchId = resolveBranchId(branchHeader, branchParam, currentUser);

        QualityReleaseResponse response = qualityUseCase.releaseBlock(orgId, branchId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Dictamen de liberación registrado con éxito", response));
    }

    @GetMapping("/releases")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Historial de liberaciones y dictámenes",
               description = "Retorna el historial de lotes dictaminados con filtro por destino final.")
    public ResponseEntity<ApiResponse<List<QualityReleaseResponse>>> getReleases(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgHeader,
            @RequestHeader(value = "X-Branch-Id", required = false) UUID branchHeader,
            @RequestParam(value = "organizationId", required = false) UUID orgParam,
            @RequestParam(value = "branchId", required = false) UUID branchParam,
            @RequestParam(value = "destination", required = false) String destination) {

        UserEntity currentUser = resolveCurrentUser();
        UUID orgId = resolveOrgId(orgHeader, orgParam, currentUser);
        UUID branchId = resolveBranchId(branchHeader, branchParam, currentUser);

        List<QualityReleaseResponse> response = qualityUseCase.getReleasesHistory(orgId, branchId, destination);
        return ResponseEntity.ok(ApiResponse.ok("Historial de liberaciones obtenido con éxito", response));
    }

    @GetMapping("/releases/{id}")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR')")
    @Operation(summary = "Obtener dictamen de liberación por ID", description = "Retorna el detalle completo de un dictamen de liberación.")
    public ResponseEntity<ApiResponse<QualityReleaseResponse>> getReleaseById(@PathVariable UUID id) {
        QualityReleaseResponse response = qualityUseCase.getReleaseById(id);
        return ResponseEntity.ok(ApiResponse.ok("Dictamen de liberación obtenido con éxito", response));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. SUBMÓDULO: VERIFICACIÓN DE CARGA F01-PO-GC-8.6-03
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/load-verifications")
    @PreAuthorize("hasAuthority('QUALITY_UPDATE') or hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Guardar / Actualizar verificación de carga F01",
               description = "Persiste los 18 criterios normativos de producto y transporte, junto con firmas reglamentarias.")
    public ResponseEntity<ApiResponse<LoadVerificationResponse>> saveVerification(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgHeader,
            @RequestHeader(value = "X-Branch-Id", required = false) UUID branchHeader,
            @RequestParam(value = "organizationId", required = false) UUID orgParam,
            @RequestParam(value = "branchId", required = false) UUID branchParam,
            @Valid @RequestBody SaveLoadVerificationRequest request) {

        UserEntity currentUser = resolveCurrentUser();
        UUID orgId = resolveOrgId(orgHeader, orgParam, currentUser);
        UUID branchId = resolveBranchId(branchHeader, branchParam, currentUser);

        LoadVerificationResponse response = qualityUseCase.createOrUpdateVerification(orgId, branchId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Verificación de carga F01 guardada con éxito", response));
    }

    @GetMapping("/load-verifications")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('SECURITY_GUARD') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Listar verificaciones de carga F01",
               description = "Retorna el directorio de verificaciones de carga filtrado por estatus o fecha.")
    public ResponseEntity<ApiResponse<List<LoadVerificationResponse>>> getVerifications(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgHeader,
            @RequestHeader(value = "X-Branch-Id", required = false) UUID branchHeader,
            @RequestParam(value = "organizationId", required = false) UUID orgParam,
            @RequestParam(value = "branchId", required = false) UUID branchParam,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date", required = false) String date) {

        UserEntity currentUser = resolveCurrentUser();
        UUID orgId = resolveOrgId(orgHeader, orgParam, currentUser);
        UUID branchId = resolveBranchId(branchHeader, branchParam, currentUser);

        List<LoadVerificationResponse> response = qualityUseCase.getVerifications(orgId, branchId, status, date);
        return ResponseEntity.ok(ApiResponse.ok("Verificaciones de carga obtenidas con éxito", response));
    }

    @GetMapping("/load-verifications/{id}")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Obtener formato de verificación de carga F01 por ID", description = "Retorna la pauta completa lista para consulta o impresión PDF.")
    public ResponseEntity<ApiResponse<LoadVerificationResponse>> getVerificationById(@PathVariable UUID id) {
        LoadVerificationResponse response = qualityUseCase.getVerificationById(id);
        return ResponseEntity.ok(ApiResponse.ok("Verificación de carga obtenida con éxito", response));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. SUBMÓDULO: RECLAMOS, KPIS Y CATÁLOGOS
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/claims")
    @PreAuthorize("hasAuthority('QUALITY_UPDATE') or hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Registrar reclamo o incidencia de calidad",
               description = "Registra una incidencia con cuantificación de material dañado/perdido y cálculo financiero de impacto.")
    public ResponseEntity<ApiResponse<QualityClaimResponse>> createClaim(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgHeader,
            @RequestHeader(value = "X-Branch-Id", required = false) UUID branchHeader,
            @RequestParam(value = "organizationId", required = false) UUID orgParam,
            @RequestParam(value = "branchId", required = false) UUID branchParam,
            @Valid @RequestBody CreateQualityClaimRequest request) {

        UserEntity currentUser = resolveCurrentUser();
        UUID orgId = resolveOrgId(orgHeader, orgParam, currentUser);
        UUID branchId = resolveBranchId(branchHeader, branchParam, currentUser);

        QualityClaimResponse response = qualityUseCase.createClaim(orgId, branchId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Reclamo de calidad registrado con éxito", response));
    }

    @GetMapping("/claims")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Listar reclamos e incidencias de calidad", description = "Retorna el concentrado histórico de reclamos por etapa.")
    public ResponseEntity<ApiResponse<List<QualityClaimResponse>>> getClaims(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgHeader,
            @RequestHeader(value = "X-Branch-Id", required = false) UUID branchHeader,
            @RequestParam(value = "organizationId", required = false) UUID orgParam,
            @RequestParam(value = "branchId", required = false) UUID branchParam,
            @RequestParam(value = "stage", required = false) String stage) {

        UserEntity currentUser = resolveCurrentUser();
        UUID orgId = resolveOrgId(orgHeader, orgParam, currentUser);
        UUID branchId = resolveBranchId(branchHeader, branchParam, currentUser);

        List<QualityClaimResponse> response = qualityUseCase.getClaims(orgId, branchId, stage);
        return ResponseEntity.ok(ApiResponse.ok("Reclamos de calidad obtenidos con éxito", response));
    }

    @GetMapping("/dashboard/kpis")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Obtener KPIs consolidados de Calidad QM",
               description = "Retorna métricas ejecutivas: total de bloqueos, liberaciones por destino, verificaciones aprobadas y costo de reclamos.")
    public ResponseEntity<ApiResponse<QualityDashboardKpisResponse>> getDashboardKpis(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgHeader,
            @RequestHeader(value = "X-Branch-Id", required = false) UUID branchHeader,
            @RequestParam(value = "organizationId", required = false) UUID orgParam,
            @RequestParam(value = "branchId", required = false) UUID branchParam) {

        UserEntity currentUser = resolveCurrentUser();
        UUID orgId = resolveOrgId(orgHeader, orgParam, currentUser);
        UUID branchId = resolveBranchId(branchHeader, branchParam, currentUser);

        QualityDashboardKpisResponse response = qualityUseCase.getDashboardKpis(orgId, branchId);
        return ResponseEntity.ok(ApiResponse.ok("Métricas de calidad obtenidas con éxito", response));
    }

    @GetMapping("/catalogs/block-reasons")
    @PreAuthorize("hasAuthority('QUALITY_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('QUALITY_AUDITOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Catálogo de motivos de bloqueo QM", description = "Retorna los motivos estandarizados para retención de tarimas.")
    public ResponseEntity<ApiResponse<List<CatBlockReasonResponse>>> getBlockReasons() {
        List<CatBlockReasonResponse> reasons = catBlockReasonRepository.findByIsActiveTrueOrderByDescriptionAsc().stream()
                .map(r -> new CatBlockReasonResponse(r.getCode(), r.getDescription(), r.getCategory()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Catálogo de motivos de bloqueo obtenido", reasons));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESOLUTORES AUXILIARES
    // ══════════════════════════════════════════════════════════════════════════

    private UserEntity resolveCurrentUser() {
        String username = securityAuditHelper.getCurrentUsername();
        return userRepositoryPort.findByUsername(username)
                .or(() -> userRepositoryPort.findByEmail(username))
                .orElseGet(() -> userRepositoryPort.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("No hay usuarios activos en el sistema")));
    }

    private UUID resolveOrgId(UUID header, UUID param, UserEntity user) {
        if (header != null) return header;
        if (param != null) return param;
        if (user != null && user.getOrganization() != null) return user.getOrganization().getId();
        return null;
    }

    private UUID resolveBranchId(UUID header, UUID param, UserEntity user) {
        if (header != null) return header;
        if (param != null) return param;
        if (user != null && user.getBranch() != null) return user.getBranch().getId();
        return DEFAULT_BRANCH_ID;
    }
}
