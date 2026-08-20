package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.reception.*;
import com.fourguard.wms.application.dto.response.reception.*;
import com.fourguard.wms.application.mapper.WarehouseReceptionMapper;
import com.fourguard.wms.domain.enums.InventoryState;
import com.fourguard.wms.domain.enums.MovementType;
import com.fourguard.wms.domain.enums.PalletType;
import com.fourguard.wms.domain.enums.ReceptionStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.WarehouseReceptionUseCase;
import com.fourguard.wms.domain.ports.out.*;
import com.fourguard.wms.infrastructure.persistence.entity.*;
import com.fourguard.wms.infrastructure.persistence.repository.WarehouseReceptionJpaRepository;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseReceptionService implements WarehouseReceptionUseCase {

    private final WarehouseReceptionRepositoryPort receptionRepositoryPort;
    private final WarehouseReceptionPalletRepositoryPort palletRepositoryPort;
    private final WarehouseReceptionJpaRepository receptionJpaRepository;
    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final CarrierRepositoryPort carrierRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final LocationRepositoryPort locationRepositoryPort;
    private final ForkliftOperatorRepositoryPort forkliftOperatorRepositoryPort;
    private final ProductSkuRepositoryPort productSkuRepositoryPort;
    private final SupplierRepositoryPort supplierRepositoryPort;
    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final AuditService auditService;
    private final SecurityAuditHelper securityAuditHelper;
    private final PasswordEncoder passwordEncoder;
    private final WarehouseReceptionMapper receptionMapper;

    @Override
    @Transactional
    public ReceptionResponse createCheckIn(CreateCheckInRequest request) {
        log.info("Creating reception check-in for client: {}, docNumber: {}", request.getClientId(), request.getDocNumber());

        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada: " + request.getOrganizationId()));

        BranchEntity branch = branchRepositoryPort.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada: " + request.getBranchId()));

        ClientEntity client = clientRepositoryPort.findById(request.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado: " + request.getClientId()));

        CarrierEntity carrier = null;
        if (request.getCarrierId() != null) {
            carrier = carrierRepositoryPort.findById(request.getCarrierId()).orElse(null);
        }

        LocationEntity ramp = null;
        if (request.getRampId() != null) {
            ramp = locationRepositoryPort.findById(request.getRampId()).orElse(null);
        }

        ForkliftOperatorEntity operator = null;
        if (request.getForkliftOperatorId() != null) {
            operator = forkliftOperatorRepositoryPort.findById(request.getForkliftOperatorId()).orElse(null);
        }

        long folioSeq = receptionRepositoryPort.nextFolioSequenceValue();
        String folio = String.valueOf(folioSeq);

        WarehouseReceptionEntity entity = WarehouseReceptionEntity.builder()
                .organization(organization)
                .branch(branch)
                .folio(folio)
                .status(ReceptionStatus.REGISTERED)
                .carrier(carrier)
                .client(client)
                .ramp(ramp)
                .forkliftOperator(operator)
                .docNumber(request.getDocNumber())
                .docDate(request.getDocDate())
                .receptionTime(request.getReceptionTime())
                .driverName(request.getDriverName())
                .tractorPlates(request.getTractorPlates())
                .boxPlates(request.getBoxPlates())
                .piecesPerPallet(0.0)
                .palletType(PalletType.MADERA_ESTANDAR)
                .build();

        if (request.getSealNumbers() != null && !request.getSealNumbers().isEmpty()) {
            List<WarehouseReceptionSealEntity> seals = new ArrayList<>();
            for (String sealNum : request.getSealNumbers()) {
                if (sealNum != null && !sealNum.isBlank()) {
                    seals.add(WarehouseReceptionSealEntity.builder()
                            .reception(entity)
                            .sealNumber(sealNum.trim())
                            .build());
                }
            }
            entity.setSeals(seals);
        }

        WarehouseReceptionEntity saved = receptionRepositoryPort.save(entity);

        // Relational Audit Log
        logAudit(saved.getId(), "RECEPCION_CREADA",
                Map.of(),
                Map.of("folio", folio,
                       "docNumber", request.getDocNumber(),
                       "client", client.getName(),
                       "driver", request.getDriverName(),
                       "plates", request.getTractorPlates() + " / " + request.getBoxPlates()));

        return receptionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReceptionResponse updateParameters(UUID id, UpdateReceptionParametersRequest request) {
        WarehouseReceptionEntity entity = receptionRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recepción no encontrada: " + id));

        if (entity.getStatus() != ReceptionStatus.REGISTERED) {
            throw new ValidationException("No se pueden editar parámetros de una recepción en estado: " + entity.getStatus());
        }

        Map<String, Object> before = Map.of(
                "lotNumber", entity.getLotNumber() != null ? entity.getLotNumber() : "",
                "piecesPerPallet", entity.getPiecesPerPallet() != null ? entity.getPiecesPerPallet().toString() : "0"
        );

        if (request.getSkuId() != null) {
            ProductSkuEntity sku = productSkuRepositoryPort.findById(request.getSkuId())
                    .orElseThrow(() -> new EntityNotFoundException("SKU no encontrado: " + request.getSkuId()));
            entity.setSku(sku);
        }

        if (request.getSupplierId() != null) {
            SupplierEntity supplier = supplierRepositoryPort.findById(request.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + request.getSupplierId()));
            entity.setSupplier(supplier);
        }

        if (request.getStorageLocationId() != null) {
            LocationEntity storageLoc = locationRepositoryPort.findById(request.getStorageLocationId())
                    .orElseThrow(() -> new EntityNotFoundException("Ubicación de almacenaje no encontrada: " + request.getStorageLocationId()));
            entity.setStorageLocation(storageLoc);
        }

        if (request.getLotNumber() != null) entity.setLotNumber(request.getLotNumber().trim());
        if (request.getElaborationDate() != null) entity.setElaborationDate(request.getElaborationDate());
        if (request.getExpirationDate() != null) entity.setExpirationDate(request.getExpirationDate());
        if (request.getPiecesPerPallet() != null) entity.setPiecesPerPallet(request.getPiecesPerPallet());
        if (request.getPalletType() != null) {
            try {
                entity.setPalletType(PalletType.valueOf(request.getPalletType()));
            } catch (IllegalArgumentException e) {
                entity.setPalletType(PalletType.MADERA_ESTANDAR);
            }
        }
        if (request.getObservations() != null) entity.setObservations(request.getObservations());

        WarehouseReceptionEntity saved = receptionRepositoryPort.save(entity);

        Map<String, Object> after = Map.of(
                "lotNumber", saved.getLotNumber() != null ? saved.getLotNumber() : "",
                "piecesPerPallet", saved.getPiecesPerPallet() != null ? saved.getPiecesPerPallet().toString() : "0"
        );
        logAudit(saved.getId(), "RECEPCION_ACTUALIZADA", before, after);

        return receptionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceptionResponse getReceptionById(UUID id) {
        WarehouseReceptionEntity entity = receptionRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recepción no encontrada: " + id));
        return receptionMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceptionSummaryResponse> getReceptions(UUID organizationId, UUID branchId, String status, String search) {
        ReceptionStatus recStatus = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                recStatus = ReceptionStatus.valueOf(status.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        List<WarehouseReceptionEntity> entities = receptionJpaRepository.findWithFilters(organizationId, branchId, recStatus, cleanSearch);
        return entities.stream().map(receptionMapper::toSummaryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ReceptionPalletResponse> addPallets(UUID receptionId, AddReceptionPalletsRequest request) {
        WarehouseReceptionEntity reception = receptionRepositoryPort.findById(receptionId)
                .orElseThrow(() -> new EntityNotFoundException("Recepción no encontrada: " + receptionId));

        if (reception.getStatus() != ReceptionStatus.REGISTERED) {
            throw new ValidationException("Solo se pueden agregar tarimas a recepciones en estado REGISTERED.");
        }

        if (reception.getSku() == null) {
            throw new ValidationException("Debes asignar un SKU/Producto a la recepción antes de capturar tarimas.");
        }

        int currentCount = palletRepositoryPort.countByReceptionId(receptionId);
        List<WarehouseReceptionPalletEntity> newPallets = new ArrayList<>();

        for (AddReceptionPalletsRequest.PalletItemRequest item : request.getPallets()) {
            String code = item.getPalletCode().trim();
            if (palletRepositoryPort.existsByReceptionIdAndPalletCode(receptionId, code)) {
                throw new ValidationException("El código de tarima/UA '" + code + "' ya existe en esta recepción.");
            }

            currentCount++;
            PalletType pType = reception.getPalletType();
            if (item.getPalletType() != null && !item.getPalletType().isBlank()) {
                try {
                    pType = PalletType.valueOf(item.getPalletType());
                } catch (IllegalArgumentException ignored) {}
            }

            WarehouseReceptionPalletEntity palletEntity = WarehouseReceptionPalletEntity.builder()
                    .reception(reception)
                    .palletNumber(currentCount)
                    .palletCode(code)
                    .sku(reception.getSku())
                    .supplier(reception.getSupplier())
                    .pieces(item.getPieces())
                    .palletType(pType)
                    .observations(item.getObservations())
                    .build();

            newPallets.add(palletRepositoryPort.save(palletEntity));
        }

        return receptionMapper.toPalletResponseList(palletRepositoryPort.findByReceptionId(receptionId));
    }

    @Override
    @Transactional
    public ReceptionPalletResponse updatePallet(UUID receptionId, UUID palletId, UpdatePalletRequest request) {
        WarehouseReceptionPalletEntity pallet = palletRepositoryPort.findByReceptionIdAndId(receptionId, palletId)
                .orElseThrow(() -> new EntityNotFoundException("Tarima no encontrada con ID: " + palletId));

        Double oldPieces = pallet.getPieces();
        if (request.getPieces() != null) pallet.setPieces(request.getPieces());
        if (request.getPalletType() != null) {
            try {
                pallet.setPalletType(PalletType.valueOf(request.getPalletType()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (request.getObservations() != null) pallet.setObservations(request.getObservations());

        WarehouseReceptionPalletEntity saved = palletRepositoryPort.save(pallet);

        logAudit(receptionId, "TARIMA_EDITADA",
                Map.of("palletCode", saved.getPalletCode(), "pieces", String.valueOf(oldPieces)),
                Map.of("palletCode", saved.getPalletCode(), "pieces", String.valueOf(saved.getPieces())));

        return receptionMapper.toPalletResponse(saved);
    }

    @Override
    @Transactional
    public void deletePallet(UUID receptionId, UUID palletId) {
        WarehouseReceptionPalletEntity pallet = palletRepositoryPort.findByReceptionIdAndId(receptionId, palletId)
                .orElseThrow(() -> new EntityNotFoundException("Tarima no encontrada: " + palletId));
        palletRepositoryPort.deleteById(pallet.getId());
    }

    @Override
    @Transactional
    public ReceptionResponse completeReception(UUID id, CompleteReceptionRequest request) {
        WarehouseReceptionEntity reception = receptionRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recepción no encontrada: " + id));

        if (reception.getStatus() != ReceptionStatus.REGISTERED) {
            throw new ValidationException("La recepción ya no está en estado REGISTERED (Estado actual: " + reception.getStatus() + ")");
        }

        // Validate Leader Credentials against wms.users
        UserEntity leader = validateUserCredentials(request.getLeaderUsername(), request.getLeaderPassword(), "Líder de Almacén");

        List<WarehouseReceptionPalletEntity> pallets = palletRepositoryPort.findByReceptionId(id);
        if (pallets.isEmpty()) {
            throw new ValidationException("No se puede completar una recepción sin tarimas escaneadas.");
        }

        if (reception.getSku() == null) {
            throw new ValidationException("La recepción debe tener un SKU asignado para el ingreso al inventario.");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        reception.setStatus(ReceptionStatus.COMPLETED);
        reception.setCompletedAt(now);
        reception.setLeaderAuthorizedBy(leader.getFirstName() + " " + leader.getLastName());
        if (request.getObservations() != null && !request.getObservations().isBlank()) {
            reception.setObservations((reception.getObservations() != null ? reception.getObservations() + " | " : "") + request.getObservations());
        }

        // Generate Inventory Items and Inventory Movements for each UA
        for (WarehouseReceptionPalletEntity pallet : pallets) {
            InventoryItemEntity inventoryItem = InventoryItemEntity.builder()
                    .organization(reception.getOrganization())
                    .branch(reception.getBranch())
                    .client(reception.getClient())
                    .sscc(pallet.getPalletCode())
                    .externalUa(pallet.getPalletCode())
                    .sku(reception.getSku())
                    .location(reception.getStorageLocation())
                    .state(InventoryState.AVAILABLE)
                    .quantity(BigDecimal.valueOf(pallet.getPieces()))
                    .batchNumber(reception.getLotNumber())
                    .manufacturingDate(reception.getElaborationDate())
                    .expirationDate(reception.getExpirationDate())
                    .sapFolio(reception.getDocNumber())
                    .build();

            InventoryItemEntity savedItem = inventoryItemRepositoryPort.save(inventoryItem);
            pallet.setInventoryItem(savedItem);
            palletRepositoryPort.save(pallet);

            // Log Inventory Movement
            InventoryMovementEntity movement = InventoryMovementEntity.builder()
                    .item(savedItem)
                    .toLocation(reception.getStorageLocation())
                    .user(leader)
                    .type(MovementType.ENTRY)
                    .reason("Recepción F01 Folio: " + reception.getFolio() + " - Remisión: " + reception.getDocNumber())
                    .createdAt(now)
                    .build();
            inventoryMovementRepositoryPort.save(movement);
        }

        WarehouseReceptionEntity saved = receptionRepositoryPort.save(reception);

        double totalPieces = pallets.stream().mapToDouble(WarehouseReceptionPalletEntity::getPieces).sum();
        logAudit(saved.getId(), "RECEPCION_COMPLETADA",
                Map.of("status", "REGISTERED"),
                Map.of("status", "COMPLETED",
                       "leader", reception.getLeaderAuthorizedBy(),
                       "totalPallets", String.valueOf(pallets.size()),
                       "totalPieces", String.valueOf(totalPieces)));

        return receptionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReceptionResponse cancelReception(UUID id, CancelReceptionRequest request) {
        WarehouseReceptionEntity reception = receptionRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recepción no encontrada: " + id));

        if (reception.getStatus() == ReceptionStatus.CANCELLED) {
            throw new ValidationException("La recepción ya se encuentra cancelada.");
        }

        // Validate Admin Credentials
        UserEntity admin = validateUserCredentials(request.getAdminUsername(), request.getAdminPassword(), "Administrador");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String oldStatus = reception.getStatus().name();
        reception.setStatus(ReceptionStatus.CANCELLED);
        reception.setCancelledAt(now);
        reception.setCancellationReason(request.getReason());
        reception.setCancelledBy(admin.getFirstName() + " " + admin.getLastName());

        WarehouseReceptionEntity saved = receptionRepositoryPort.save(reception);

        logAudit(saved.getId(), "RECEPCION_CANCELADA",
                Map.of("status", oldStatus),
                Map.of("status", "CANCELLED",
                       "cancelledBy", reception.getCancelledBy(),
                       "reason", request.getReason()));

        return receptionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReceptionResponse changeRemision(UUID id, ChangeRemisionRequest request) {
        WarehouseReceptionEntity reception = receptionRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recepción no encontrada: " + id));

        String oldDoc = reception.getDocNumber();
        String newDoc = request.getNewDocNumber();
        if (newDoc == null || newDoc.isBlank()) {
            throw new ValidationException("El nuevo número de remisión es obligatorio.");
        }

        reception.setDocNumber(newDoc.trim());
        reception.setObservations((reception.getObservations() != null ? reception.getObservations() : "") +
                " | Cambio Remisión: " + oldDoc + " -> " + newDoc + " (" + request.getReason() + ")");

        WarehouseReceptionEntity saved = receptionRepositoryPort.save(reception);

        logAudit(saved.getId(), "REMISION_MODIFICADA",
                Map.of("docNumber", oldDoc),
                Map.of("docNumber", newDoc, "reason", request.getReason()));

        return receptionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementAuditResponse> getAuditLogs(UUID id) {
        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("RECEPTION", id);
        return logs.stream().map(this::mapToAuditResponse).collect(Collectors.toList());
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────────

    private UserEntity validateUserCredentials(String username, String password, String expectedRoleName) {
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
                auditService.log(activeUser, action, "RECEPTION", entityId, before, after);
            }
        } catch (Exception e) {
            log.warn("Could not record relational audit log for reception {}: {}", entityId, e.getMessage());
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
            case "RECEPCION_CREADA" -> "Pre-Recepción Registrada en Caseta";
            case "RECEPCION_ACTUALIZADA" -> "Actualización de Datos de Recepción";
            case "TARIMA_EDITADA" -> "Corrección de Tarima Individual";
            case "RECEPCION_COMPLETADA" -> "Descarga y Cierre de Recepción F01";
            case "RECEPCION_CANCELADA" -> "Cancelación Extraordinaria con Autorización";
            case "REMISION_MODIFICADA" -> "Cambio de Número de Remisión";
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
