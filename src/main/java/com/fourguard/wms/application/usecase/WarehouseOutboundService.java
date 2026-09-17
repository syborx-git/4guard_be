package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.outbound.CancelOutboundRequest;
import com.fourguard.wms.application.dto.request.outbound.CreateOutboundRequest;
import com.fourguard.wms.application.dto.request.outbound.UpdateOutboundRequest;
import com.fourguard.wms.application.dto.request.outbound.ValidatePalletsRequest;
import com.fourguard.wms.application.dto.response.outbound.InventoryBatchResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundSummaryResponse;
import com.fourguard.wms.application.dto.response.outbound.ScanPalletResponse;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.application.mapper.WarehouseOutboundMapper;
import com.fourguard.wms.domain.enums.InventoryState;
import com.fourguard.wms.domain.enums.LocationType;
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
public class WarehouseOutboundService implements WarehouseOutboundUseCase {

    private final WarehouseOutboundRepositoryPort outboundRepositoryPort;
    private final WarehouseOutboundJpaRepository outboundJpaRepository;
    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final ClientDestinationRepositoryPort clientDestinationRepositoryPort;
    private final CarrierRepositoryPort carrierRepositoryPort;
    private final LocationRepositoryPort locationRepositoryPort;
    private final ForkliftOperatorRepositoryPort forkliftOperatorRepositoryPort;
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

