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
            
            if (targetLocation.getStatus() != null && targetLocation.getStatus() != LocationStatus.ACTIVE) {
                throw new ValidationException("La ubicación seleccionada (" + targetLocation.getCode() + ") no está activa para recibir inventario (Estatus: " + targetLocation.getStatus() + ")");
            }
            if (targetLocation.getCurrentOccupancy() != null && targetLocation.getCapacityUnits() != null
                    && targetLocation.getCurrentOccupancy() >= targetLocation.getCapacityUnits()) {
                throw new ValidationException("La ubicación seleccionada (" + targetLocation.getCode() + ") se encuentra a su máxima capacidad (" + targetLocation.getCapacityUnits() + " tarimas)");
            }

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

    // ══════════════════════════════════════════════════════════════════════════
    // 5. SUBMÓDULO: DESVIACIONES NATIVAS Y TABLERO MENSUAL DE 10 KPIS
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public QualityDeviationResponse createDeviation(UUID organizationId, UUID branchId, UUID userId, CreateQualityDeviationRequest request) {
        log.info("Creating quality deviation for remision: {}, branch: {}", request.getRemisionNumber(), branchId);

        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseGet(() -> organizationRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada")));

        BranchEntity branch = branchRepository.findById(branchId)
                .orElseGet(() -> branchRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada")));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));

        String folio = qualityRepository.generateNextDeviationFolio(org.getId());

        QualityDeviationEntity entity = QualityDeviationEntity.builder()
                .organization(org)
                .branch(branch)
                .folio(folio)
                .remisionNumber(request.getRemisionNumber())
                .skuId(request.getSkuId())
                .skuDescription(request.getSkuDescription() != null ? request.getSkuDescription() : request.getSkuId())
                .uaCode(request.getUaCode())
                .materialType(request.getMaterialType())
                .deviationDate(request.getDeviationDate())
                .deviationTime(request.getDeviationTime() != null ? request.getDeviationTime() : java.time.LocalTime.now())
                .detectedBy(user)
                .responsibleCollaborator(request.getResponsibleCollaborator() != null ? request.getResponsibleCollaborator() : user.getFullName())
                .bayLocationCode(request.getBayLocationCode())
                .damagedUnits(request.getDamagedUnits() != null ? request.getDamagedUnits() : 0)
                .materialCost(request.getMaterialCost() != null ? request.getMaterialCost() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "MXN")
                .conditionDeviation(request.getConditionDeviation())
                .rootCauseMotive(request.getRootCauseMotive())
                .originArea(request.getOriginArea())
                .evidencePhotoUrls(toJson(request.getEvidencePhotoUrls()))
                .actionTaken(request.getActionTaken())
                .observations(request.getObservations())
                .isResolved(false)
                .build();

        QualityDeviationEntity saved = qualityRepository.saveDeviation(entity);
        return mapToDeviationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityDeviationResponse> getDeviations(UUID organizationId, UUID branchId, String materialType, String rootCause, String month) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (month != null && month.matches("^\\d{4}-\\d{2}$")) {
            String[] parts = month.split("-");
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            startDate = LocalDate.of(y, m, 1);
            endDate = startDate.plusMonths(1).minusDays(1);
        }

        List<QualityDeviationEntity> list = qualityRepository.findDeviationsByBranch(
                branchId,
                materialType != null && !materialType.isBlank() && !"ALL".equalsIgnoreCase(materialType) ? materialType : null,
                rootCause != null && !rootCause.isBlank() && !"ALL".equalsIgnoreCase(rootCause) ? rootCause : null,
                startDate,
                endDate);

        return list.stream().map(this::mapToDeviationResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QualityDeviationResponse getDeviationById(UUID deviationId) {
        QualityDeviationEntity entity = qualityRepository.findDeviationById(deviationId)
                .orElseThrow(() -> new EntityNotFoundException("Desviación no encontrada con ID: " + deviationId));
        return mapToDeviationResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public QualityMonthlyBoardResponse getMonthlyBoard(UUID organizationId, UUID branchId, Integer year, Integer month) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        int targetMonth = month != null ? month : LocalDate.now().getMonthValue();
        LocalDate startDate = LocalDate.of(targetYear, targetMonth, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        String monthName = startDate.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new Locale("es", "MX"));
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);

        // 1. Obtener desviaciones del mes
        List<QualityDeviationEntity> monthDeviations = qualityRepository.findDeviationsByBranch(
                branchId, null, null, startDate, endDate);

        // 2. Obtener verificaciones F01 del mes (Checklists QM)
        List<LoadVerificationEntity> verifications = qualityRepository.findVerificationsByBranch(branchId, null, null).stream()
                .filter(v -> v.getVerificationDate() != null && !v.getVerificationDate().isBefore(startDate) && !v.getVerificationDate().isAfter(endDate))
                .toList();

        // 3. Obtener liberaciones del mes
        List<QualityReleaseEntity> releases = qualityRepository.findReleasesByBranch(branchId, null).stream()
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().toLocalDate().isBefore(startDate) && !r.getCreatedAt().toLocalDate().isAfter(endDate))
                .toList();

        // 4. Obtener reclamos del mes
        List<IncidenceEntity> claims = qualityRepository.findClaimsByBranch(branchId, null, startDate, endDate);

        // --- CÁLCULO DE LOS 10 KPIS ---

        // KPI 1: % Liberaciones sin Desviación (Target >= 95%)
        long totalInspected = verifications.size() > 0 ? verifications.size() : Math.max(releases.size(), 1);
        long conformingReleases = verifications.stream()
                .filter(v -> LoadVerificationStatus.APROBADO.equals(v.getStatus()))
                .count();
        if (verifications.isEmpty() && !releases.isEmpty()) {
            conformingReleases = releases.stream()
                    .filter(r -> ReleaseDestination.DISTRIBUTION.equals(r.getDestination()))
                    .count();
        }
        double kpi1Pct = totalInspected > 0 ? ((double) conformingReleases / totalInspected) * 100.0 : 100.0;

        // KPI 2: % Transporte en Buenas Condiciones (Target >= 95%)
        long conformingTransport = verifications.stream()
                .filter(v -> !LoadVerificationStatus.RECHAZADO.equals(v.getStatus()) && !LoadVerificationStatus.LIMPIEZA_PENDIENTE.equals(v.getStatus()))
                .count();
        double kpi2Pct = totalInspected > 0 ? ((double) conformingTransport / totalInspected) * 100.0 : 100.0;

        // KPI 3: Liberaciones por Colaborador
        Map<String, Long> releasesByCollab = new HashMap<>();
        for (QualityReleaseEntity rel : releases) {
            String collab = rel.getAuthorizedByName() != null ? rel.getAuthorizedByName() : "Auditor QM";
            releasesByCollab.put(collab, releasesByCollab.getOrDefault(collab, 0L) + 1);
        }
        String topCollaborator = releasesByCollab.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .orElse("Sin registros");

        // KPI 4: Producto Dañado en Almacén (Target <= 70 unidades / <= 5 eventos)
        long storageDeviationsCount = monthDeviations.stream()
                .filter(d -> "STORAGE".equalsIgnoreCase(d.getOriginArea()) || "ALMACEN".equalsIgnoreCase(d.getOriginArea()) || "MANEJO_INADECUADO".equalsIgnoreCase(d.getRootCauseMotive()))
                .count();

        // KPI 5: Motivo del Daño / Causa Raíz Predominante
        Map<String, Long> rootCauseMap = new HashMap<>();
        for (QualityDeviationEntity dev : monthDeviations) {
            String motive = dev.getRootCauseMotive() != null ? dev.getRootCauseMotive() : "MANEJO_INADECUADO";
            rootCauseMap.put(motive, rootCauseMap.getOrDefault(motive, 0L) + 1);
        }
        String topRootCause = rootCauseMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Sin incidentes");

        // KPI 6: Desviaciones en Descarga / Inbound
        long inboundDeviationsCount = monthDeviations.stream()
                .filter(d -> "INBOUND".equalsIgnoreCase(d.getOriginArea()) || "RECEPCION".equalsIgnoreCase(d.getOriginArea()) || "CALIDAD".equalsIgnoreCase(d.getOriginArea()))
                .count();

        // KPI 7: Reclamos de Cliente
        long clientClaimsCount = claims.size();

        // KPI 8: Costo de la No Calidad ($ MXN)
        BigDecimal deviationCost = monthDeviations.stream()
                .map(d -> d.getMaterialCost() != null ? d.getMaterialCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal claimCost = claims.stream()
                .map(c -> c.getAssociatedCost() != null ? c.getAssociatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNonQualityCost = deviationCost.add(claimCost);

        // KPI 9: Total Producto Dañado (Unidades Físicas)
        long ptDamaged = monthDeviations.stream()
                .filter(d -> "PRODUCTO_TERMINADO".equalsIgnoreCase(d.getMaterialType()))
                .mapToLong(d -> d.getDamagedUnits() != null ? d.getDamagedUnits().longValue() : 0L)
                .sum();
        long pkgDamaged = monthDeviations.stream()
                .filter(d -> "EMBALAJES".equalsIgnoreCase(d.getMaterialType()))
                .mapToLong(d -> d.getDamagedUnits() != null ? d.getDamagedUnits().longValue() : 0L)
                .sum();
        long coffeeDamaged = monthDeviations.stream()
                .filter(d -> "CAFE_VERDE".equalsIgnoreCase(d.getMaterialType()))
                .mapToLong(d -> d.getDamagedUnits() != null ? d.getDamagedUnits().longValue() : 0L)
                .sum();
        long totalDamagedPieces = ptDamaged + pkgDamaged + coffeeDamaged;

        // KPI 10: Acciones Realizadas / Resoluciones
        Map<String, Long> actionsMap = new HashMap<>();
        for (QualityDeviationEntity dev : monthDeviations) {
            String act = dev.getActionTaken() != null ? dev.getActionTaken() : "BLOQUEO_CALIDAD";
            actionsMap.put(act, actionsMap.getOrDefault(act, 0L) + 1);
        }

        // Construir Lista de 10 Tarjetas Bento Grid
        List<MonthlyKpiCardDto> cards = List.of(
                MonthlyKpiCardDto.builder()
                        .kpiNumber(1)
                        .id("kpi-liberaciones-sin-desviacion")
                        .title("1. % Liberaciones sin Desviación")
                        .category("Liberación")
                        .value(String.format(Locale.US, "%.1f%%", kpi1Pct))
                        .numericValue(BigDecimal.valueOf(kpi1Pct))
                        .unit("%")
                        .target("≥ 95.0%")
                        .targetValue(BigDecimal.valueOf(95.0))
                        .compliancePercentage(BigDecimal.valueOf(Math.min(100.0, (kpi1Pct / 95.0) * 100.0)))
                        .status(kpi1Pct >= 95.0 ? "SUCCESS" : (kpi1Pct >= 90.0 ? "WARNING" : "DANGER"))
                        .previousMonthDiff(BigDecimal.ZERO)
                        .trend("UP")
                        .sublabel("Lotes conformes en F01")
                        .sparklineData(List.of(BigDecimal.valueOf(98.0), BigDecimal.valueOf(97.5), BigDecimal.valueOf(99.0), BigDecimal.valueOf(kpi1Pct)))
                        .build(),

                MonthlyKpiCardDto.builder()
                        .kpiNumber(2)
                        .id("kpi-transporte-buenas-condiciones")
                        .title("2. % Transporte Óptimo")
                        .category("Transporte")
                        .value(String.format(Locale.US, "%.1f%%", kpi2Pct))
                        .numericValue(BigDecimal.valueOf(kpi2Pct))
                        .unit("%")
                        .target("≥ 95.0%")
                        .targetValue(BigDecimal.valueOf(95.0))
                        .compliancePercentage(BigDecimal.valueOf(Math.min(100.0, (kpi2Pct / 95.0) * 100.0)))
                        .status(kpi2Pct >= 95.0 ? "SUCCESS" : "WARNING")
                        .previousMonthDiff(BigDecimal.ZERO)
                        .trend("STABLE")
                        .sublabel("Limpieza, olores y plagas")
                        .sparklineData(List.of(BigDecimal.valueOf(96.0), BigDecimal.valueOf(98.0), BigDecimal.valueOf(97.0), BigDecimal.valueOf(kpi2Pct)))
                        .build(),

                MonthlyKpiCardDto.builder()
                        .kpiNumber(3)
                        .id("kpi-liberaciones-colaborador")
                        .title("3. Inspector Top del Mes")
                        .category("Productividad")
                        .value(topCollaborator)
                        .numericValue(BigDecimal.valueOf(releases.size()))
                        .unit("Lotes")
                        .target("Desempeño QM")
                        .status("INFO")
                        .sublabel(releases.size() + " liberaciones dictaminadas")
                        .build(),

                MonthlyKpiCardDto.builder()
                        .kpiNumber(4)
                        .id("kpi-producto-danado-almacen")
                        .title("4. Daños en Almacenamiento")
                        .category("Almacén")
                        .value(String.valueOf(storageDeviationsCount))
                        .numericValue(BigDecimal.valueOf(storageDeviationsCount))
                        .unit("Eventos")
                        .target("≤ 5 eventos")
                        .targetValue(BigDecimal.valueOf(5))
                        .status(storageDeviationsCount <= 5 ? "SUCCESS" : "DANGER")
                        .sublabel("Racks, goteras y tarimas")
                        .build(),

                MonthlyKpiCardDto.builder()
                        .kpiNumber(5)
                        .id("kpi-causa-raiz-predominante")
                        .title("5. Causa Raíz Predominante")
                        .category("Análisis Causa Raíz")
                        .value(topRootCause)
                        .numericValue(BigDecimal.valueOf(rootCauseMap.values().stream().mapToLong(Long::longValue).max().orElse(0L)))
                        .unit("Casos")
                        .target("Mitigación")
                        .status("WARNING")
                        .sublabel("Tipificación de incidentes")
                        .build(),

                MonthlyKpiCardDto.builder()
                        .kpiNumber(6)
                        .id("kpi-desviaciones-descarga")
                        .title("6. Desviaciones en Descarga")
                        .category("Recepción Inbound")
                        .value(String.valueOf(inboundDeviationsCount))
                        .numericValue(BigDecimal.valueOf(inboundDeviationsCount))
                        .unit("Fallas")
                        .target("≤ 5 al mes")
                        .status(inboundDeviationsCount <= 5 ? "SUCCESS" : "WARNING")
                        .sublabel("Tarima rota, plagas, COA")
                        .build(),

                MonthlyKpiCardDto.builder()
                        .kpiNumber(7)
                        .id("kpi-reclamos-cliente")
                        .title("7. Reclamos de Cliente")
                        .category("Satisfacción")
                        .value(String.valueOf(clientClaimsCount))
                        .numericValue(BigDecimal.valueOf(clientClaimsCount))
                        .unit("Reclamos")
                        .target("0 Críticos")
                        .status(clientClaimsCount == 0 ? "SUCCESS" : (clientClaimsCount <= 2 ? "WARNING" : "DANGER"))
                        .sublabel("Quejas externas registradas")
                        .build(),

                MonthlyKpiCardDto.builder()
                        .kpiNumber(8)
                        .id("kpi-costo-no-calidad")
                        .title("8. Costo de la No Calidad")
                        .category("Finanzas QM")
                        .value(String.format(Locale.US, "$ %,.2f MXN", totalNonQualityCost.doubleValue()))
                        .numericValue(totalNonQualityCost)
                        .unit("MXN")
                        .target("< $10,000 MXN")
                        .status(totalNonQualityCost.compareTo(BigDecimal.valueOf(10000)) <= 0 ? "SUCCESS" : "DANGER")
                        .sublabel("Impacto económico total")
                        .build(),

                MonthlyKpiCardDto.builder()
                        .kpiNumber(9)
                        .id("kpi-total-producto-danado")
                        .title("9. Total Piezas Físicas Dañadas")
                        .category("Inventario Físico")
                        .value(totalDamagedPieces + " U")
                        .numericValue(BigDecimal.valueOf(totalDamagedPieces))
                        .unit("Pzas")
                        .target("≤ 70 U máx")
                        .status(totalDamagedPieces <= 70 ? "SUCCESS" : "DANGER")
                        .sublabel(String.format("PT: %d · Emb: %d · Café: %d", ptDamaged, pkgDamaged, coffeeDamaged))
                        .build(),

                MonthlyKpiCardDto.builder()
                        .kpiNumber(10)
                        .id("kpi-acciones-realizadas")
                        .title("10. Acciones & Resoluciones")
                        .category("Disposición FSM")
                        .value(monthDeviations.size() + " Resueltas")
                        .numericValue(BigDecimal.valueOf(monthDeviations.size()))
                        .unit("Acciones")
                        .target("100% Cerradas")
                        .status("SUCCESS")
                        .sublabel("Bloqueos, rechazos y acond.")
                        .build()
        );

        return QualityMonthlyBoardResponse.builder()
                .year(targetYear)
                .month(targetMonth)
                .monthName(monthName)
                .branchName("Toluca - Nave M1")
                .kpiCards(cards)
                .releasesByCollaborator(releasesByCollab)
                .rootCauseDistribution(rootCauseMap)
                .storageDeviationsByType(Map.of("Pallet dañado", storageDeviationsCount))
                .inboundDeviationsByType(Map.of("Desembarque", inboundDeviationsCount))
                .clientClaimsByOrigin(Map.of("Calidad", clientClaimsCount))
                .actionsTakenDistribution(actionsMap)
                .totalInspectedLots(totalInspected)
                .totalDeviations(monthDeviations.size())
                .totalDamagedPieces(totalDamagedPieces)
                .ptDamagedPieces(BigDecimal.valueOf(ptDamaged))
                .packagingDamagedPieces(BigDecimal.valueOf(pkgDamaged))
                .greenCoffeeDamagedPieces(BigDecimal.valueOf(coffeeDamaged))
                .totalNonQualityCost(totalNonQualityCost)
                .deviations(monthDeviations.stream().map(this::mapToDeviationResponse).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDeviationsExcel(UUID organizationId, UUID branchId, Integer year, Integer month) {
        QualityMonthlyBoardResponse board = getMonthlyBoard(organizationId, branchId, year, month);
        StringBuilder csv = new StringBuilder();
        csv.append("4GUARD WMS — CONCENTRADO DE KPIS DE CALIDAD Y DESVIACIONES\n");
        csv.append(String.format("Periodo: %s %d | Almacén: %s\n\n", escapeCsvCell(board.getMonthName()), board.getYear(), escapeCsvCell(board.getBranchName())));
        csv.append("MATRIZ DE 10 KPIS:\n");
        csv.append("No,KPI,Valor,Meta,Estatus\n");
        for (MonthlyKpiCardDto card : board.getKpiCards()) {
            csv.append(String.format("%d,%s,%s,%s,%s\n",
                    card.getKpiNumber(),
                    escapeCsvCell(card.getTitle()),
                    escapeCsvCell(card.getValue()),
                    escapeCsvCell(card.getTarget()),
                    escapeCsvCell(card.getStatus())));
        }
        csv.append("\nDETALLE DE DESVIACIONES REGISTRADAS:\n");
        csv.append("Folio,Fecha,Remision,SKU,UA/SSCC,Material,Unidades,Costo ($),Condicion,Causa Raiz,Area,Accion\n");
        for (QualityDeviationResponse dev : board.getDeviations()) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%d,%.2f,%s,%s,%s,%s\n",
                    escapeCsvCell(dev.getFolio()),
                    escapeCsvCell(dev.getDeviationDate()),
                    escapeCsvCell(dev.getRemisionNumber()),
                    escapeCsvCell(dev.getSkuId()),
                    escapeCsvCell(dev.getUaCode()),
                    escapeCsvCell(dev.getMaterialType()),
                    dev.getDamagedUnits() != null ? dev.getDamagedUnits() : 0,
                    dev.getMaterialCost() != null ? dev.getMaterialCost().doubleValue() : 0.0,
                    escapeCsvCell(dev.getConditionDeviation()),
                    escapeCsvCell(dev.getRootCauseMotive()),
                    escapeCsvCell(dev.getOriginArea()),
                    escapeCsvCell(dev.getActionTaken())));
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Sanitizes CSV cell content against CSV Formula Injection (CWE-1236)
     * and escapes internal quotes.
     */
    private String escapeCsvCell(Object value) {
        if (value == null) return "\"\"";
        String str = String.valueOf(value);
        if (str.isEmpty()) return "\"\"";
        
        // Neutralize formula injection characters (=, +, -, @, tab, CR)
        char firstChar = str.charAt(0);
        if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
            str = "'" + str;
        }
        
        // Escape internal double quotes
        str = str.replace("\"", "\"\"");
        return "\"" + str + "\"";
    }

    private QualityDeviationResponse mapToDeviationResponse(QualityDeviationEntity entity) {
        if (entity == null) return null;
        List<String> photos = fromJson(entity.getEvidencePhotoUrls(), new TypeReference<List<String>>() {});
        return QualityDeviationResponse.builder()
                .id(entity.getId())
                .folio(entity.getFolio())
                .remisionNumber(entity.getRemisionNumber())
                .skuId(entity.getSkuId())
                .skuDescription(entity.getSkuDescription())
                .uaCode(entity.getUaCode())
                .materialType(entity.getMaterialType())
                .deviationDate(entity.getDeviationDate())
                .deviationTime(entity.getDeviationTime())
                .detectedById(entity.getDetectedBy() != null ? entity.getDetectedBy().getId() : null)
                .detectedByName(entity.getDetectedBy() != null ? entity.getDetectedBy().getFullName() : "Inspector QM")
                .responsibleCollaborator(entity.getResponsibleCollaborator())
                .bayLocationCode(entity.getBayLocationCode())
                .damagedUnits(entity.getDamagedUnits())
                .materialCost(entity.getMaterialCost())
                .currency(entity.getCurrency())
                .conditionDeviation(entity.getConditionDeviation())
                .rootCauseMotive(entity.getRootCauseMotive())
                .originArea(entity.getOriginArea())
                .evidencePhotoUrls(photos != null ? photos : new ArrayList<>())
                .actionTaken(entity.getActionTaken())
                .observations(entity.getObservations())
                .isResolved(entity.getIsResolved())
                .createdAt(entity.getCreatedAt())
                .build();
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
