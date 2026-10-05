package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.domain.enums.DetectionStage;
import com.fourguard.wms.domain.enums.IncidenceStatus;
import com.fourguard.wms.domain.enums.LoadVerificationStatus;
import com.fourguard.wms.domain.enums.ReleaseDestination;
import com.fourguard.wms.infrastructure.persistence.entity.IncidenceEntity;
import com.fourguard.wms.infrastructure.persistence.entity.LoadVerificationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.QualityReleaseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port OUT — Contrato de persistencia para el módulo de Calidad QM.
 */
public interface QualityRepositoryPort {

    // ── 1. Bloqueos y PNC (Incidences) ─────────────────────────────────────────
    IncidenceEntity saveIncidence(IncidenceEntity entity);

    Optional<IncidenceEntity> findIncidenceById(UUID id);

    Optional<IncidenceEntity> findIncidenceByFolio(Integer folio);

    List<IncidenceEntity> findIncidencesByBranch(UUID branchId);

    List<IncidenceEntity> findIncidencesByBranchAndStatus(UUID branchId, IncidenceStatus status);

    List<IncidenceEntity> findIncidencesWithFilters(UUID branchId, DetectionStage stage, IncidenceStatus status);

    // ── 2. Dictámenes de Liberación ───────────────────────────────────────────
    QualityReleaseEntity saveRelease(QualityReleaseEntity entity);

    Optional<QualityReleaseEntity> findReleaseById(UUID id);

    Optional<QualityReleaseEntity> findReleaseByFolio(String folio);

    Optional<QualityReleaseEntity> findReleaseByIncidenceId(UUID incidenceId);

    List<QualityReleaseEntity> findReleasesByBranch(UUID branchId, ReleaseDestination destination);

    String generateNextReleaseFolio(UUID organizationId);

    // ── 3. Verificación de Carga F01 ───────────────────────────────────────────
    LoadVerificationEntity saveVerification(LoadVerificationEntity entity);

    Optional<LoadVerificationEntity> findVerificationById(UUID id);

    Optional<LoadVerificationEntity> findVerificationByFolio(String folio);

    Optional<LoadVerificationEntity> findVerificationByRemision(UUID branchId, String remisionNumber);

    List<LoadVerificationEntity> findVerificationsByBranch(UUID branchId, LoadVerificationStatus status, LocalDate date);

    String generateNextVerificationFolio(UUID organizationId);

    // ── 4. Reclamos e Incidencias ─────────────────────────────────────────────
    List<IncidenceEntity> findClaimsByBranch(UUID branchId, DetectionStage stage, LocalDate startDate, LocalDate endDate);

    // ── 5. Desviaciones de Calidad (Nativas & KPIs) ───────────────────────────
    com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity saveDeviation(
            com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity entity);

    Optional<com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity> findDeviationById(UUID id);

    Optional<com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity> findDeviationByFolio(String folio);

    List<com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity> findDeviationsByBranch(
            UUID branchId, String materialType, String rootCause, LocalDate startDate, LocalDate endDate);

    String generateNextDeviationFolio(UUID organizationId);
}