        // Resolución flexible de Cliente
        ClientEntity client = null;
        if (request.getClientId() != null) {
            client = clientRepositoryPort.findById(request.getClientId()).orElse(null);
        }
        if (client == null) {
            List<ClientEntity> orgClients = clientRepositoryPort.findByOrganizationId(organization.getId());
            if (request.getClientCode() != null && !request.getClientCode().isBlank()) {
                String searchCode = request.getClientCode().trim();
                client = orgClients.stream()
                        .filter(c -> (c.getExternalId() != null && c.getExternalId().equalsIgnoreCase(searchCode)) ||
                                     (c.getTaxId() != null && c.getTaxId().equalsIgnoreCase(searchCode)) ||
                                     (c.getName() != null && c.getName().equalsIgnoreCase(searchCode)))
                        .findFirst()
                        .orElse(null);
            }
            if (client == null && request.getClientName() != null && !request.getClientName().isBlank()) {
                String searchName = request.getClientName().trim();
                client = orgClients.stream()
                        .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(searchName))
                        .findFirst()
                        .orElse(null);
            }
            if (client == null && !orgClients.isEmpty()) {
                client = orgClients.get(0);
            }
        }
        if (client == null) {
            throw new ValidationException("No se encontró un cliente válido registrado para la organización.");
        }

        ClientDestinationEntity destination = null;
        if (request.getDestinationId() != null) {
            destination = clientDestinationRepositoryPort.findById(request.getDestinationId()).orElse(null);
        }

        // Resolución flexible de Línea Transportista
        CarrierEntity carrier = null;
        if (request.getCarrierId() != null) {
            carrier = carrierRepositoryPort.findById(request.getCarrierId()).orElse(null);
        }
        if (carrier == null) {
            List<CarrierEntity> orgCarriers = carrierRepositoryPort.findByOrganizationId(organization.getId());
            if (request.getCarrierLineCode() != null && !request.getCarrierLineCode().isBlank()) {
                String searchCode = request.getCarrierLineCode().trim();
                carrier = orgCarriers.stream()
                        .filter(c -> (c.getTaxId() != null && c.getTaxId().equalsIgnoreCase(searchCode)) ||
                                     (c.getName() != null && c.getName().equalsIgnoreCase(searchCode)) ||
                                     (c.getTradeName() != null && c.getTradeName().equalsIgnoreCase(searchCode)))
                        .findFirst()
                        .orElse(null);
            }
            if (carrier == null && request.getCarrierLine() != null && !request.getCarrierLine().isBlank()) {
                String searchLine = request.getCarrierLine().trim();
                carrier = orgCarriers.stream()
                        .filter(c -> (c.getName() != null && c.getName().equalsIgnoreCase(searchLine)) ||
                                     (c.getTradeName() != null && c.getTradeName().equalsIgnoreCase(searchLine)))
                        .findFirst()
                        .orElse(null);
            }
            if (carrier == null && request.getCarrierName() != null && !request.getCarrierName().isBlank()) {
                String searchName = request.getCarrierName().trim();
                carrier = orgCarriers.stream()
                        .filter(c -> (c.getName() != null && c.getName().equalsIgnoreCase(searchName)) ||
                                     (c.getTradeName() != null && c.getTradeName().equalsIgnoreCase(searchName)))
                        .findFirst()
                        .orElse(null);
            }
        }

        LocationEntity ramp = null;
        if (request.getRampId() != null) {
            ramp = locationRepositoryPort.findById(request.getRampId()).orElse(null);
        }
        if (ramp == null && request.getRampNumber() != null && branch.getId() != null) {
            String formattedCode = String.format("LOC-RAMP-%02d", request.getRampNumber());
            ramp = locationRepositoryPort.findByBranchIdAndCode(branch.getId(), formattedCode).orElse(null);
            if (ramp == null) {
                ramp = locationRepositoryPort.findFirstByCode(formattedCode).orElse(null);
            }
        }
        if (ramp == null && request.getRampCode() != null && !request.getRampCode().isBlank()) {
            String cleanCode = request.getRampCode().trim();
            ramp = locationRepositoryPort.findByBranchIdAndCode(branch.getId(), cleanCode).orElse(null);
            if (ramp == null) {
                ramp = locationRepositoryPort.findFirstByCode(cleanCode).orElse(null);
            }
        }
        if (ramp == null && request.getRampNumber() != null && branch.getId() != null) {
            ramp = locationRepositoryPort.findByBranchId(branch.getId()).stream()
                    .filter(l -> l.getType() == LocationType.RAMP)
                    .findFirst()
                    .orElse(null);
        }

        if (ramp != null && Boolean.TRUE.equals(ramp.getIsBlocked())) {
            throw new ValidationException("La rampa seleccionada (" + (ramp.getCode() != null ? ramp.getCode() : "Rampa") + ") se encuentra bloqueada: " + (ramp.getBlockReason() != null ? ramp.getBlockReason() : "Mantenimiento / Bloqueada"));
        }

        ForkliftOperatorEntity operator = null;
        if (request.getForkliftOperatorId() != null) {
            operator = forkliftOperatorRepositoryPort.findById(request.getForkliftOperatorId()).orElse(null);
        }
        if (operator == null && request.getForkliftOperatorName() != null && !request.getForkliftOperatorName().isBlank()) {
            String opName = request.getForkliftOperatorName().trim().toLowerCase();
            operator = forkliftOperatorRepositoryPort.findAll().stream()
                    .filter(o -> o.getFullName() != null && o.getFullName().toLowerCase().contains(opName))
                    .findFirst()
                    .orElse(null);
        }

        // Determine target lifecycle status
        OutboundStatus targetStatus = OutboundStatus.REGISTERED;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                targetStatus = OutboundStatus.valueOf(request.getStatus().toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        } else if (request.getSelectedItemIds() != null && !request.getSelectedItemIds().isEmpty()) {
            targetStatus = OutboundStatus.COMPLETED;
        }

        // Generate consecutive folio: SAL-YYYY-XXXXXX
        long seq = outboundRepositoryPort.nextFolioSequenceValue();
        int year = LocalDate.now().getYear();
        String folio = String.format("SAL-%d-%06d", year, seq);

        List<InventoryItemEntity> itemsToDispatch = new ArrayList<>();
        if (request.getSelectedItemIds() != null && !request.getSelectedItemIds().isEmpty()) {
            itemsToDispatch = inventoryItemJpaRepository.findAllByIdInWithDetails(request.getSelectedItemIds());
            if (itemsToDispatch.size() != request.getSelectedItemIds().size()) {
                Set<UUID> foundIds = itemsToDispatch.stream().map(item -> item.getId()).collect(Collectors.toSet());
                List<UUID> missingIds = request.getSelectedItemIds().stream()
                        .filter(id -> !foundIds.contains(id))
                        .collect(Collectors.toList());
                throw new EntityNotFoundException("Uno o más ítems de inventario no fueron encontrados: " + missingIds);
            }

            for (InventoryItemEntity item : itemsToDispatch) {
                if (item.getState() != InventoryState.AVAILABLE && item.getState() != InventoryState.EXPIRED) {
                    throw new ValidationException("La tarima " + item.getSscc() + " no está disponible para despacho (Estado actual: " + item.getState() + ")");
                }
                if (item.getOrganization() != null && !item.getOrganization().getId().equals(organization.getId())) {
                    throw new ValidationException("La tarima " + item.getSscc() + " pertenece a otra organización.");
                }
                if (item.getBranch() != null && !item.getBranch().getId().equals(branch.getId())) {
                    throw new ValidationException("La tarima " + item.getSscc() + " pertenece a otra sucursal.");
                }
                if (item.getClient() != null && !item.getClient().getId().equals(client.getId())) {
                    throw new ValidationException("La tarima " + item.getSscc() + " pertenece a otro cliente (" + item.getClient().getName() + ").");
                }
            }
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

        String remisionNumber = (request.getRemisionNo() != null && !request.getRemisionNo().isBlank())
                ? request.getRemisionNo().trim()
                : "REM-" + folio;

        WarehouseOutboundEntity outbound = WarehouseOutboundEntity.builder()
                .organization(organization)
                .branch(branch)
                .folio(folio)
                .status(targetStatus)
                .client(client)
                .destination(destination)
                .destinationName(destName)
                .destinationAddress(destAddress)
                .carrier(carrier)
                .ramp(ramp)
                .forkliftOperator(operator)
                .transportType(request.getTransportType() != null ? request.getTransportType().toUpperCase().trim() : "TRAILER")
                .driverName(request.getDriverName())
                .economicNumber(request.getEconomicNumber())
                .boxEconomicNumber(request.getBoxEconomicNumber())
                .tractorPlates(request.getTractorPlates())
                .boxPlates(request.getBoxPlates())
                .sealNumber(request.getSealNumber())
                .remisionNo(remisionNumber)
                .observations(request.getObservations())
                .totalPallets(itemsToDispatch.size())
                .totalPieces(BigDecimal.valueOf(totalPieces))
                .distinctSkus(distinctSkuIds.size())
                .build();

        String loggedUser = securityAuditHelper.getCurrentUsername();
        String currentUsername = (loggedUser != null && !loggedUser.isBlank()) ? loggedUser : "admin";
        UserEntity activeUser = null;
        try {
            activeUser = userRepositoryPort.findByUsernameOrEmail(currentUsername)
                    .orElse(null);
            if (activeUser == null) {
                activeUser = userRepositoryPort.findAll().stream()
                        .filter(u -> Boolean.TRUE.equals(u.getIsEnabled()))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception ignored) {}

        outbound.setCreatedBy(currentUsername);
        outbound.setUpdatedBy(currentUsername);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (targetStatus == OutboundStatus.COMPLETED) {
            outbound.setCompletedAt(now);
            outbound.setLeaderAuthorizedBy(currentUsername);
        }

        List<WarehouseOutboundItemEntity> outboundItems = new ArrayList<>();

        for (InventoryItemEntity item : itemsToDispatch) {
            if (targetStatus == OutboundStatus.COMPLETED) {
                // Update state to DISPATCHED
                item.setState(InventoryState.DISPATCHED);
                inventoryItemRepositoryPort.save(item);

                // Decrementar ocupación de la bahía física origen
                if (item.getLocation() != null && item.getLocation().getId() != null) {
                    locationRepositoryPort.decrementOccupancy(item.getLocation().getId(), 1);
                }
            }

            String locCode = item.getLocation() != null ? item.getLocation().getCode() : "N/A";

            outboundItems.add(WarehouseOutboundItemEntity.builder()
                    .outbound(outbound)
                    .item(item)
                    .pieces(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO)
                    .palletCode(item.getSscc())
                    .lotNumber(item.getBatchNumber())
                    .expirationDate(item.getExpirationDate())
                    .locationCode(locCode)
                    .build());

            // Log Inventory Movement EXIT if completed
            if (targetStatus == OutboundStatus.COMPLETED && activeUser != null) {
                InventoryMovementEntity movement = InventoryMovementEntity.builder()
                        .item(item)
                        .fromLocation(item.getLocation())
                        .user(activeUser)
                        .type(MovementType.EXIT)
                        .reason("Despacho Outbound Folio: " + folio + " - Remisión Salida: " + remisionNumber + " [Entrada Origen: " + (item.getSapFolio() != null ? item.getSapFolio() : "N/A") + "]")
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
                       "status", targetStatus.name(),
                       "client", client.getName(),
                       "destination", destName != null ? destName : "N/A",
                       "carrier", carrier != null ? carrier.getName() : "N/A",
                       "ramp", ramp != null ? (ramp.getCode() != null ? ramp.getCode() : (ramp.getName() != null ? ramp.getName() : "Rampa")) : "N/A",
                       "forkliftOperator", operator != null ? operator.getFullName() : (request.getForkliftOperatorName() != null ? request.getForkliftOperatorName() : "N/A"),
                       "sealNumber", request.getSealNumber() != null ? request.getSealNumber() : "N/A",
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

        List<WarehouseOutboundEntity> list = outboundJpaRepository.findAll(
                WarehouseOutboundSpecification.withFilters(organizationId, branchId, obStatus, cleanSearch));
        return list.stream().map(outboundMapper::toSummaryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OutboundResponse updateOutbound(UUID id, UpdateOutboundRequest request) {
        WarehouseOutboundEntity outbound = outboundRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salida no encontrada: " + id));

        if (outbound.getStatus() == OutboundStatus.CANCELLED) {
            throw new ValidationException("No se puede modificar una salida cancelada.");
        }

        Map<String, Object> oldValues = new LinkedHashMap<>();
        Map<String, Object> newValues = new LinkedHashMap<>();

        // 1. Status transition
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                OutboundStatus newStatus = OutboundStatus.valueOf(request.getStatus().toUpperCase().trim());
                if (newStatus != outbound.getStatus()) {
                    oldValues.put("status", outbound.getStatus().name());
                    newValues.put("status", newStatus.name());
                    outbound.setStatus(newStatus);

                    if (newStatus == OutboundStatus.COMPLETED) {
                        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                        outbound.setCompletedAt(now);
                        String currentUsername = securityAuditHelper.getCurrentUsername();
                        outbound.setLeaderAuthorizedBy(currentUsername != null ? currentUsername : "Admin");

                        // Update inventory items state to DISPATCHED y decrementar ocupación
                        if (outbound.getItems() != null) {
                            for (WarehouseOutboundItemEntity oi : outbound.getItems()) {
                                if (oi.getItem() != null && oi.getItem().getState() != InventoryState.DISPATCHED) {
                                    oi.getItem().setState(InventoryState.DISPATCHED);
                                    inventoryItemRepositoryPort.save(oi.getItem());

                                    if (oi.getItem().getLocation() != null && oi.getItem().getLocation().getId() != null) {
                                        locationRepositoryPort.decrementOccupancy(oi.getItem().getLocation().getId(), 1);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }

        // 2. Ramp assignment
        if (request.getRampId() != null || request.getRampNumber() != null) {
            LocationEntity ramp = null;
            if (request.getRampId() != null) {
                ramp = locationRepositoryPort.findById(request.getRampId()).orElse(null);
            } else if (request.getRampNumber() != null && outbound.getBranch() != null) {
                String formattedCode = String.format("LOC-RAMP-%02d", request.getRampNumber());
                ramp = locationRepositoryPort.findByBranchIdAndCode(outbound.getBranch().getId(), formattedCode).orElse(null);
                if (ramp == null) {
                    ramp = locationRepositoryPort.findFirstByCode(formattedCode).orElse(null);
                }
            }
            if (ramp != null) {
                String oldRamp = outbound.getRamp() != null ? (outbound.getRamp().getCode() != null ? outbound.getRamp().getCode() : outbound.getRamp().getName()) : "N/A";
                outbound.setRamp(ramp);
                oldValues.put("ramp", oldRamp);
                newValues.put("ramp", ramp.getCode() != null ? ramp.getCode() : (ramp.getName() != null ? ramp.getName() : "Rampa"));
            }
        }

        // 3. Forklift Operator assignment
        if (request.getForkliftOperatorId() != null) {
            ForkliftOperatorEntity operator = forkliftOperatorRepositoryPort.findById(request.getForkliftOperatorId()).orElse(null);
            if (operator != null) {
                String oldOp = outbound.getForkliftOperator() != null ? outbound.getForkliftOperator().getFullName() : "N/A";
                outbound.setForkliftOperator(operator);
                oldValues.put("forkliftOperator", oldOp);
                newValues.put("forkliftOperator", operator.getFullName());
            }
        }

        // 4. Vehicle & Driver data
        if (request.getDriverName() != null && !request.getDriverName().isBlank()) {
            oldValues.put("driverName", outbound.getDriverName());
            newValues.put("driverName", request.getDriverName());
            outbound.setDriverName(request.getDriverName());
        }
        if (request.getTractorPlates() != null && !request.getTractorPlates().isBlank()) {
            oldValues.put("tractorPlates", outbound.getTractorPlates());
            newValues.put("tractorPlates", request.getTractorPlates());
            outbound.setTractorPlates(request.getTractorPlates());
        }
        if (request.getBoxPlates() != null && !request.getBoxPlates().isBlank()) {
            oldValues.put("boxPlates", outbound.getBoxPlates());
            newValues.put("boxPlates", request.getBoxPlates());
            outbound.setBoxPlates(request.getBoxPlates());
        }
        if (request.getEconomicNumber() != null) {
            outbound.setEconomicNumber(request.getEconomicNumber());
        }
        if (request.getBoxEconomicNumber() != null) {
            outbound.setBoxEconomicNumber(request.getBoxEconomicNumber());
        }

        // 5. Seal number
        if (request.getSealNumber() != null && !request.getSealNumber().isBlank()) {
            oldValues.put("sealNumber", outbound.getSealNumber());
            newValues.put("sealNumber", request.getSealNumber());
            outbound.setSealNumber(request.getSealNumber());
        }

        // 6. Observations
        if (request.getObservations() != null) {
            outbound.setObservations(request.getObservations());
        }

        String currentUsername = securityAuditHelper.getCurrentUsername();
        outbound.setUpdatedBy(currentUsername != null ? currentUsername : "Admin");

        WarehouseOutboundEntity saved = outboundRepositoryPort.save(outbound);

        if (!newValues.isEmpty()) {
            logAudit(saved.getId(), "SALIDA_MODIFICADA", oldValues, newValues);
        }

        return outboundMapper.toResponse(saved);
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
    public List<InventoryBatchResponse> getInventoryBatches(UUID organizationId, UUID branchId, UUID clientId, UUID skuId, String search) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        // High performance single DB query with JOIN FETCH
        List<InventoryItemEntity> availableItems = inventoryItemJpaRepository.findAvailableBatchesWithFilters(
                organizationId, branchId, clientId, skuId, cleanSearch);

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
    public ScanPalletResponse scanPallet(String barcode, UUID organizationId, UUID branchId) {
        if (barcode == null || barcode.isBlank()) {
            throw new ValidationException("El código de barras / SSCC no puede estar vacío.");
        }
        String cleanBarcode = barcode.trim();
        InventoryItemEntity item = inventoryItemJpaRepository.findAvailableBySsccOrExternalUa(cleanBarcode, organizationId, branchId)
                .or(() -> {
                    try {
                        UUID itemId = UUID.fromString(cleanBarcode);
                        return inventoryItemJpaRepository.findById(itemId);
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElseThrow(() -> new EntityNotFoundException("Tarima o código de barras no encontrado en inventario disponible: " + cleanBarcode));

        return mapToScanPalletResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanPalletResponse> validatePallets(ValidatePalletsRequest request) {
        if (request.getBarcodes() == null || request.getBarcodes().isEmpty()) {
            return List.of();
        }
        List<InventoryItemEntity> items = inventoryItemJpaRepository.findByBarcodesIn(
                request.getBarcodes(),
                request.getOrganizationId(),
                request.getBranchId());

        return items.stream().map(this::mapToScanPalletResponse).collect(Collectors.toList());
    }

    private ScanPalletResponse mapToScanPalletResponse(InventoryItemEntity item) {
        LocalDate exp = item.getExpirationDate();
        long daysRemaining = 999;
        String pabloStatus = "OPTIMAL";
        String pabloLabel = "Óptimo";

        if (exp != null) {
            daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), exp);
            if (daysRemaining <= 0) {
                pabloStatus = "EXPIRED";
                pabloLabel = "Caduco / Bloqueado (" + daysRemaining + "d)";
            } else if (daysRemaining <= 30) {
                pabloStatus = "PABLO_ALERT";
                pabloLabel = "Alerta Pablo (" + daysRemaining + "d)";
            } else {
                pabloStatus = "OPTIMAL";
                pabloLabel = "Óptimo (" + daysRemaining + "d)";
            }
        }

        String locCode = item.getLocation() != null ? item.getLocation().getCode() : "N/A";
        String skuCode = item.getSku() != null ? item.getSku().getCode() : "";
        String skuName = item.getSku() != null ? item.getSku().getName() : "";
        String category = item.getSku() != null && item.getSku().getCategory() != null ? item.getSku().getCategory() : "GENERAL";
        String clientName = item.getClient() != null ? item.getClient().getName() : "";
        Double pieces = item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0;

        return ScanPalletResponse.builder()
                .itemId(item.getId())
                .palletCode(item.getSscc())
                .externalUa(item.getExternalUa())
                .skuId(item.getSku() != null ? item.getSku().getId() : null)
                .skuCode(skuCode)
                .productName(skuName)
                .category(category)
                .clientId(item.getClient() != null ? item.getClient().getId() : null)
                .clientName(clientName)
                .lotNumber(item.getBatchNumber())
                .inboundRemisionNo(item.getSapFolio())
                .manufacturingDate(item.getManufacturingDate())
                .expirationDate(item.getExpirationDate())
                .daysRemaining(daysRemaining)
                .pabloStatus(pabloStatus)
                .pabloLabel(pabloLabel)
                .isSuggestedFefo(pabloStatus.equals("PABLO_ALERT") || (daysRemaining > 0 && daysRemaining <= 60))
                .pieces(pieces)
                .palletTypeId("MADERA_ESTANDAR")
                .palletTypeLabel("Madera Estándar")
                .locationCode(locCode)
                .state(item.getState() != null ? item.getState().name() : "AVAILABLE")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementAuditResponse> getAuditLogs(UUID id) {
        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("OUTBOUND", id);
        return logs.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt()); // Reverse chronological
                })
                .map(this::mapToAuditResponse)
                .collect(Collectors.toList());
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────────

    private UserEntity validateUserCredentials(String username, String password) {
        final String searchIdentifier = (username != null && !username.isBlank())
                ? username.trim()
                : (securityAuditHelper.getCurrentUsername() != null ? securityAuditHelper.getCurrentUsername().trim() : "");

        if (searchIdentifier.isBlank()) {
            throw new ValidationException("El nombre de usuario para autorización de despacho es obligatorio.");
        }

        UserEntity user = userRepositoryPort.findByUsernameOrEmail(searchIdentifier)
                .or(() -> userRepositoryPort.findByUsername(searchIdentifier))
                .or(() -> userRepositoryPort.findByEmail(searchIdentifier))
                .orElseThrow(() -> new ValidationException("Credenciales inválidas: usuario '" + searchIdentifier + "' no encontrado."));

        if (Boolean.FALSE.equals(user.getIsEnabled())) {
            throw new ValidationException("El usuario '" + user.getUsername() + "' está inactivo o deshabilitado.");
        }

        if (password == null || password.isBlank()) {
            throw new ValidationException("La contraseña de autorización para el usuario '" + user.getUsername() + "' es requerida.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ValidationException("Contraseña incorrecta para el usuario '" + (user.getEmail() != null ? user.getEmail() : user.getUsername()) + "'.");
        }

        return user;
    }

    private void logAudit(UUID entityId, String action, Map<String, ?> before, Map<String, ?> after) {
        try {
            String username = securityAuditHelper.getCurrentUsername();
            UserEntity activeUser = null;
            if (username != null && !username.isBlank()) {
                activeUser = userRepositoryPort.findByUsername(username)
                        .or(() -> userRepositoryPort.findByEmail(username))
                        .orElse(null);
            }
            if (activeUser == null) {
                activeUser = userRepositoryPort.findAll().stream()
                        .filter(u -> Boolean.TRUE.equals(u.getIsEnabled()))
                        .findFirst()
                        .orElse(null);
            }
            if (activeUser != null) {
                Map<String, Object> beforeMap = before != null ? new HashMap<>(before) : Collections.emptyMap();
                Map<String, Object> afterMap = after != null ? new HashMap<>(after) : Collections.emptyMap();
                auditService.log(activeUser, action, "OUTBOUND", entityId, beforeMap, afterMap);
            }
        } catch (Exception e) {
            log.warn("Could not record relational audit log for outbound {}: {}", entityId, e.getMessage());
        }
    }

    private MovementAuditResponse mapToAuditResponse(AuditLogEntity log) {
        List<MovementAuditResponse.MovementAuditDetailResponse> details = log.getDetails() != null ?
                log.getDetails().stream().map(d -> MovementAuditResponse.MovementAuditDetailResponse.builder()
                        .fieldName(translateFieldName(d.getFieldName()))
                        .oldValue(translateFieldValue(d.getFieldName(), d.getOldValue()))
                        .newValue(translateFieldValue(d.getFieldName(), d.getNewValue()))
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

        String formattedTimestamp = log.getCreatedAt() != null ? log.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : "";

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
        if (field == null || field.isBlank()) return "Dato";
        return switch (field.trim()) {
            case "client", "clientId", "clientName" -> "Cliente / Destinatario";
            case "carrier", "carrierId", "carrierName" -> "Línea Transportista";
            case "forkliftOperator", "forklift_operator", "operator" -> "Operador de Montacargas";
            case "driver", "driverName" -> "Operador del Transporte";
            case "plates", "tractorPlates", "boxPlates" -> "Placas (Tractor / Caja)";
            case "status" -> "Estado Operativo";
            case "reason", "cancellationReason" -> "Motivo / Justificación";
            case "authorizedBy", "authorized_by" -> "Autorizado Por (Supervisor)";
            case "cancelledBy", "cancelled_by" -> "Cancelado Por";
            case "totalPallets", "pallets" -> "Tarimas Totales Despachadas";
            case "totalPieces", "pieces" -> "Piezas Totales Despachadas";
            case "folio" -> "Folio de Operación";
            case "sealNumber" -> "Número de Sello / Marchamo";
            default -> field;
        };
    }

    private String translateFieldValue(String field, String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) return "Sin especificar";
        String val = value.trim();
        return switch (val) {
            case "REGISTERED" -> "1. Caseta / Registrado";
            case "ASSIGNED" -> "2. Asignado a Andén";
            case "IN_PROGRESS" -> "3. En Carga";
            case "LOADED" -> "4. Carga Finalizada";
            case "COMPLETED", "DISPATCHED" -> "5. Despachado / Salida Confirmada";
            case "CANCELLED" -> "Cancelado";
            case "PENDING" -> "Pendiente de Carga";
            default -> val;
        };
    }
}
