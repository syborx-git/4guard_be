package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.outbound.CancelOutboundRequest;
import com.fourguard.wms.application.dto.request.outbound.CreateOutboundRequest;
import com.fourguard.wms.application.dto.response.outbound.InventoryBatchResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundSummaryResponse;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.application.mapper.WarehouseOutboundMapper;
import com.fourguard.wms.domain.enums.InventoryState;
import com.fourguard.wms.domain.enums.MovementType;
import com.fourguard.wms.domain.enums.OutboundStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.WarehouseOutboundUseCase;
import com.fourguard.wms.domain.ports.out.*;
import com.fourguard.wms.infrastructure.persistence.entity.*;
import com.fourguard.wms.infrastructure.persistence.repository.InventoryItemJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.WarehouseOutboundJpaRepository;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseOutboundService implements WarehouseOutboundUseCase {

    private final WarehouseOutboundRepositoryPort outboundRepositoryPort;
    private final WarehouseOutboundJpaRepository outboundJpaRepository;
    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final ClientDestinationRepositoryPort clientDestinationRepositoryPort;
    private final CarrierRepositoryPort carrierRepositoryPort;
    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;
    private final InventoryItemJpaRepository inventoryItemJpaRepository;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final AuditService auditService;
    private final SecurityAuditHelper securityAuditHelper;
    private final PasswordEncoder passwordEncoder;
    private final WarehouseOutboundMapper outboundMapper;

    @Override
    @Transactional
    public OutboundResponse createOutbound(CreateOutboundRequest request) {
        log.info("Creating outbound dispatch for client: {}, destination: {}", request.getClientId(), request.getDestinationId());

        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada: " + request.getOrganizationId()));

        BranchEntity branch = branchRepositoryPort.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada: " + request.getBranchId()));

        ClientEntity client = clientRepositoryPort.findById(request.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado: " + request.getClientId()));

        ClientDestinationEntity destination = null;
        if (request.getDestinationId() != null) {
            destination = clientDestinationRepositoryPort.findById(request.getDestinationId()).orElse(null);
        }

        CarrierEntity carrier = null;
        if (request.getCarrierId() != null) {
            carrier = carrierRepositoryPort.findById(request.getCarrierId()).orElse(null);
        }

        // Generate consecutive folio: SAL-YYYY-XXXXXX
        long seq = outboundRepositoryPort.nextFolioSequenceValue();
        int year = LocalDate.now().getYear();
        String folio = String.format("SAL-%d-%06d", year, seq);

        // Fetch and validate selected items
        List<InventoryItemEntity> itemsToDispatch = new ArrayList<>();
        for (UUID itemId : request.getSelectedItemIds()) {
            InventoryItemEntity item = inventoryItemRepositoryPort.findById(itemId)
                    .orElseThrow(() -> new EntityNotFoundException("Ítem de inventario no encontrado: " + itemId));

            if (item.getState() != InventoryState.AVAILABLE) {
                throw new ValidationException("La tarima " + item.getSscc() + " no está disponible para despacho (Estado: " + item.getState() + ")");
            }

            if (!item.getClient().getId().equals(client.getId())) {
                throw new ValidationException("La tarima " + item.getSscc() + " no pertenece al cliente seleccionado.");
            }
            itemsToDispatch.add(item);
        }

        Set<UUID> distinctSkuIds = itemsToDispatch.stream().map(i -> i.getSku().getId()).collect(Collectors.toSet());
        double totalPieces = itemsToDispatch.stream().mapToDouble(i -> i.getQuantity() != null ? i.getQuantity().doubleValue() : 0.0).sum();

        String destName = request.getDestinationName();
        if (destName == null && destination != null) {
            destName = destination.getPlantName();
        }

        String destAddress = request.getDestinationAddress();
        if (destAddress == null && destination != null) {
            destAddress = destination.getFullAddress() != null ? destination.getFullAddress() : "";
        }

        WarehouseOutboundEntity outbound = WarehouseOutboundEntity.builder()
                .organization(organization)
                .branch(branch)
                .folio(folio)
                .status(OutboundStatus.COMPLETED)
                .client(client)
                .destination(destination)
                .destinationName(destName)
                .destinationAddress(destAddress)
                .carrier(carrier)
                .transportType(request.getTransportType() != null ? request.getTransportType().toUpperCase().trim() : "TRAILER")
                .driverName(request.getDriverName())
                .economicNumber(request.getEconomicNumber())
                .tractorPlates(request.getTractorPlates())
                .boxPlates(request.getBoxPlates())
                .sealNumber(request.getSealNumber())
                .remisionNo(request.getRemisionNo())
                .totalPallets(itemsToDispatch.size())
                .totalPieces(totalPieces)
                .distinctSkus(distinctSkuIds.size())
                .build();

        UserEntity activeUser = null;
        try {
            String currentUsername = securityAuditHelper.getCurrentUsername();
            activeUser = userRepositoryPort.findByUsername(currentUsername).orElse(null);
        } catch (Exception ignored) {}

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<WarehouseOutboundItemEntity> outboundItems = new ArrayList<>();

        for (InventoryItemEntity item : itemsToDispatch) {
            // Update state to DISPATCHED
            item.setState(InventoryState.DISPATCHED);
            inventoryItemRepositoryPort.save(item);

            String locCode = item.getLocation() != null ? item.getLocation().getCode() : "N/A";

            outboundItems.add(WarehouseOutboundItemEntity.builder()
                    .outbound(outbound)
                    .item(item)
                    .pieces(item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0)
                    .palletCode(item.getSscc())
                    .lotNumber(item.getBatchNumber())
                    .expirationDate(item.getExpirationDate())
                    .locationCode(locCode)
                    .build());

            // Log Inventory Movement EXIT
            if (activeUser != null) {
                InventoryMovementEntity movement = InventoryMovementEntity.builder()
                        .item(item)
                        .fromLocation(item.getLocation())
                        .user(activeUser)
                        .type(MovementType.EXIT)
                        .reason("Despacho Outbound Folio: " + folio + " - Remisión: " + request.getRemisionNo())
                        .createdAt(now)
                        .build();
                inventoryMovementRepositoryPort.save(movement);
            }
        }
        outbound.setItems(outboundItems);

        WarehouseOutboundEntity saved = outboundRepositoryPort.save(outbound);

        logAudit(saved.getId(), "SALIDA_REGISTRADA",
                Map.of(),
                Map.of("folio", folio,
                       "client", client.getName(),
                       "destination", destName != null ? destName : "N/A",
                       "carrier", carrier != null ? carrier.getName() : "N/A",
                       "sealNumber", request.getSealNumber(),
                       "totalPallets", String.valueOf(itemsToDispatch.size()),
                       "totalPieces", String.valueOf(totalPieces)));

        return outboundMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OutboundResponse getOutboundById(UUID id) {
        WarehouseOutboundEntity entity = outboundRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salida no encontrada: " + id));
        return outboundMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboundSummaryResponse> getOutbounds(UUID organizationId, UUID branchId, String status, String search) {
        OutboundStatus obStatus = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                obStatus = OutboundStatus.valueOf(status.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        List<WarehouseOutboundEntity> list = outboundJpaRepository.findWithFilters(organizationId, branchId, obStatus, cleanSearch);
        return list.stream().map(outboundMapper::toSummaryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OutboundResponse cancelOutbound(UUID id, CancelOutboundRequest request) {
        WarehouseOutboundEntity outbound = outboundRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salida no encontrada: " + id));

        if (outbound.getStatus() == OutboundStatus.CANCELLED) {
            throw new ValidationException("La salida ya se encuentra cancelada.");
        }

        UserEntity admin = validateUserCredentials(request.getAdminUsername(), request.getAdminPassword());

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        outbound.setStatus(OutboundStatus.CANCELLED);
        outbound.setCancelledAt(now);
        outbound.setCancellationReason(request.getReason());
        outbound.setCancelledBy(admin.getFirstName() + " " + admin.getLastName());

        // Revert items back to AVAILABLE
        if (outbound.getItems() != null) {
            for (WarehouseOutboundItemEntity oi : outbound.getItems()) {
                InventoryItemEntity item = oi.getItem();
                if (item != null && item.getState() == InventoryState.DISPATCHED) {
                    item.setState(InventoryState.AVAILABLE);
                    inventoryItemRepositoryPort.save(item);

                    InventoryMovementEntity comp = InventoryMovementEntity.builder()
                            .item(item)
                            .toLocation(item.getLocation())
                            .user(admin)
                            .type(MovementType.ENTRY)
                            .reason("Compensación por cancelación de Salida: " + outbound.getFolio() + " (" + request.getReason() + ")")
                            .createdAt(now)
                            .build();
                    inventoryMovementRepositoryPort.save(comp);
                }
            }
        }

        WarehouseOutboundEntity saved = outboundRepositoryPort.save(outbound);

        logAudit(saved.getId(), "SALIDA_CANCELADA",
                Map.of("status", "COMPLETED"),
                Map.of("status", "CANCELLED", "cancelledBy", saved.getCancelledBy(), "reason", request.getReason()));

        return outboundMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> getInventoryBatches(UUID organizationId, UUID branchId, UUID clientId, UUID skuId) {
        List<InventoryItemEntity> availableItems;
        if (skuId != null) {
            availableItems = inventoryItemJpaRepository.findAvailableBySkuOrderedByFefo(skuId);
        } else {
            availableItems = inventoryItemJpaRepository.findAll().stream()
                    .filter(i -> i.getState() == InventoryState.AVAILABLE)
                    .collect(Collectors.toList());
        }

        // Filter by organization, branch and client if provided
        if (organizationId != null) {
            availableItems = availableItems.stream()
                    .filter(i -> i.getOrganization() != null && i.getOrganization().getId().equals(organizationId))
                    .collect(Collectors.toList());
        }
        if (branchId != null) {
            availableItems = availableItems.stream()
                    .filter(i -> i.getBranch() != null && i.getBranch().getId().equals(branchId))
                    .collect(Collectors.toList());
        }
        if (clientId != null) {
            availableItems = availableItems.stream()
                    .filter(i -> i.getClient() != null && i.getClient().getId().equals(clientId))
                    .collect(Collectors.toList());
        }

        // Group by (sapFolio / remisionNo, lotNumber, expirationDate, sku)
        Map<String, List<InventoryItemEntity>> grouped = availableItems.stream().collect(
                Collectors.groupingBy(i -> {
                    String rem = i.getSapFolio() != null ? i.getSapFolio() : "REM-SIN-FOLIO";
                    String lot = i.getBatchNumber() != null ? i.getBatchNumber() : "LOTE-GENERAL";
                    String exp = i.getExpirationDate() != null ? i.getExpirationDate().toString() : "SIN-CADUCIDAD";
                    String sku = i.getSku() != null ? i.getSku().getId().toString() : "SKU-NIL";
                    return rem + "___" + lot + "___" + exp + "___" + sku;
                })
        );

        List<InventoryBatchResponse> batches = new ArrayList<>();
        for (List<InventoryItemEntity> groupItems : grouped.values()) {
            if (groupItems.isEmpty()) continue;
            InventoryItemEntity first = groupItems.get(0);

            double totalPieces = groupItems.stream().mapToDouble(i -> i.getQuantity() != null ? i.getQuantity().doubleValue() : 0.0).sum();
            String locCode = first.getLocation() != null ? first.getLocation().getCode() : "N/A";

            List<InventoryBatchResponse.BatchPalletItemResponse> palletResponses = groupItems.stream().map(item -> {
                String pLoc = item.getLocation() != null ? item.getLocation().getCode() : "N/A";
                return InventoryBatchResponse.BatchPalletItemResponse.builder()
                        .itemId(item.getId())
                        .palletCode(item.getSscc())
                        .skuCode(item.getSku() != null ? item.getSku().getCode() : "")
                        .description(item.getSku() != null ? item.getSku().getName() : "")
                        .lotNumber(item.getBatchNumber())
                        .expirationDate(item.getExpirationDate())
                        .pieces(item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0)
                        .palletTypeId("MADERA_ESTANDAR")
                        .palletTypeLabel("Madera Estándar")
                        .locationCode(pLoc)
                        .build();
            }).collect(Collectors.toList());

            batches.add(InventoryBatchResponse.builder()
                    .remisionNo(first.getSapFolio() != null ? first.getSapFolio() : "REM-0000")
                    .clientId(first.getClient() != null ? first.getClient().getId() : null)
                    .clientName(first.getClient() != null ? first.getClient().getName() : "")
                    .skuId(first.getSku() != null ? first.getSku().getId() : null)
                    .skuCode(first.getSku() != null ? first.getSku().getCode() : "")
                    .productName(first.getSku() != null ? first.getSku().getName() : "")
                    .lotNumber(first.getBatchNumber())
                    .manufacturingDate(first.getManufacturingDate())
                    .expirationDate(first.getExpirationDate())
                    .availablePallets(groupItems.size())
                    .totalPieces(totalPieces)
                    .locationCode(locCode)
                    .isFifoSuggested(false)
                    .pallets(palletResponses)
                    .build());
        }

        // Sort by expirationDate ASC and mark the oldest as isFifoSuggested = true
        batches.sort(Comparator.comparing(
                InventoryBatchResponse::getExpirationDate,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        if (!batches.isEmpty()) {
            batches.get(0).setIsFifoSuggested(true);
        }

        return batches;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementAuditResponse> getAuditLogs(UUID id) {
        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("OUTBOUND", id);
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
                auditService.log(activeUser, action, "OUTBOUND", entityId, before, after);
            }
        } catch (Exception e) {
            log.warn("Could not record relational audit log for outbound {}: {}", entityId, e.getMessage());
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
            case "SALIDA_REGISTRADA" -> "Despacho Outbound Confirmado";
            case "SALIDA_CANCELADA" -> "Cancelación de Despacho Outbound";
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
