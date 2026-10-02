package com.fourguard.wms.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourguard.wms.application.dto.request.quality.*;
import com.fourguard.wms.application.dto.response.quality.*;
import com.fourguard.wms.domain.enums.*;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.model.quality.QualityAlertEvent;
import com.fourguard.wms.domain.ports.in.QualityUseCase;
import com.fourguard.wms.domain.ports.out.*;
import com.fourguard.wms.infrastructure.notification.QualityAlertBroadcaster;
import com.fourguard.wms.infrastructure.persistence.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QualityService implements QualityUseCase {

    private final QualityRepositoryPort qualityRepository;
    private final InventoryItemRepositoryPort inventoryItemRepository;
    private final InventoryMovementRepositoryPort inventoryMovementRepository;
    private final InventoryAuditLogRepositoryPort inventoryAuditLogRepository;
    private final LocationRepositoryPort locationRepository;
    private final WarehouseOutboundRepositoryPort warehouseOutboundRepository;
    private final UserRepositoryPort userRepository;
    private final OrganizationRepositoryPort organizationRepository;
    private final BranchRepositoryPort branchRepository;
    private final ObjectMapper objectMapper;
    private final QualityAlertBroadcaster qualityAlertBroadcaster;

    // ══════════════════════════════════════════════════════════════════════════
    // 1. SUBMÓDULO: BLOQUEOS Y PRODUCTO NO CONFORME (PNC)
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public QualityBlockResponse createBlock(UUID organizationId, UUID branchId, UUID userId, CreateQualityBlockRequest request) {
        log.info("Creating quality block for item: {}, branch: {}", request.getItemId(), branchId);

        InventoryItemEntity item = inventoryItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("Tarima / Item no encontrado con ID: " + request.getItemId()));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));

        // 1. Conmutar estado de inventario a IN_QUALITY (20)
        item.setState(InventoryState.IN_QUALITY);
        item.setQuarantineReason(request.getNotes());

        LocationEntity fromLocation = item.getLocation();
        if (request.getTargetLocationId() != null) {
            LocationEntity targetLocation = locationRepository.findById(request.getTargetLocationId())
                    .orElseThrow(() -> new EntityNotFoundException("Ubicación destino QM no encontrada: " + request.getTargetLocationId()));
            if (fromLocation != null) {
                locationRepository.decrementOccupancy(fromLocation.getId(), 1);
            }
            locationRepository.incrementOccupancy(targetLocation.getId(), 1);
            item.setLocation(targetLocation);
        }

        inventoryItemRepository.save(item);

        // 2. Mapear severidad semafórica
        IncidenceSeverity incidenceSeverity;
        try {
            incidenceSeverity = IncidenceSeverity.valueOf(request.getSeverity().getDbValue());
        } catch (Exception e) {
            incidenceSeverity = IncidenceSeverity.RED;
        }

        // 3. Crear IncidenceEntity
        IncidenceEntity incidence = IncidenceEntity.builder()
                .item(item)
                .type(IncidenceType.DAMAGE)
                .severity(incidenceSeverity)
                .reportedBy(user)
                .status(IncidenceStatus.OPEN)
                .stage(request.getStage())
                .defectCategory(request.getDefectCategory())
                .damagedQty(request.getQuantity())
                .observations(request.getNotes())
                .criteriaMetadata(toJson(request.getDefectCriteria()))
                .evidenceMetadata(toJson(request.getEvidenceFiles()))
                .build();

        IncidenceEntity savedIncidence = qualityRepository.saveIncidence(incidence);

        // 4. Asentar movimiento en Kardex inmutable
        InventoryMovementEntity movement = InventoryMovementEntity.builder()
                .item(item)
                .fromLocation(fromLocation)
                .toLocation(item.getLocation())
                .user(user)
                .type(MovementType.QUARANTINE)
                .reason(request.getNotes())
                .build();
        inventoryMovementRepository.save(movement);

        // 5. Asentar evento granular en Árbol de la Vida
        InventoryAuditLogEntity audit = InventoryAuditLogEntity.builder()
                .organization(item.getOrganization())
                .inventoryItem(item)
                .palletCode(item.getSscc() != null ? item.getSscc() : item.getId().toString())
                .remisionFolio(item.getSapFolio())
                .eventType("QM_BLOCK_PLACED")
                .sourceLocation(fromLocation != null ? fromLocation.getCode() : "N/A")
                .targetLocation(item.getLocation() != null ? item.getLocation().getCode() : "N/A")
                .performedBy(user.getFullName())
                .reason(request.getNotes())
                .build();
        inventoryAuditLogRepository.save(audit);

        // 6. Transmitir alerta reactiva en tiempo real a Terminales RF
        try {
            qualityAlertBroadcaster.broadcast(QualityAlertEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("QM_BLOCK_ALERT")
                    .severity(savedIncidence.getSeverity() != null ? savedIncidence.getSeverity().name() : "CRITICAL")
                    .sscc(item.getSscc() != null ? item.getSscc() : item.getId().toString())
                    .palletFolio("BLQ-" + savedIncidence.getId().toString().substring(0, 8).toUpperCase())
                    .sku(item.getSku() != null ? item.getSku().getCode() : "N/A")
                    .productName(item.getSku() != null ? item.getSku().getName() : "N/A")
                    .locationCode(item.getLocation() != null ? item.getLocation().getCode() : "N/A")
                    .reason(request.getNotes() != null ? request.getNotes() : "Retención por no conformidad de Calidad")
                    .recommendedAction("NO MOVER ni despachar. Traslado exclusivo a Bahía QM.")
                    .requiredInstruction("IT01-PO-GC-8.6-01")
                    .timestamp(OffsetDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Error transmitiendo alerta RF de bloqueo: {}", e.getMessage());
        }

        return mapToBlockResponse(savedIncidence);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityBlockResponse> getActiveBlocks(UUID organizationId, UUID branchId, String stageStr, String statusStr) {
        DetectionStage stage = parseEnum(DetectionStage.class, stageStr);
        IncidenceStatus status = parseEnum(IncidenceStatus.class, statusStr);

        List<IncidenceEntity> list = qualityRepository.findIncidencesWithFilters(branchId, stage, status);
        return list.stream()
                .filter(i -> i.getStatus() != IncidenceStatus.CLOSED)
                .map(this::mapToBlockResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QualityBlockResponse getBlockById(UUID blockId) {
        IncidenceEntity incidence = qualityRepository.findIncidenceById(blockId)
                .orElseThrow(() -> new EntityNotFoundException("Bloqueo de Calidad no encontrado con ID: " + blockId));
        return mapToBlockResponse(incidence);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. SUBMÓDULO: DICTAMEN DE LIBERACIONES Y DESTINOS
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public QualityReleaseResponse releaseBlock(UUID organizationId, UUID branchId, UUID userId, CreateQualityReleaseRequest request) {
        log.info("Processing release for block: {}, destination: {}", request.getBlockId(), request.getDestination());

        IncidenceEntity incidence = qualityRepository.findIncidenceById(request.getBlockId())
                .orElseThrow(() -> new EntityNotFoundException("Bloqueo no encontrado con ID: " + request.getBlockId()));

        if (incidence.getStatus() == IncidenceStatus.CLOSED) {
            throw new ValidationException("El bloqueo ya ha sido liberado o dictaminado previamente.");
        }

        InventoryItemEntity item = incidence.getItem();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autorizador no encontrado con ID: " + userId));

        // 1. Conmutar estado de inventario según destino
        MovementType movementType;
        if (request.getDestination() == ReleaseDestination.DISTRIBUTION) {
            item.setState(InventoryState.AVAILABLE);
            movementType = MovementType.RELEASE;
        } else if (request.getDestination() == ReleaseDestination.DESTRUCTION) {
            item.setState(InventoryState.DAMAGED);
            movementType = MovementType.ADJUSTMENT;
        } else {
            item.setState(InventoryState.RETURNED);
            movementType = MovementType.RETURN;
        }

        item.setQuarantineReason(null);
        inventoryItemRepository.save(item);

        // 2. Cerrar incidencia
        incidence.setStatus(IncidenceStatus.CLOSED);
        qualityRepository.saveIncidence(incidence);

        // 3. Generar folio formal de liberación
        String folio = qualityRepository.generateNextReleaseFolio(organizationId);

        // 4. Crear entidad QualityReleaseEntity
        QualityReleaseEntity release = QualityReleaseEntity.builder()
                .organization(item.getOrganization())
                .branch(item.getBranch())
                .folio(folio)
                .incidence(incidence)
                .item(item)
                .authorizerType(request.getAuthorizerType())
                .supportType(request.getSupportType())
                .supportCustomType(request.getSupportCustomType())
                .supportSubject(request.getSupportSubject())
                .supportFileName(request.getSupportFileName())
                .authorizedByName(request.getAuthorizedByName())
                .authorizedByPosition(request.getAuthorizedByPosition())
                .destination(request.getDestination())
                .decisionNotes(request.getDecisionNotes())
                .releasedByUser(user)
                .evidenceMetadata(toJson(request.getEvidenceFiles()))
                .build();

        QualityReleaseEntity savedRelease = qualityRepository.saveRelease(release);

        // 5. Asentar movimiento en Kardex
        InventoryMovementEntity movement = InventoryMovementEntity.builder()
                .item(item)
                .fromLocation(item.getLocation())
                .toLocation(item.getLocation())
                .user(user)
                .type(movementType)
                .reason(request.getDecisionNotes())
                .build();
        inventoryMovementRepository.save(movement);

        // 6. Asentar auditoría forense
        InventoryAuditLogEntity audit = InventoryAuditLogEntity.builder()
                .organization(item.getOrganization())
                .inventoryItem(item)
                .palletCode(item.getSscc() != null ? item.getSscc() : item.getId().toString())
                .remisionFolio(item.getSapFolio())
                .eventType("QM_RELEASE_APPROVED")
                .sourceLocation(item.getLocation() != null ? item.getLocation().getCode() : "N/A")
                .targetLocation(item.getLocation() != null ? item.getLocation().getCode() : "N/A")
                .performedBy(user.getFullName())
                .reason(request.getDecisionNotes())
                .build();
        inventoryAuditLogRepository.save(audit);

        // 7. Transmitir evento reactivo de liberación a Terminales RF
        try {
            qualityAlertBroadcaster.broadcast(QualityAlertEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("QM_RELEASE_AUTHORIZED")
                    .severity("INFO")
                    .sscc(item.getSscc() != null ? item.getSscc() : item.getId().toString())
                    .palletFolio(savedRelease.getFolio())
                    .sku(item.getSku() != null ? item.getSku().getCode() : "N/A")
                    .productName(item.getSku() != null ? item.getSku().getName() : "N/A")
                    .locationCode(item.getLocation() != null ? item.getLocation().getCode() : "N/A")
                    .reason("Liberación autorizada por " + savedRelease.getAuthorizedByName() + " (" + savedRelease.getDestination() + ")")
                    .recommendedAction("Tarima habilitada para operaciones de " + savedRelease.getDestination())
                    .requiredInstruction("PO-GC-8.6-03")
                    .timestamp(OffsetDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Error transmitiendo alerta RF de liberación: {}", e.getMessage());
        }

        return mapToReleaseResponse(savedRelease);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityReleaseResponse> getReleasesHistory(UUID organizationId, UUID branchId, String destinationStr) {
        ReleaseDestination destination = parseEnum(ReleaseDestination.class, destinationStr);
        List<QualityReleaseEntity> releases = qualityRepository.findReleasesByBranch(branchId, destination);
        return releases.stream().map(this::mapToReleaseResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QualityReleaseResponse getReleaseById(UUID releaseId) {
        QualityReleaseEntity release = qualityRepository.findReleaseById(releaseId)
                .orElseThrow(() -> new EntityNotFoundException("Dictamen de liberación no encontrado con ID: " + releaseId));
        return mapToReleaseResponse(release);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. SUBMÓDULO: VERIFICACIÓN DE CARGA F01-PO-GC-8.6-03
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public LoadVerificationResponse createOrUpdateVerification(UUID organizationId, UUID branchId, UUID userId, SaveLoadVerificationRequest request) {
        log.info("Saving load verification for remision: {}, branch: {}", request.getRemisionNumber(), branchId);

        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada: " + organizationId));
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada: " + branchId));

        WarehouseOutboundEntity outbound = null;
        if (request.getOutboundId() != null) {
            outbound = warehouseOutboundRepository.findById(request.getOutboundId()).orElse(null);
        }

        LoadVerificationEntity entity;
        if (request.getId() != null) {
            entity = qualityRepository.findVerificationById(request.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Verificación no encontrada con ID: " + request.getId()));
        } else {
            String folio = qualityRepository.generateNextVerificationFolio(organizationId);
            entity = LoadVerificationEntity.builder()
                    .organization(org)
                    .branch(branch)
                    .folio(folio)
                    .build();
        }

        entity.setOutbound(outbound);
        entity.setRemisionNumber(request.getRemisionNumber());
        entity.setProductDescription(request.getProductDescription());
        entity.setClientName(request.getClientName());
        entity.setVerificationDate(request.getDate());
        entity.setVerificationTime(request.getTime());
        entity.setRampCode(request.getRamp());
        entity.setStatus(request.getStatus());
        entity.setProductCriteria(toJson(request.getProductCriteria()));
        entity.setTransportCriteria(toJson(request.getTransportCriteria()));
        entity.setSignatures(toJson(request.getSignatures()));
        entity.setGeneralObservations(request.getGeneralObservations());
        entity.setEvidenceMetadata(toJson(request.getEvidencePhotos()));

        LoadVerificationEntity saved = qualityRepository.saveVerification(entity);

        // 8. Transmitir alerta si la verificación requiere acondicionamiento o rechazo
        if (saved.getStatus() == LoadVerificationStatus.LIMPIEZA_PENDIENTE ||
            saved.getStatus() == LoadVerificationStatus.ACONDICIONAMIENTO_PENDIENTE ||
            saved.getStatus() == LoadVerificationStatus.RECHAZADO) {
            try {
                qualityAlertBroadcaster.broadcast(QualityAlertEvent.builder()
                        .eventId(UUID.randomUUID())
                        .eventType("QM_CONDITIONING_REQUIRED")
                        .severity(saved.getStatus() == LoadVerificationStatus.RECHAZADO ? "CRITICAL" : "WARNING")
                        .palletFolio(saved.getFolio())
                        .locationCode(saved.getRampCode())
                        .reason("Verificación de Carga " + saved.getFolio() + " no autorizada (" + saved.getStatus().name() + ")")
                        .recommendedAction("Detener maniobras en Rampa " + saved.getRampCode() + ". Aplicar acondicionamiento/limpieza inmediata.")
                        .requiredInstruction("IT02-PO-GC-8.6-02")
                        .timestamp(OffsetDateTime.now())
                        .build());
            } catch (Exception e) {
                log.error("Error transmitiendo alerta RF de verificación: {}", e.getMessage());
            }
        }

        return mapToVerificationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoadVerificationResponse getVerificationById(UUID verificationId) {
        LoadVerificationEntity entity = qualityRepository.findVerificationById(verificationId)
                .orElseThrow(() -> new EntityNotFoundException("Verificación no encontrada con ID: " + verificationId));
        return mapToVerificationResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoadVerificationResponse> getVerifications(UUID organizationId, UUID branchId, String statusStr, String dateStr) {
        LoadVerificationStatus status = parseEnum(LoadVerificationStatus.class, statusStr);
        LocalDate date = (dateStr != null && !dateStr.isBlank()) ? LocalDate.parse(dateStr) : null;

        List<LoadVerificationEntity> list = qualityRepository.findVerificationsByBranch(branchId, status, date);
        return list.stream().map(this::mapToVerificationResponse).toList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. SUBMÓDULO: RECLAMOS E INCIDENCIAS Y KPIS
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public QualityClaimResponse createClaim(UUID organizationId, UUID branchId, UUID userId, CreateQualityClaimRequest request) {
        log.info("Creating quality claim for SKU: {}, client: {}", request.getSku(), request.getClientName());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));

        InventoryItemEntity item = null;
        if (request.getItemId() != null) {
            item = inventoryItemRepository.findById(request.getItemId()).orElse(null);
        }

        IncidenceEntity incidence = IncidenceEntity.builder()
                .item(item)
                .type(IncidenceType.OTHER)
                .severity(IncidenceSeverity.YELLOW)
                .reportedBy(user)
                .status(IncidenceStatus.OPEN)
                .stage(request.getStage())
                .defectCategory(DefectCategory.MATERIAL)
                .damagedQty(request.getDamagedQty() != null ? request.getDamagedQty() : BigDecimal.ZERO)
                .lostQty(request.getLostQty() != null ? request.getLostQty() : BigDecimal.ZERO)
                .associatedCost(request.getAssociatedCost() != null ? request.getAssociatedCost() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "MXN")
                .observations(request.getObservations())
                .evidenceMetadata(toJson(request.getEvidenceFiles()))
                .build();

        IncidenceEntity saved = qualityRepository.saveIncidence(incidence);
        return mapToClaimResponse(saved, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityClaimResponse> getClaims(UUID organizationId, UUID branchId, String stageStr) {
        DetectionStage stage = parseEnum(DetectionStage.class, stageStr);
        List<IncidenceEntity> incidences = qualityRepository.findClaimsByBranch(branchId, stage, null, null);
        return incidences.stream().map(i -> mapToClaimResponse(i, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QualityDashboardKpisResponse getDashboardKpis(UUID organizationId, UUID branchId) {
        List<IncidenceEntity> blocks = qualityRepository.findIncidencesByBranch(branchId);
        List<QualityReleaseEntity> releases = qualityRepository.findReleasesByBranch(branchId, null);
        List<LoadVerificationEntity> verifs = qualityRepository.findVerificationsByBranch(branchId, null, null);

        long totalActiveBlocks = blocks.stream().filter(b -> b.getStatus() != IncidenceStatus.CLOSED).count();
        long totalBlocked = blocks.stream().filter(b -> b.getStatus() == IncidenceStatus.OPEN).count();
        long totalUnderInspection = blocks.stream().filter(b -> b.getStatus() == IncidenceStatus.IN_PROGRESS).count();

        long totalReleases = releases.size();
        long distReleases = releases.stream().filter(r -> r.getDestination() == ReleaseDestination.DISTRIBUTION).count();
        long destReleases = releases.stream().filter(r -> r.getDestination() == ReleaseDestination.DESTRUCTION).count();
        long retReleases = releases.stream().filter(r -> r.getDestination() == ReleaseDestination.RETURN).count();

        long totalVerifs = verifs.size();
        long approvedVerifs = verifs.stream().filter(v -> v.getStatus() == LoadVerificationStatus.APROBADO).count();
        long pendingVerifs = verifs.stream().filter(v -> v.getStatus() != LoadVerificationStatus.APROBADO).count();

        BigDecimal totalDamaged = blocks.stream()
                .map(i -> i.getDamagedQty() != null ? i.getDamagedQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (acc, val) -> acc.add(val));

        BigDecimal totalLost = blocks.stream()
                .map(i -> i.getLostQty() != null ? i.getLostQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (acc, val) -> acc.add(val));

        BigDecimal totalCost = blocks.stream()
                .map(i -> i.getAssociatedCost() != null ? i.getAssociatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (acc, val) -> acc.add(val));

        return QualityDashboardKpisResponse.builder()
                .totalActiveBlocks(totalActiveBlocks)
                .totalBlocked(totalBlocked)
                .totalUnderInspection(totalUnderInspection)
                .totalReleases(totalReleases)
                .distributionReleases(distReleases)
                .destructionReleases(destReleases)
                .returnReleases(retReleases)
                .totalVerifications(totalVerifs)
                .approvedVerifications(approvedVerifs)
                .pendingVerifications(pendingVerifs)
                .totalClaims(blocks.size())
                .totalDamagedQty(totalDamaged)
                .totalLostQty(totalLost)
                .totalClaimsCost(totalCost)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAPPERS Y HELPERS INTERNOS
    // ══════════════════════════════════════════════════════════════════════════

    private QualityBlockResponse mapToBlockResponse(IncidenceEntity entity) {
        InventoryItemEntity item = entity.getItem();
        String folio = entity.getFolio() != null ? String.format("BLQ-2026-%04d", entity.getFolio()) : "BLQ-PENDING";

        List<String> criteria = fromJson(entity.getCriteriaMetadata(), new TypeReference<>() {});
        List<EvidenceFileDto> evidences = fromJson(entity.getEvidenceMetadata(), new TypeReference<>() {});

        String feStatus = entity.getStatus() == IncidenceStatus.CLOSED ? "RELEASED"
                : (entity.getStatus() == IncidenceStatus.IN_PROGRESS ? "UNDER_INSPECTION" : "BLOCKED");

        QualitySeverity severity = entity.getSeverity() != null
                ? QualitySeverity.fromDb(entity.getSeverity().name())
                : QualitySeverity.INFO;

        return QualityBlockResponse.builder()
                .id(entity.getId())
                .folio(folio)
                .itemId(item != null ? item.getId() : null)
                .sscc(item != null ? item.getSscc() : "")
                .sku(item != null && item.getSku() != null ? item.getSku().getCode() : "")
                .skuDescription(item != null && item.getSku() != null ? item.getSku().getDescription() : "")
                .clientName(item != null && item.getClient() != null ? item.getClient().getName() : "")
                .batchNumber(item != null ? item.getBatchNumber() : "")
                .quantity(entity.getDamagedQty() != null ? entity.getDamagedQty() : (item != null ? item.getQuantity() : BigDecimal.ZERO))
                .unitOfMeasure(item != null && item.getSku() != null ? item.getSku().getUnit() : "BOX")
                .locationCode(item != null && item.getLocation() != null ? item.getLocation().getCode() : "")
                .stage(entity.getStage() != null ? entity.getStage() : DetectionStage.STORAGE)
                .defectCategory(entity.getDefectCategory() != null ? entity.getDefectCategory() : DefectCategory.MATERIAL)
                .defectCriteria(criteria != null ? criteria : new ArrayList<>())
                .severity(severity)
                .status(feStatus)
                .reportedByName(entity.getReportedBy() != null ? entity.getReportedBy().getFullName() : "")
                .reportedAt(entity.getCreatedAt())
                .notes(entity.getObservations() != null ? entity.getObservations() : "")
                .evidenceFiles(evidences != null ? evidences : new ArrayList<>())
                .build();
    }

    private QualityReleaseResponse mapToReleaseResponse(QualityReleaseEntity entity) {
        InventoryItemEntity item = entity.getItem();
        List<EvidenceFileDto> evidences = fromJson(entity.getEvidenceMetadata(), new TypeReference<>() {});

        String blockFolio = entity.getIncidence() != null && entity.getIncidence().getFolio() != null
                ? String.format("BLQ-2026-%04d", entity.getIncidence().getFolio()) : "";

        return QualityReleaseResponse.builder()
                .id(entity.getId())
                .folio(entity.getFolio())
                .blockId(entity.getIncidence() != null ? entity.getIncidence().getId() : null)
                .blockFolio(blockFolio)
                .sku(item != null && item.getSku() != null ? item.getSku().getCode() : "")
                .description(item != null && item.getSku() != null ? item.getSku().getDescription() : "")
                .batchNumber(item != null ? item.getBatchNumber() : "")
                .clientName(item != null && item.getClient() != null ? item.getClient().getName() : "")
                .quantity(item != null ? item.getQuantity() : BigDecimal.ZERO)
                .unitOfMeasure(item != null && item.getSku() != null ? item.getSku().getUnit() : "BOX")
                .authorizerType(entity.getAuthorizerType())
                .supportType(entity.getSupportType())
                .supportCustomType(entity.getSupportCustomType())
                .supportSubject(entity.getSupportSubject())
                .supportFileName(entity.getSupportFileName())
                .authorizedByName(entity.getAuthorizedByName())
                .authorizedByPosition(entity.getAuthorizedByPosition())
                .destination(entity.getDestination())
                .decisionNotes(entity.getDecisionNotes())
                .releasedByUserName(entity.getReleasedByUser() != null ? entity.getReleasedByUser().getFullName() : "")
                .releasedAt(entity.getCreatedAt())
                .evidenceFiles(evidences != null ? evidences : new ArrayList<>())
                .build();
    }

    private LoadVerificationResponse mapToVerificationResponse(LoadVerificationEntity entity) {
        List<VerificationCriterionDto> prodCriteria = fromJson(entity.getProductCriteria(), new TypeReference<>() {});
        List<VerificationCriterionDto> transCriteria = fromJson(entity.getTransportCriteria(), new TypeReference<>() {});
        VerificationSignaturesDto signatures = fromJson(entity.getSignatures(), new TypeReference<>() {});
        List<EvidenceFileDto> photos = fromJson(entity.getEvidenceMetadata(), new TypeReference<>() {});

        return LoadVerificationResponse.builder()
                .id(entity.getId())
                .folio(entity.getFolio())
                .controlNumber(entity.getControlNumber())
                .revisionNumber(entity.getRevisionNumber())
                .processName("Liberación de carga")
                .ownerDepartment("Seguridad e Inocuidad / Calidad")
                .outboundId(entity.getOutbound() != null ? entity.getOutbound().getId() : null)
                .receptionId(entity.getReception() != null ? entity.getReception().getId() : null)
                .remisionNumber(entity.getRemisionNumber())
                .productDescription(entity.getProductDescription())
                .clientName(entity.getClientName())
                .date(entity.getVerificationDate())
                .time(entity.getVerificationTime())
                .ramp(entity.getRampCode())
                .status(entity.getStatus())
                .productCriteria(prodCriteria != null ? prodCriteria : new ArrayList<>())
                .transportCriteria(transCriteria != null ? transCriteria : new ArrayList<>())
                .signatures(signatures != null ? signatures : new VerificationSignaturesDto())
                .generalObservations(entity.getGeneralObservations())
                .evidencePhotos(photos != null ? photos : new ArrayList<>())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private QualityClaimResponse mapToClaimResponse(IncidenceEntity entity, CreateQualityClaimRequest req) {
        InventoryItemEntity item = entity.getItem();
        String folio = entity.getFolio() != null ? String.format("REC-2026-%04d", entity.getFolio()) : "REC-PENDING";
        List<EvidenceFileDto> evidences = fromJson(entity.getEvidenceMetadata(), new TypeReference<>() {});

        return QualityClaimResponse.builder()
                .id(entity.getId())
                .folio(folio)
                .date(entity.getCreatedAt() != null ? entity.getCreatedAt().toLocalDate() : (req != null ? req.getDate() : LocalDate.now()))
                .time(entity.getCreatedAt() != null ? entity.getCreatedAt().toLocalTime() : (req != null ? req.getTime() : null))
                .stage(entity.getStage() != null ? entity.getStage() : (req != null ? req.getStage() : DetectionStage.STORAGE))
                .sku(item != null && item.getSku() != null ? item.getSku().getCode() : (req != null ? req.getSku() : ""))
                .productDescription(item != null && item.getSku() != null ? item.getSku().getDescription() : (req != null ? req.getProductDescription() : ""))
                .clientName(item != null && item.getClient() != null ? item.getClient().getName() : (req != null ? req.getClientName() : ""))
                .batchNumber(item != null ? item.getBatchNumber() : (req != null ? req.getBatchNumber() : ""))
                .remisionNumber(item != null ? item.getSapFolio() : (req != null ? req.getRemisionNumber() : ""))
                .defectType(req != null ? req.getDefectType() : "MATERIAL_DEFECT")
                .defectCustomType(req != null ? req.getDefectCustomType() : "")
                .damagedQty(entity.getDamagedQty() != null ? entity.getDamagedQty() : BigDecimal.ZERO)
                .lostQty(entity.getLostQty() != null ? entity.getLostQty() : BigDecimal.ZERO)
                .associatedCost(entity.getAssociatedCost() != null ? entity.getAssociatedCost() : BigDecimal.ZERO)
                .currency(entity.getCurrency() != null ? entity.getCurrency() : "MXN")
                .authorizedByName(entity.getReportedBy() != null ? entity.getReportedBy().getFullName() : (req != null ? req.getAuthorizedByName() : ""))
                .authorizedByPosition(req != null ? req.getAuthorizedByPosition() : "")
                .observations(entity.getObservations())
                .evidenceFiles(evidences != null ? evidences : new ArrayList<>())
                .status(entity.getStatus() != null ? entity.getStatus().name() : "OPEN")
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getCreatedAt())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Error serializing to JSON", e);
            return "[]";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank() || "[]".equals(json)) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("Error deserializing from JSON: {}", json, e);
            return null;
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String val) {
        if (val == null || val.isBlank() || "ALL".equalsIgnoreCase(val)) return null;
        try {
            return Enum.valueOf(enumClass, val.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
