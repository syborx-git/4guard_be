package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.transfer.CancelTransferRequest;
import com.fourguard.wms.application.dto.request.transfer.CreateTransferRequest;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferSummaryResponse;
import com.fourguard.wms.application.mapper.WarehouseTransferMapper;
import com.fourguard.wms.domain.enums.MovementType;
import com.fourguard.wms.domain.enums.TransferReason;
import com.fourguard.wms.domain.enums.TransferStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.WarehouseTransferUseCase;
import com.fourguard.wms.domain.ports.out.*;
import com.fourguard.wms.infrastructure.persistence.entity.*;
import com.fourguard.wms.infrastructure.persistence.repository.WarehouseTransferJpaRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseTransferService implements WarehouseTransferUseCase {

    private final WarehouseTransferRepositoryPort transferRepositoryPort;
    private final WarehouseTransferJpaRepository transferJpaRepository;
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

    @Override
    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request) {
        log.info("Creating warehouse transfer from {} to {}", request.getOriginLocationId(), request.getDestinationLocationId());

        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada: " + request.getOrganizationId()));

        BranchEntity branch = branchRepositoryPort.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada: " + request.getBranchId()));

        LocationEntity origin = locationRepositoryPort.findById(request.getOriginLocationId())
                .orElseThrow(() -> new EntityNotFoundException("Ubicación origen no encontrada: " + request.getOriginLocationId()));

        LocationEntity destination = locationRepositoryPort.findById(request.getDestinationLocationId())
                .orElseThrow(() -> new EntityNotFoundException("Ubicación destino no encontrada: " + request.getDestinationLocationId()));

        if (origin.getId().equals(destination.getId())) {
            throw new ValidationException("La ubicación de origen y destino no pueden ser la misma.");
        }

        ForkliftOperatorEntity operator = null;
        if (request.getForkliftOperatorId() != null) {
            operator = forkliftOperatorRepositoryPort.findById(request.getForkliftOperatorId()).orElse(null);
        }

        TransferReason reason = TransferReason.REUB_OPERATIVA;
        if (request.getReasonCode() != null) {
            try {
                reason = TransferReason.valueOf(request.getReasonCode().toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }

        // Generate consecutive folio: CAM-YYYY-XXXXXX
        long seq = transferRepositoryPort.nextFolioSequenceValue();
        int year = LocalDate.now().getYear();
        String folio = String.format("CAM-%d-%06d", year, seq);

        // Fetch items to transfer
        List<InventoryItemEntity> itemsToMove = new ArrayList<>();
        for (UUID itemId : request.getSelectedItemIds()) {
            InventoryItemEntity item = inventoryItemRepositoryPort.findById(itemId)
                    .orElseThrow(() -> new EntityNotFoundException("Ítem de inventario no encontrado: " + itemId));

            if (item.getLocation() == null || !item.getLocation().getId().equals(origin.getId())) {
                throw new ValidationException("La tarima " + item.getSscc() + " no pertenece a la bahía origen " + origin.getCode());
            }
            itemsToMove.add(item);
        }

        Set<UUID> distinctSkuIds = itemsToMove.stream().map(i -> i.getSku().getId()).collect(Collectors.toSet());
        double totalPieces = itemsToMove.stream().mapToDouble(i -> i.getQuantity() != null ? i.getQuantity().doubleValue() : 0.0).sum();

        WarehouseTransferEntity transfer = WarehouseTransferEntity.builder()
                .organization(organization)
                .branch(branch)
                .folio(folio)
                .status(TransferStatus.COMPLETED)
                .originLocation(origin)
                .destinationLocation(destination)
                .forkliftOperator(operator)
                .reasonCode(reason)
                .reasonLabel(request.getReasonLabel())
                .observations(request.getObservations())
                .totalPallets(itemsToMove.size())
                .totalPieces(BigDecimal.valueOf(totalPieces))
                .distinctSkus(distinctSkuIds.size())
                .build();

        UserEntity activeUser = null;
        try {
            String currentUsername = securityAuditHelper.getCurrentUsername();
            activeUser = userRepositoryPort.findByUsername(currentUsername).orElse(null);
        } catch (Exception ignored) {}

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<WarehouseTransferItemEntity> transferItems = new ArrayList<>();

        for (InventoryItemEntity item : itemsToMove) {
            // Relocate item in inventory
            item.setLocation(destination);
            inventoryItemRepositoryPort.save(item);

            transferItems.add(WarehouseTransferItemEntity.builder()
                    .transfer(transfer)
                    .item(item)
                    .pieces(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO)
                    .palletCode(item.getSscc())
                    .build());

            // Log Inventory Movement
            if (activeUser != null) {
                InventoryMovementEntity movement = InventoryMovementEntity.builder()
                        .item(item)
                        .fromLocation(origin)
                        .toLocation(destination)
                        .user(activeUser)
                        .type(MovementType.TRANSFER)
                        .reason("Traspaso Folio: " + folio + " (" + reason.name() + ")")
                        .createdAt(now)
                        .build();
                inventoryMovementRepositoryPort.save(movement);
            }
        }
        transfer.setItems(transferItems);

        WarehouseTransferEntity saved = transferRepositoryPort.save(transfer);

        logAudit(saved.getId(), "TRASPASO_REGISTRADO",
                Map.of("origin", origin.getCode()),
                Map.of("folio", folio,
                       "origin", origin.getCode(),
                       "destination", destination.getCode(),
                       "totalPallets", String.valueOf(itemsToMove.size()),
                       "operator", operator != null ? operator.getFullName() : "N/A"));

        return transferMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransferById(UUID id) {
        WarehouseTransferEntity entity = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado: " + id));
        return transferMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferSummaryResponse> getTransfers(UUID organizationId, UUID branchId, String status, String search) {
        TransferStatus trStatus = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                trStatus = TransferStatus.valueOf(status.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        List<WarehouseTransferEntity> list = transferJpaRepository.findAll(
                WarehouseTransferSpecification.withFilters(organizationId, branchId, trStatus, cleanSearch));
        return list.stream().map(transferMapper::toSummaryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransferResponse cancelTransfer(UUID id, CancelTransferRequest request) {
        WarehouseTransferEntity transfer = transferRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Traspaso no encontrado: " + id));

        if (transfer.getStatus() == TransferStatus.CANCELLED) {
            throw new ValidationException("El traspaso ya se encuentra cancelado.");
        }

        UserEntity admin = validateUserCredentials(request.getAdminUsername(), request.getAdminPassword());

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        transfer.setStatus(TransferStatus.CANCELLED);
        transfer.setCancelledAt(now);
        transfer.setCancellationReason(request.getReason());
        transfer.setCancelledBy(admin.getFirstName() + " " + admin.getLastName());

        // Revert items back to originLocation
        if (transfer.getItems() != null) {
            for (WarehouseTransferItemEntity ti : transfer.getItems()) {
                InventoryItemEntity item = ti.getItem();
                if (item != null && item.getLocation() != null && item.getLocation().getId().equals(transfer.getDestinationLocation().getId())) {
                    item.setLocation(transfer.getOriginLocation());
                    inventoryItemRepositoryPort.save(item);

                    InventoryMovementEntity comp = InventoryMovementEntity.builder()
                            .item(item)
                            .fromLocation(transfer.getDestinationLocation())
                            .toLocation(transfer.getOriginLocation())
                            .user(admin)
                            .type(MovementType.TRANSFER)
                            .reason("Compensación por cancelación de Traspaso: " + transfer.getFolio() + " (" + request.getReason() + ")")
                            .createdAt(now)
                            .build();
                    inventoryMovementRepositoryPort.save(comp);
                }
            }
        }

        WarehouseTransferEntity saved = transferRepositoryPort.save(transfer);

        logAudit(saved.getId(), "TRASPASO_CANCELADO",
                Map.of("status", "COMPLETED"),
                Map.of("status", "CANCELLED", "cancelledBy", saved.getCancelledBy(), "reason", request.getReason()));

        return transferMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementAuditResponse> getAuditLogs(UUID id) {
        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("TRANSFER", id);
        return logs.stream().map(this::mapToAuditResponse).collect(Collectors.toList());
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────────

    private UserEntity validateUserCredentials(String username, String password) {
        UserEntity user = userRepositoryPort.findByUsername(username.trim())
                .orElseThrow(() -> new ValidationException("Credenciales inválidas: usuario '" + username + "' no encontrado."));

        if (!user.getIsEnabled()) {
            throw new ValidationException("El usuario '" + username + "' está inactivo o deshabilitado.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ValidationException("Contraseña incorrecta para el usuario '" + username + "'.");
        }

        return user;
    }

    private void logAudit(UUID entityId, String action, Map<String, Object> before, Map<String, Object> after) {
        try {
            String username = securityAuditHelper.getCurrentUsername();
            UserEntity activeUser = userRepositoryPort.findByUsername(username).orElse(null);
            if (activeUser != null) {
                auditService.log(activeUser, action, "TRANSFER", entityId, before, after);
            }
        } catch (Exception e) {
            log.warn("Could not record relational audit log for transfer {}: {}", entityId, e.getMessage());
        }
    }

    private MovementAuditResponse mapToAuditResponse(AuditLogEntity log) {
        List<MovementAuditResponse.MovementAuditDetailResponse> details = log.getDetails() != null ?
                log.getDetails().stream().map(d -> MovementAuditResponse.MovementAuditDetailResponse.builder()
                        .fieldName(d.getFieldName())
                        .oldValue(d.getOldValue())
                        .newValue(d.getNewValue())
                        .build()).collect(Collectors.toList()) : List.of();

        String actionLabel = switch (log.getAction()) {
            case "TRASPASO_REGISTRADO" -> "Reubicación de Inventario Confirmada";
            case "TRASPASO_CANCELADO" -> "Cancelación de Reubicación de Inventario";
            default -> log.getAction();
        };

        String username = "Usuario Sistema";
        if (log.getUserId() != null) {
            username = userRepositoryPort.findById(log.getUserId())
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("Usuario " + log.getUserId());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String formattedTimestamp = log.getCreatedAt() != null ? log.getCreatedAt().format(formatter) : "";

        return MovementAuditResponse.builder()
                .id(log.getLogId() != null ? log.getLogId().toString() : UUID.randomUUID().toString())
                .action(log.getAction())
                .actionLabel(actionLabel)
                .username(username)
                .timestamp(formattedTimestamp)
                .details(details)
                .build();
    }
}
