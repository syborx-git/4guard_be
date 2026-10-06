package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.enums.DetectionStage;
import com.fourguard.wms.domain.enums.IncidenceStatus;
import com.fourguard.wms.domain.enums.LoadVerificationStatus;
import com.fourguard.wms.domain.enums.ReleaseDestination;
import com.fourguard.wms.domain.ports.out.QualityRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.IncidenceEntity;
import com.fourguard.wms.infrastructure.persistence.entity.LoadVerificationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.QualityReleaseEntity;
import com.fourguard.wms.infrastructure.persistence.repository.IncidenceJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.LoadVerificationJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.QualityReleaseJpaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QualityPersistenceAdapter implements QualityRepositoryPort {

    private final IncidenceJpaRepository incidenceRepository;
    private final QualityReleaseJpaRepository releaseRepository;
    private final LoadVerificationJpaRepository verificationRepository;
    private final com.fourguard.wms.infrastructure.persistence.repository.QualityDeviationJpaRepository deviationRepository;

    // ── 1. Bloqueos y PNC (Incidences) ─────────────────────────────────────────

    @Override
    public IncidenceEntity saveIncidence(IncidenceEntity entity) {
        IncidenceEntity saved = incidenceRepository.saveAndFlush(Objects.requireNonNull(entity));
        return incidenceRepository.findById(Objects.requireNonNull(saved.getId())).orElse(saved);
    }

    @Override
    public Optional<IncidenceEntity> findIncidenceById(UUID id) {
        return incidenceRepository.findById(Objects.requireNonNull(id));
    }

    @Override
    public Optional<IncidenceEntity> findIncidenceByFolio(Integer folio) {
        return incidenceRepository.findByFolio(folio);
    }

    @Override
    public List<IncidenceEntity> findIncidencesByBranch(UUID branchId) {
        return incidenceRepository.findByItemBranchIdOrderByCreatedAtDesc(branchId);
    }

    @Override
    public List<IncidenceEntity> findIncidencesByBranchAndStatus(UUID branchId, IncidenceStatus status) {
        return incidenceRepository.findByItemBranchIdAndStatusOrderByCreatedAtDesc(branchId, status);
    }

    @Override
    public List<IncidenceEntity> findIncidencesWithFilters(UUID branchId, DetectionStage stage, IncidenceStatus status) {
        if (stage != null && status != null) {
            return incidenceRepository.findByItemBranchIdAndStatusOrderByCreatedAtDesc(branchId, status).stream()
                    .filter(i -> stage.equals(i.getStage()))
                    .toList();
        } else if (stage != null) {
            return incidenceRepository.findByItemBranchIdAndStageOrderByCreatedAtDesc(branchId, stage);
        } else if (status != null) {
            return incidenceRepository.findByItemBranchIdAndStatusOrderByCreatedAtDesc(branchId, status);
        }
        return incidenceRepository.findByItemBranchIdOrderByCreatedAtDesc(branchId);
    }

    // ── 2. Dictámenes de Liberación ───────────────────────────────────────────

    @Override
    public QualityReleaseEntity saveRelease(QualityReleaseEntity entity) {
        return releaseRepository.saveAndFlush(Objects.requireNonNull(entity));
    }

    @Override
    public Optional<QualityReleaseEntity> findReleaseById(UUID id) {
        return releaseRepository.findById(Objects.requireNonNull(id));
    }

    @Override
    public Optional<QualityReleaseEntity> findReleaseByFolio(String folio) {
        return releaseRepository.findByFolio(folio);
    }

    @Override
    public Optional<QualityReleaseEntity> findReleaseByIncidenceId(UUID incidenceId) {
        return releaseRepository.findByIncidenceId(incidenceId);
    }

    @Override
    public List<QualityReleaseEntity> findReleasesByBranch(UUID branchId, ReleaseDestination destination) {
        if (destination != null) {
            return releaseRepository.findByBranchIdAndDestinationOrderByCreatedAtDesc(branchId, destination);
        }
        return releaseRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
    }

    @Override
    public String generateNextReleaseFolio(UUID organizationId) {
        long count = releaseRepository.countByOrganizationId(organizationId);
        int currentYear = Year.now().getValue();
        return String.format("LIB-%d-%04d", currentYear, count + 1);
    }

    // ── 3. Verificación de Carga F01 ───────────────────────────────────────────

    @Override
    public LoadVerificationEntity saveVerification(LoadVerificationEntity entity) {
        return verificationRepository.saveAndFlush(Objects.requireNonNull(entity));
    }

    @Override
    public Optional<LoadVerificationEntity> findVerificationById(UUID id) {
        return verificationRepository.findById(Objects.requireNonNull(id));
    }

    @Override
    public Optional<LoadVerificationEntity> findVerificationByFolio(String folio) {
        return verificationRepository.findByFolio(folio);
    }

    @Override
    public Optional<LoadVerificationEntity> findVerificationByRemision(UUID branchId, String remisionNumber) {
        return verificationRepository.findByBranchIdAndRemisionNumber(branchId, remisionNumber);
    }

    @Override
    public List<LoadVerificationEntity> findVerificationsByBranch(UUID branchId, LoadVerificationStatus status, LocalDate date) {
        if (status != null) {
            return verificationRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, status);
        }
        if (date != null) {
            return verificationRepository.findByBranchIdAndVerificationDateOrderByCreatedAtDesc(branchId, date);
        }
        return verificationRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
    }

    @Override
    public String generateNextVerificationFolio(UUID organizationId) {
        long count = verificationRepository.countByOrganizationId(organizationId);
        int currentYear = Year.now().getValue();
        return String.format("VER-%d-%04d", currentYear, count + 1);
    }

    // ── 4. Reclamos e Incidencias ─────────────────────────────────────────────
    @Override
    public List<IncidenceEntity> findClaimsByBranch(UUID branchId, DetectionStage stage, LocalDate startDate, LocalDate endDate) {
        List<IncidenceEntity> list = incidenceRepository.findByItemBranchIdOrderByCreatedAtDesc(branchId);
        return list.stream()
                .filter(i -> stage == null || stage.equals(i.getStage()))
                .filter(i -> startDate == null || (i.getCreatedAt() != null && !i.getCreatedAt().toLocalDate().isBefore(startDate)))
                .filter(i -> endDate == null || (i.getCreatedAt() != null && !i.getCreatedAt().toLocalDate().isAfter(endDate)))
                .toList();
    }

    // ── 5. Desviaciones de Calidad (Nativas & KPIs) ───────────────────────────
    @Override
    public com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity saveDeviation(
            com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity entity) {
        return deviationRepository.saveAndFlush(Objects.requireNonNull(entity));
    }

    @Override
    public Optional<com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity> findDeviationById(UUID id) {
        return deviationRepository.findById(Objects.requireNonNull(id));
    }

    @Override
    public Optional<com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity> findDeviationByFolio(String folio) {
        return deviationRepository.findByFolio(folio);
    }

    @Override
    public List<QualityDeviationEntity> findDeviationsByBranch(
            UUID branchId, String materialType, String rootCause, LocalDate startDate, LocalDate endDate) {
        Specification<QualityDeviationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }
            if (materialType != null && !materialType.isBlank() && !"ALL".equalsIgnoreCase(materialType)) {
                predicates.add(cb.equal(root.get("materialType"), materialType));
            }
            if (rootCause != null && !rootCause.isBlank() && !"ALL".equalsIgnoreCase(rootCause)) {
                predicates.add(cb.equal(root.get("rootCauseMotive"), rootCause));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deviationDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deviationDate"), endDate));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("deviationDate")), cb.desc(root.get("createdAt")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return deviationRepository.findAll(spec);
    }

    @Override
    public String generateNextDeviationFolio(UUID organizationId) {
        long count = deviationRepository.countByOrganizationId(organizationId);
        int currentYear = Year.now().getValue();
        return String.format("DEV-%d-%04d", currentYear, count + 1);
    }
}
