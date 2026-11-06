package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.transfer.CancelTransferRequest;
import com.fourguard.wms.application.dto.request.transfer.CreateTransferRequest;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferSummaryResponse;
import com.fourguard.wms.application.mapper.WarehouseTransferMapper;
import com.fourguard.wms.domain.enums.MovementScope;
import com.fourguard.wms.domain.enums.MovementType;
import com.fourguard.wms.domain.enums.TransferReason;
import com.fourguard.wms.domain.enums.TransferStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.WarehouseTransferUseCase;
import com.fourguard.wms.domain.ports.out.*;
import com.fourguard.wms.infrastructure.notification.QualityAlertBroadcaster;
import com.fourguard.wms.infrastructure.persistence.entity.*;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for Warehouse Transfer Use Case (Cambio de Almacén).
 * Homologado con ADR-011, ADR-017, ADR-020 y ADR-022.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseTransferService implements WarehouseTransferUseCase {

    private final WarehouseTransferRepositoryPort transferRepositoryPort;
    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final LocationRepositoryPort locationRepositoryPort;
    private final ForkliftOperatorRepositoryPort forkliftOperatorRepositoryPort;
    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final AuditService auditService;
    private final SecurityAuditHelper securityAuditHelper;
    private final PasswordEncoder passwordEncoder;
    private final WarehouseTransferMapper transferMapper;
    private final ShiftSessionService shiftSessionService;
    private final QualityAlertBroadcaster alertBroadcaster;

    @Override
    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request) {
        log.info("Creating warehouse transfer from origin: (id={}, code={}) to dest: (id={}, code={}) with priority: {}",
                request.getOriginLocationId(), request.getOriginLocationCode(),
                request.getDestinationLocationId(), request.getDestinationLocationCode(), request.getPriority());

        if (request.getOrganizationId() == null) {
            throw new ValidationException("El ID de la organización es obligatorio para registrar un traspaso.");
        }
        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organización no encontrada con ID: " + request.getOrganizationId()));

        if (request.getBranchId() == null) {
            throw new ValidationException("El ID de la sucursal es obligatorio para registrar un traspaso.");
        }
        BranchEntity branch = branchRepositoryPort.findById(request.getBranchId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Sucursal no encontrada con ID: " + request.getBranchId()));

        LocationEntity origin = null;
        if (request.getOriginLocationId() != null) {
            origin = locationRepositoryPort.findById(request.getOriginLocationId()).orElse(null);
        }
        if (origin == null && request.getOriginLocationCode() != null && !request.getOriginLocationCode().isBlank()) {
            String oCode = request.getOriginLocationCode().trim();
            origin = locationRepositoryPort.findAll().stream()
                    .filter(l -> oCode.equalsIgnoreCase(l.getCode())
                            || (l.getName() != null && oCode.equalsIgnoreCase(l.getName())))
                    .findFirst().orElse(null);
        }
        if (origin == null) {
            throw new EntityNotFoundException("Ubicación origen no encontrada: "
                    + (request.getOriginLocationCode() != null ? request.getOriginLocationCode()
                            : request.getOriginLocationId()));
        }

        LocationEntity destination = null;
        if (request.getDestinationLocationId() != null) {
            destination = locationRepositoryPort.findById(request.getDestinationLocationId()).orElse(null);
        }
        if (destination == null && request.getDestinationLocationCode() != null
                && !request.getDestinationLocationCode().isBlank()) {
            String dCode = request.getDestinationLocationCode().trim();
            destination = locationRepositoryPort.findAll().stream()
                    .filter(l -> dCode.equalsIgnoreCase(l.getCode())
                            || (l.getName() != null && dCode.equalsIgnoreCase(l.getName())))
                    .findFirst().orElse(null);
        }
        if (destination == null) {
            throw new EntityNotFoundException("Ubicación destino no encontrada: "
                    + (request.getDestinationLocationCode() != null ? request.getDestinationLocationCode()
                            : request.getDestinationLocationId()));
        }

        if (origin.getId().equals(destination.getId())) {
            throw new ValidationException("La ubicación de origen y destino no pueden ser la misma.");
        }

        ForkliftOperatorEntity operator = null;
        if (request.getForkliftOperatorId() != null) {
            operator = forkliftOperatorRepositoryPort.findById(request.getForkliftOperatorId()).orElse(null);
        }
        if (operator == null && request.getForkliftOperatorName() != null
                && !request.getForkliftOperatorName().isBlank()) {
            String opName = request.getForkliftOperatorName().trim();
            operator = forkliftOperatorRepositoryPort.findAll().stream()
                    .filter(o -> opName.equalsIgnoreCase(o.getFullName())
                            || (o.getCode() != null && opName.equalsIgnoreCase(o.getCode())))
                    .findFirst().orElse(null);
        }

        TransferReason reason;
        try {
            reason = TransferReason.valueOf(request.getReasonCode());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Código de motivo inválido: " + request.getReasonCode());
        }

        long nextVal = transferRepositoryPort.nextFolioSequenceValue();
        int year = LocalDate.now().getYear();
        String folio = String.format("CAM-%d-%06d", year, nextVal);

        List<InventoryItemEntity> originItems = inventoryItemRepositoryPort.findByLocationId(origin.getId());
        if (originItems.isEmpty()) {
            throw new ValidationException("La bahía origen (" + origin.getCode() + ") no tiene inventario disponible.");
        }

        List<InventoryItemEntity> itemsToMove = new ArrayList<>();
        if (request.getSelectedItemIds() != null && !request.getSelectedItemIds().isEmpty()) {
            Set<UUID> targetIds = new HashSet<>(request.getSelectedItemIds());
            itemsToMove = originItems.stream()
                    .filter(i -> targetIds.contains(i.getId()))
                    .collect(Collectors.toList());
        } else if (request.getPalletCodes() != null && !request.getPalletCodes().isEmpty()) {
            Set<String> targetCodes = new HashSet<>(request.getPalletCodes());
            itemsToMove = originItems.stream()
                    .filter(i -> (i.getSscc() != null && targetCodes.contains(i.getSscc())) ||
                            (i.getExternalUa() != null && targetCodes.contains(i.getExternalUa())))
                    .collect(Collectors.toList());
        } else if (request.getPalletIds() != null && !request.getPalletIds().isEmpty()) {
            Set<String> targetCodes = new HashSet<>(request.getPalletIds());
            itemsToMove = originItems.stream()
                    .filter(i -> (i.getSscc() != null && targetCodes.contains(i.getSscc())) ||
                            (i.getExternalUa() != null && targetCodes.contains(i.getExternalUa())))
                    .collect(Collectors.toList());
        } else {
            itemsToMove = new ArrayList<>(originItems);
        }

        if (itemsToMove.isEmpty()) {
            throw new ValidationException("No se encontraron las tarimas seleccionadas en la bahía de origen.");
        }

        Set<UUID> distinctSkuIds = itemsToMove.stream()
                .filter(i -> i.getSku() != null)
                .map(i -> i.getSku().getId())
                .collect(Collectors.toSet());
        double totalPieces = itemsToMove.stream()
                .mapToDouble(i -> i.getQuantity() != null ? i.getQuantity().doubleValue() : 0.0)
                .sum();

        UserEntity activeUser = null;
        try {
            String currentUsername = securityAuditHelper.getCurrentUsername();
            if (currentUsername != null && !currentUsername.isBlank()) {
                activeUser = userRepositoryPort.findByUsernameOrEmail(currentUsername).orElse(null);
            }
        } catch (Exception ignored) {
        }

        String creator = null;
        if (request.getTransferredBy() != null && !request.getTransferredBy().isBlank()) {
            creator = request.getTransferredBy().trim();
        } else if (activeUser != null) {
            creator = ((activeUser.getFirstName() != null ? activeUser.getFirstName() : "") +
                    (activeUser.getLastName() != null ? " " + activeUser.getLastName() : "")).trim();
            if (creator.isBlank())
                creator = activeUser.getUsername();
        } else {
            creator = securityAuditHelper.getCurrentUsername();
        }
        if (creator == null || creator.isBlank() || creator.startsWith("@")) {
            creator = activeUser != null ? activeUser.getUsername() : "Sistema / Supervisor";
        }

        String priority = (request.getPriority() != null && !request.getPriority().isBlank())
                ? request.getPriority().trim().toUpperCase()
                : "NORMAL";
        if (!"NORMAL".equals(priority) && !"URGENT".equals(priority)) {
            priority = "NORMAL";
        }

        WarehouseTransferEntity transfer = WarehouseTransferEntity.builder()
                .organization(organization)
                .branch(branch)
                .folio(folio)
                .status(TransferStatus.ASSIGNED)
                .priority(priority)
                .originLocation(origin)
                .destinationLocation(destination)
                .forkliftOperator(operator)
                .reasonCode(reason)
                .reasonLabel(request.getReasonLabel() != null ? request.getReasonLabel() : reason.name())
                .observations(request.getObservations())
                .totalPallets(itemsToMove.size())
                .totalPieces(BigDecimal.valueOf(totalPieces))
                .distinctSkus(distinctSkuIds.isEmpty() ? 1 : distinctSkuIds.size())
                .createdBy(creator)
                .shiftSessionId(shiftSessionService.getOrOpenActiveSession(branch.getId()).getId())
                .build();

        List<WarehouseTransferItemEntity> transferItems = new ArrayList<>();
        for (InventoryItemEntity item : itemsToMove) {
            String itemSscc = (item.getSscc() != null && !item.getSscc().isBlank()) ? item.getSscc()
                    : item.getExternalUa();
            if (itemSscc == null || itemSscc.isBlank())
                itemSscc = "UA-" + (System.currentTimeMillis() % 100000);

            transferItems.add(WarehouseTransferItemEntity.builder()
                    .transfer(transfer)
                    .item(item)
                    .pieces(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO)
                    .palletCode(itemSscc)
                    .build());
        }
        transfer.setItems(transferItems);

        WarehouseTransferEntity saved = transferRepositoryPort.save(transfer);

        String originCode = (origin != null && origin.getCode() != null) ? origin.getCode() : "N/A";
        String destinationCode = (destination != null && destination.getCode() != null) ? destination.getCode() : "N/A";
        String operatorName = (operator != null && operator.getFullName() != null) ? operator.getFullName() : "N/A";
        String safeFolio = (folio != null) ? folio : "";

        logAudit(saved.getId(), "TRASPASO_REGISTRADO", activeUser,
                Map.of("origin", originCode),
                Map.of("folio", safeFolio,
                        "origin", originCode,
                        "destination", destinationCode,
                        "priority", priority,
                        "totalPallets", String.valueOf(itemsToMove.size()),
                        "operator", operatorName));

        // Transmisión SSE a la terminal RF
        try {
            Map<String, Object> ssePayload = new HashMap<>();
            ssePayload.put("missionId", saved.getId().toString());
            ssePayload.put("folio", saved.getFolio());
            ssePayload.put("type", "INTERNAL_TRANSFER");
            ssePayload.put("status", saved.getStatus().name());
            ssePayload.put("priority", saved.getPriority());
            ssePayload.put("originLocation", originCode);
            ssePayload.put("destinationLocation", destinationCode);
            if (saved.getForkliftOperator() != null) {
                ssePayload.put("forkliftOperatorId", saved.getForkliftOperator().getId().toString());
                ssePayload.put("forkliftOperatorCode", saved.getForkliftOperator().getCode());
                ssePayload.put("forkliftOperatorName", saved.getForkliftOperator().getFullName());
            }
            ssePayload.put("totalPallets", saved.getTotalPallets());
            ssePayload.put("totalPieces", saved.getTotalPieces());
            alertBroadcaster.broadcastMissionEvent("MISSION_ASSIGNED", ssePayload);
        } catch (Exception ignored) {
        }

        return transferMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse startTransfer(UUID id, UUID operatorId) {
        WarehouseTransferEntity transfer = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado con ID: " + id));

        if (transfer.getStatus() != TransferStatus.ASSIGNED && transfer.getStatus() != TransferStatus.PAUSED) {
            throw new ValidationException("Solo se puede iniciar un traspaso en estado ASIGNADO o PAUSADO. Estado actual: " + transfer.getStatus());
        }

        if (operatorId != null && transfer.getForkliftOperator() == null) {
            forkliftOperatorRepositoryPort.findById(operatorId).ifPresent(transfer::setForkliftOperator);
        }

        transfer.setStatus(TransferStatus.IN_PROGRESS);
        if (transfer.getStartedAt() == null) {
            transfer.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }

        WarehouseTransferEntity saved = transferRepositoryPort.save(transfer);
        broadcastTransferEvent("MISSION_UPDATED", saved);
        return transferMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse pauseTransfer(UUID id, String reason, String notes) {
        WarehouseTransferEntity transfer = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado con ID: " + id));

        if (transfer.getStatus() != TransferStatus.IN_PROGRESS && transfer.getStatus() != TransferStatus.ASSIGNED) {
            throw new ValidationException("Solo se puede pausar un traspaso en curso o asignado. Estado actual: " + transfer.getStatus());
        }

        transfer.setStatus(TransferStatus.PAUSED);
        transfer.setPausedAt(OffsetDateTime.now(ZoneOffset.UTC));
        transfer.setPauseReason(reason != null ? reason : "Pausa operativa / Cambio de turno");

        WarehouseTransferEntity saved = transferRepositoryPort.save(transfer);
        broadcastTransferEvent("MISSION_UPDATED", saved);
        return transferMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse resumeTransfer(UUID id, UUID operatorId) {
        WarehouseTransferEntity transfer = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado con ID: " + id));

        if (transfer.getStatus() != TransferStatus.PAUSED) {
            throw new ValidationException("Solo se puede reanudar un traspaso en estado PAUSADO. Estado actual: " + transfer.getStatus());
        }

        if (operatorId != null) {
            forkliftOperatorRepositoryPort.findById(operatorId).ifPresent(transfer::setForkliftOperator);
        }

        transfer.setStatus(TransferStatus.IN_PROGRESS);
        transfer.setPauseReason(null);

        WarehouseTransferEntity saved = transferRepositoryPort.save(transfer);
        broadcastTransferEvent("MISSION_UPDATED", saved);
        return transferMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse completeTransfer(UUID id, UUID destinationLocationId) {
        WarehouseTransferEntity transfer = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado con ID: " + id));

        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            return transferMapper.toResponse(transfer);
        }
        if (transfer.getStatus() == TransferStatus.CANCELLED) {
            throw new ValidationException("No se puede completar un traspaso cancelado.");
        }

        LocationEntity destination = transfer.getDestinationLocation();
        if (destinationLocationId != null && !destinationLocationId.equals(destination.getId())) {
            destination = locationRepositoryPort.findById(destinationLocationId)
                    .orElseThrow(() -> new EntityNotFoundException("Nueva bahía destino no encontrada"));
            transfer.setDestinationLocation(destination);
        }

        LocationEntity origin = transfer.getOriginLocation();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        UserEntity activeUser = null;
        try {
            String currentUsername = securityAuditHelper.getCurrentUsername();
            if (currentUsername != null && !currentUsername.isBlank()) {
                activeUser = userRepositoryPort.findByUsernameOrEmail(currentUsername).orElse(null);
            }
        } catch (Exception ignored) {
        }

        int movedCount = 0;
        for (WarehouseTransferItemEntity transferItem : transfer.getItems()) {
            InventoryItemEntity item = transferItem.getItem();
            if (item != null) {
                item.setLocation(destination);
                inventoryItemRepositoryPort.save(item);
                movedCount++;

                if (activeUser != null) {
                    InventoryMovementEntity movement = InventoryMovementEntity.builder()
                            .item(item)
                            .fromLocation(origin)
                            .toLocation(destination)
                            .user(activeUser)
                            .type(MovementType.TRANSFER)
                            .reason("Traspaso Folio: " + transfer.getFolio() + " (" + transfer.getReasonCode() + ")")
                            .createdAt(now)
                            .build();
                    inventoryMovementRepositoryPort.save(movement);
                }
            }
        }

        if (origin != null && origin.getId() != null && movedCount > 0) {
            locationRepositoryPort.decrementOccupancy(origin.getId(), movedCount);
        }
        if (destination != null && destination.getId() != null && movedCount > 0) {
            locationRepositoryPort.incrementOccupancy(destination.getId(), movedCount);
        }

        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(now);

        WarehouseTransferEntity saved = transferRepositoryPort.save(transfer);
        broadcastTransferEvent("MISSION_COMPLETED", saved);

        logAudit(saved.getId(), "TRASPASO_COMPLETADO", activeUser,
                Map.of("status", "IN_PROGRESS"),
                Map.of("status", "COMPLETED", "destination", destination.getCode(), "totalPallets", String.valueOf(movedCount)));

        return transferMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse updatePriority(UUID id, String priority) {
        WarehouseTransferEntity transfer = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado con ID: " + id));

        String sanitized = (priority != null) ? priority.trim().toUpperCase() : "NORMAL";
        if (!"NORMAL".equals(sanitized) && !"URGENT".equals(sanitized)) {
            throw new ValidationException("Prioridad inválida: " + priority + ". Debe ser 'NORMAL' o 'URGENT'.");
        }

        transfer.setPriority(sanitized);
        WarehouseTransferEntity saved = transferRepositoryPort.save(transfer);

        broadcastTransferEvent("URGENT".equals(sanitized) ? "MISSION_URGENT_PREEMPT" : "MISSION_UPDATED", saved);
        return transferMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse reassignOperator(UUID id, UUID forkliftOperatorId) {
        WarehouseTransferEntity transfer = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado con ID: " + id));

        ForkliftOperatorEntity newOperator = forkliftOperatorRepositoryPort.findById(forkliftOperatorId)
                .orElseThrow(() -> new EntityNotFoundException("Operador de montacargas no encontrado con ID: " + forkliftOperatorId));

        transfer.setForkliftOperator(newOperator);
        WarehouseTransferEntity saved = transferRepositoryPort.save(transfer);

        broadcastTransferEvent("MISSION_ASSIGNED", saved);
        return transferMapper.toResponse(saved);
    }

    private void broadcastTransferEvent(String eventType, WarehouseTransferEntity transfer) {
        try {
            Map<String, Object> ssePayload = new HashMap<>();
            ssePayload.put("missionId", transfer.getId().toString());
            ssePayload.put("folio", transfer.getFolio());
            ssePayload.put("type", "INTERNAL_TRANSFER");
            ssePayload.put("status", transfer.getStatus().name());
            ssePayload.put("priority", transfer.getPriority());
            if (transfer.getOriginLocation() != null) {
                ssePayload.put("originLocation", transfer.getOriginLocation().getCode());
            }
            if (transfer.getDestinationLocation() != null) {
                ssePayload.put("destinationLocation", transfer.getDestinationLocation().getCode());
            }
            if (transfer.getForkliftOperator() != null) {
                ssePayload.put("forkliftOperatorId", transfer.getForkliftOperator().getId().toString());
                ssePayload.put("forkliftOperatorCode", transfer.getForkliftOperator().getCode());
                ssePayload.put("forkliftOperatorName", transfer.getForkliftOperator().getFullName());
            }
            ssePayload.put("totalPallets", transfer.getTotalPallets());
            ssePayload.put("totalPieces", transfer.getTotalPieces());
            alertBroadcaster.broadcastMissionEvent(eventType, ssePayload);
        } catch (Exception ignored) {
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransferById(UUID id) {
        WarehouseTransferEntity entity = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado con ID: " + id));
        return transferMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferSummaryResponse> getTransfers(
            UUID organizationId,
            UUID branchId,
            String status,
            String search,
            UUID shiftSessionId,
            MovementScope scope) {

        TransferStatus transferStatus = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                transferStatus = TransferStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid transfer status filter: {}", status);
            }
        }

        var spec = WarehouseTransferSpecification.withFilters(
                organizationId, branchId, transferStatus, search, shiftSessionId, scope);

        return transferRepositoryPort.findAll(spec).stream()
                .map(transferMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransferResponse cancelTransfer(UUID id, CancelTransferRequest request) {
        WarehouseTransferEntity transfer = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado con ID: " + id));

        if (transfer.getStatus() == TransferStatus.CANCELLED) {
            throw new ValidationException("El traspaso ya se encuentra cancelado.");
        }

        UserEntity authorizedBy = userRepositoryPort.findByUsernameOrEmail(request.getAdminUsername())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuario autorizador no encontrado: " + request.getAdminUsername()));

        if (!passwordEncoder.matches(request.getAdminPassword(), authorizedBy.getPassword())) {
            throw new ValidationException("Contraseña incorrecta para el usuario autorizador.");
        }

        // Revertir inventario si estaba completado
        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            LocationEntity origin = transfer.getOriginLocation();
            LocationEntity destination = transfer.getDestinationLocation();
            int movedCount = 0;

            for (WarehouseTransferItemEntity transferItem : transfer.getItems()) {
                InventoryItemEntity item = transferItem.getItem();
                if (item != null) {
                    item.setLocation(origin);
                    inventoryItemRepositoryPort.save(item);
                    movedCount++;

                    InventoryMovementEntity revMovement = InventoryMovementEntity.builder()
                            .item(item)
                            .fromLocation(destination)
                            .toLocation(origin)
                            .user(authorizedBy)
                            .type(MovementType.TRANSFER)
                            .reason("REVERSIÓN de Traspaso Folio: " + transfer.getFolio()
                                    + " (Cancelado: " + request.getReason() + ")")
                            .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                            .build();
                    inventoryMovementRepositoryPort.save(revMovement);
                }
            }

            if (destination != null && destination.getId() != null && movedCount > 0) {
                locationRepositoryPort.decrementOccupancy(destination.getId(), movedCount);
            }
            if (origin != null && origin.getId() != null && movedCount > 0) {
                locationRepositoryPort.incrementOccupancy(origin.getId(), movedCount);
            }
        }

        transfer.setStatus(TransferStatus.CANCELLED);
        transfer.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));
        transfer.setCancellationReason(request.getReason());
        transfer.setCancelledBy(authorizedBy.getUsername());

        WarehouseTransferEntity saved = transferRepositoryPort.save(transfer);

        logAudit(saved.getId(), "TRASPASO_CANCELADO", authorizedBy,
                Map.of("status", "ACTIVE"),
                Map.of("status", "CANCELLED",
                        "reason", request.getReason(),
                        "authorizedBy", authorizedBy.getUsername()));

        broadcastTransferEvent("MISSION_CANCELLED", saved);
        return transferMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementAuditResponse> getAuditLogs(UUID id) {
        WarehouseTransferEntity transfer = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado con ID: " + id));

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("TRANSFER", id);
        if (logs.isEmpty()) {
            logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("WarehouseTransferEntity", id);
        }

        return logs.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::mapToAuditResponse)
                .collect(Collectors.toList());
    }

    private void logAudit(UUID entityId, String action, UserEntity user,
            Map<String, ?> oldVals, Map<String, ?> newVals) {
        try {
            UserEntity activeUser = user;
            if (activeUser == null) {
                String username = securityAuditHelper.getCurrentUsername();
                if (username != null && !username.isBlank()) {
                    activeUser = userRepositoryPort.findByUsername(username).orElse(null);
                }
            }
            if (activeUser == null) {
                activeUser = userRepositoryPort.findAll().stream()
                        .filter(u -> Boolean.TRUE.equals(u.getIsEnabled()))
                        .findFirst()
                        .orElse(null);
            }
            if (activeUser != null) {
                Map<String, Object> beforeMap = oldVals != null ? new HashMap<>(oldVals) : Collections.emptyMap();
                Map<String, Object> afterMap = newVals != null ? new HashMap<>(newVals) : Collections.emptyMap();
                auditService.log(activeUser, action, "TRANSFER", entityId, beforeMap, afterMap);
            }
        } catch (Exception e) {
            log.warn("Could not record audit log for transfer {}: {}", entityId, e.getMessage());
        }
    }

    private MovementAuditResponse mapToAuditResponse(AuditLogEntity log) {
        List<MovementAuditResponse.MovementAuditDetailResponse> details = log.getDetails() != null
                ? log.getDetails().stream()
                        .map(d -> MovementAuditResponse.MovementAuditDetailResponse.builder()
                                .fieldName(translateFieldName(d.getFieldName()))
                                .oldValue(translateFieldValue(d.getFieldName(), d.getOldValue()))
                                .newValue(translateFieldValue(d.getFieldName(), d.getNewValue()))
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        String actionLabel = switch (log.getAction()) {
            case "TRASPASO_REGISTRADO" -> "Reubicación de Tarima (Traspaso / Putaway)";
            case "TRASPASO_COMPLETADO" -> "Depósito Físico Concluido en Bahía Destino";
            case "TRASPASO_CANCELADO" -> "Cancelación de Reubicación de Inventario";
            default -> log.getAction();
        };

        String username = "Usuario Sistema";
        if (log.getUserId() != null) {
            username = userRepositoryPort.findById(log.getUserId())
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("Usuario " + log.getUserId());
        }

        String formattedTimestamp = log.getCreatedAt() != null
                ? log.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : "";

        return MovementAuditResponse.builder()
                .id(log.getLogId() != null ? log.getLogId().toString() : UUID.randomUUID().toString())
                .action(log.getAction())
                .actionLabel(actionLabel)
                .username(username)
                .timestamp(formattedTimestamp)
                .details(details)
                .build();
    }

    private String translateFieldName(String field) {
        if (field == null || field.isBlank())
            return "Dato";
        return switch (field.trim()) {
            case "sourceLocation", "source_location", "origin" -> "Ubicación Origen";
            case "targetLocation", "target_location", "destination" -> "Ubicación Destino";
            case "status" -> "Estado Operativo";
            case "priority" -> "Prioridad Operativa";
            case "reason", "cancellationReason" -> "Motivo / Justificación";
            case "authorizedBy", "authorized_by" -> "Autorizado Por (Supervisor)";
            case "cancelledBy", "cancelled_by" -> "Cancelado Por";
            case "palletCode", "pallet_code" -> "Código de Tarima (UA)";
            case "forkliftOperator", "operator" -> "Operador de Montacargas";
            case "transferredBy", "transferred_by" -> "Operador Responsable";
            case "totalPallets", "pallets" -> "Tarimas Totales (UAs)";
            case "totalPieces", "pieces" -> "Piezas Totales";
            case "folio" -> "Folio de Operación";
            default -> field;
        };
    }

    private String translateFieldValue(String field, String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value))
            return "Sin especificar";
        String val = value.trim();
        return switch (val) {
            case "ASSIGNED" -> "Asignado a Montacargas";
            case "IN_PROGRESS" -> "En Maniobra de Reubicación";
            case "PAUSED" -> "Pausado (Cambio de Turno / Espera)";
            case "COMPLETED" -> "Completado / Reubicado";
            case "CANCELLED" -> "Cancelado";
            case "URGENT" -> "Urgente (Prioridad Alta)";
            case "NORMAL" -> "Normal";
            default -> val;
        };
    }
}
