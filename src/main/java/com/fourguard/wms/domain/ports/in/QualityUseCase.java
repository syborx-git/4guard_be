package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.quality.*;
import com.fourguard.wms.application.dto.response.quality.*;

import java.util.List;
import java.util.UUID;

public interface QualityUseCase {

    // ── Submódulo 1: Bloqueos y PNC ──
    QualityBlockResponse createBlock(UUID organizationId, UUID branchId, UUID userId, CreateQualityBlockRequest request);

    List<QualityBlockResponse> getActiveBlocks(UUID organizationId, UUID branchId, String stage, String status);

    QualityBlockResponse getBlockById(UUID blockId);

    // ── Submódulo 2: Liberaciones y Destinos ──
    QualityReleaseResponse releaseBlock(UUID organizationId, UUID branchId, UUID userId, CreateQualityReleaseRequest request);

    List<QualityReleaseResponse> getReleasesHistory(UUID organizationId, UUID branchId, String destination);

    QualityReleaseResponse getReleaseById(UUID releaseId);

    // ── Submódulo 3: Verificación de Carga F01 ──
    LoadVerificationResponse createOrUpdateVerification(UUID organizationId, UUID branchId, UUID userId, SaveLoadVerificationRequest request);

    LoadVerificationResponse getVerificationById(UUID verificationId);

    List<LoadVerificationResponse> getVerifications(UUID organizationId, UUID branchId, String status, String date);

    // ── Submódulo 4: Reclamos y KPIs ──
    QualityClaimResponse createClaim(UUID organizationId, UUID branchId, UUID userId, CreateQualityClaimRequest request);

    List<QualityClaimResponse> getClaims(UUID organizationId, UUID branchId, String stage);

    QualityDashboardKpisResponse getDashboardKpis(UUID organizationId, UUID branchId);
}
