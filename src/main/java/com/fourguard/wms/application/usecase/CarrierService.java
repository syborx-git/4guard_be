package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateCarrierRequest;
import com.fourguard.wms.application.dto.request.UpdateCarrierRequest;
import com.fourguard.wms.application.dto.response.CarrierResponse;
import com.fourguard.wms.application.dto.response.audit.CarrierAuditResponse;
import com.fourguard.wms.application.mapper.CarrierMapper;
import com.fourguard.wms.domain.enums.CarrierStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.CarrierUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.CarrierRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.CarrierEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.repository.ClientJpaRepository;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarrierService implements CarrierUseCase {

    private final CarrierRepositoryPort carrierRepositoryPort;
    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ClientJpaRepository clientJpaRepository;
    private final CarrierMapper carrierMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;
    private final AuditLogRepositoryPort auditLogRepositoryPort;

    @Override
    @Transactional
    public CarrierResponse createCarrier(CreateCarrierRequest request) {
        log.info("Creating carrier: {} under organization: {}", request.getName(), request.getOrganizationId());

        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + request.getOrganizationId()));

        // Validar unicidad de nombre en la misma organización
        List<CarrierEntity> existingCarriers = carrierRepositoryPort.findByOrganizationId(request.getOrganizationId());
        boolean nameExists = existingCarriers.stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(request.getName()));
        if (nameExists) {
            throw new ValidationException("Ya existe un transportista registrado con el nombre '" + request.getName() + "' en esta organización.");
        }

        // Validar unicidad de RFC (tax_id)
        if (request.getTaxId() != null && !request.getTaxId().isBlank()) {
            validateTaxId(request.getTaxId(), request.getOrganizationId(), null);
        }

        CarrierEntity entity = carrierMapper.toEntity(request);
        entity.setOrganization(organization);
        
        // Asignar lista de capacidades de vehículos
        if (request.getVehicleTypes() != null) {
            entity.setVehicleTypes(new ArrayList<>(request.getVehicleTypes()));
        }

        // Asignar lista de clientes preferentes
        if (request.getPreferredClientIds() != null && !request.getPreferredClientIds().isEmpty()) {
            List<ClientEntity> preferredClients = clientJpaRepository.findAllById(request.getPreferredClientIds());
            entity.setPreferredClients(preferredClients);
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        CarrierEntity saved = carrierRepositoryPort.save(entity);

        // Registro de auditoría obligatoria
        logAuditChange(currentUser, "CARRIER_CREATED", saved.getId(), null, saved);

        return carrierMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CarrierResponse updateCarrier(UpdateCarrierRequest request) {
        log.info("Updating carrier with ID: {}", request.getId());

        CarrierEntity existing = carrierRepositoryPort.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("Transportista no encontrado con ID: " + request.getId()));

        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + request.getOrganizationId()));

        // Validar unicidad del nombre si es que cambia
        if (!existing.getName().equalsIgnoreCase(request.getName())) {
            List<CarrierEntity> existingCarriers = carrierRepositoryPort.findByOrganizationId(request.getOrganizationId());
            boolean nameExists = existingCarriers.stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(request.getName()));
            if (nameExists) {
                throw new ValidationException("Ya existe otro transportista registrado con el nombre '" + request.getName() + "' en esta organización.");
            }
        }

        // Validar unicidad del RFC si es que cambia o se envía
        if (request.getTaxId() != null && !request.getTaxId().isBlank()) {
            validateTaxId(request.getTaxId(), request.getOrganizationId(), request.getId());
        }

        // Tomar snapshot para la auditoría antes de modificar
        CarrierEntity originalSnapshot = cloneCarrierEntity(existing);

        carrierMapper.updateEntityFromDto(request, existing);
        existing.setOrganization(organization);
        
        // Actualizar capacidades de vehículos
        if (request.getVehicleTypes() != null) {
            existing.getVehicleTypes().clear();
            existing.getVehicleTypes().addAll(request.getVehicleTypes());
        }

        // Actualizar clientes preferentes
        if (request.getPreferredClientIds() != null) {
            existing.getPreferredClients().clear();
            if (!request.getPreferredClientIds().isEmpty()) {
                List<ClientEntity> preferredClients = clientJpaRepository.findAllById(request.getPreferredClientIds());
                existing.getPreferredClients().addAll(preferredClients);
            }
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        CarrierEntity saved = carrierRepositoryPort.save(existing);

        // Registro de auditoría obligatoria
        logAuditChange(currentUser, "CARRIER_UPDATED", saved.getId(), originalSnapshot, saved);

        return carrierMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CarrierResponse getCarrierById(UUID id) {
        log.debug("Fetching carrier with ID: {}", id);
        return carrierRepositoryPort.findById(id)
                .map(carrierMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Transportista no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarrierResponse> getCarriersByOrganizationId(UUID organizationId) {
        log.debug("Fetching carriers by organization ID: {}", organizationId);
        return carrierRepositoryPort.findByOrganizationId(organizationId).stream()
                .map(carrierMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarrierResponse> getAllCarriers() {
        log.debug("Fetching all carriers");
        return carrierRepositoryPort.findAll().stream()
                .map(carrierMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCarrier(UUID id) {
        log.info("Deleting carrier with ID: {}", id);
        CarrierEntity existing = carrierRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transportista no encontrado con ID: " + id));

        String currentUser = securityAuditHelper.getCurrentUsername();
        
        // Log delete audit before actually deleting
        logAuditChange(currentUser, "CARRIER_DELETED", id, existing, null);

        carrierRepositoryPort.deleteById(id);
    }

    @Override
    @Transactional
    public CarrierResponse updateCarrierStatus(UUID id, com.fourguard.wms.application.dto.request.UpdateCarrierStatusRequest request) {
        CarrierStatus newStatus;
        try {
            newStatus = CarrierStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new ValidationException("Estado inválido: " + request.getStatus());
        }

        log.info("Updating status for carrier: {} to {}", id, newStatus);
        CarrierEntity existing = carrierRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transportista no encontrado con ID: " + id));

        CarrierEntity originalSnapshot = cloneCarrierEntity(existing);
        existing.setStatus(newStatus);

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        CarrierEntity saved = carrierRepositoryPort.save(existing);

        Map<String, Object> beforeState = buildAuditState(originalSnapshot);
        Map<String, Object> afterState = buildAuditState(saved);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            afterState.put("reason", request.getReason());
        }
        if (request.getObservations() != null && !request.getObservations().isBlank()) {
            afterState.put("observations", request.getObservations());
        }

        try {
            UserEntity actor = userRepositoryPort.findByUsername(currentUser).orElse(null);
            if (actor != null) {
                auditService.log(actor, "CARRIER_STATUS_UPDATED", "CARRIER", saved.getId(), beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist status update audit log", e);
        }

        return carrierMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarrierAuditResponse> getCarrierAuditLogs(UUID id) {
        log.debug("Fetching audit logs for carrier: {}", id);
        if (!carrierRepositoryPort.findById(id).isPresent()) {
            throw new EntityNotFoundException("Transportista no encontrado con ID: " + id);
        }

        List<com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity> logs = 
                auditLogRepositoryPort.findByEntityTypeAndEntityId("CARRIER", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(UserEntity::getUsername)
                                .orElse("UNKNOWN");
                    }
                    List<CarrierAuditResponse.AuditDetailResponse> detailResponses = logEntry.getDetails().stream()
                            .map(d -> CarrierAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return CarrierAuditResponse.builder()
                            .logId(logEntry.getLogId())
                            .action(logEntry.getAction())
                            .username(username)
                            .createdAt(logEntry.getCreatedAt())
                            .details(detailResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private CarrierEntity cloneCarrierEntity(CarrierEntity source) {
        return CarrierEntity.builder()
                .id(source.getId())
                .organization(source.getOrganization())
                .name(source.getName())
                .tradeName(source.getTradeName())
                .taxId(source.getTaxId())
                .carrierType(source.getCarrierType())
                .status(source.getStatus())
                .contactName(source.getContactName())
                .contactPhone(source.getContactPhone())
                .contactEmail(source.getContactEmail())
                .serviceType(source.getServiceType())
                .permitNumber(source.getPermitNumber())
                .geographicCoverage(source.getGeographicCoverage())
                .notes(source.getNotes())
                .vehicleTypes(new ArrayList<>(source.getVehicleTypes()))
                .preferredClients(new ArrayList<>(source.getPreferredClients()))
                .version(source.getVersion())
                .createdAt(source.getCreatedAt())
                .createdBy(source.getCreatedBy())
                .build();
    }

    private void logAuditChange(String username, String action, UUID entityId, CarrierEntity before, CarrierEntity after) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                Map<String, Object> beforeState = buildAuditState(before);
                Map<String, Object> afterState = buildAuditState(after);
                auditService.log(actor, action, "CARRIER", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for carrier operation", e);
        }
    }

    private Map<String, Object> buildAuditState(CarrierEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("id", entity.getId() != null ? entity.getId().toString() : null);
        state.put("name", entity.getName());
        state.put("tradeName", entity.getTradeName());
        state.put("taxId", entity.getTaxId());
        state.put("carrierType", entity.getCarrierType() != null ? entity.getCarrierType().name() : null);
        state.put("status", entity.getStatus() != null ? entity.getStatus().name() : null);
        state.put("contactName", entity.getContactName());
        state.put("contactPhone", entity.getContactPhone());
        state.put("contactEmail", entity.getContactEmail());
        state.put("serviceType", entity.getServiceType() != null ? entity.getServiceType().name() : null);
        state.put("permitNumber", entity.getPermitNumber());
        state.put("geographicCoverage", entity.getGeographicCoverage());
        state.put("notes", entity.getNotes());
        state.put("vehicleTypes", new ArrayList<>(entity.getVehicleTypes()));
        if (entity.getPreferredClients() != null) {
            state.put("preferredClients", entity.getPreferredClients().stream()
                    .map(c -> Map.of("id", c.getId().toString(), "name", c.getName()))
                    .collect(Collectors.toList()));
        }
        state.put("organizationId", entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null);
        return state;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateTaxId(String taxId, UUID organizationId, UUID excludeId) {
        if (taxId == null || taxId.isBlank()) {
            throw new ValidationException("El RFC (tax_id) es obligatorio para realizar la validación.");
        }

        String cleanedTaxId = taxId.trim();
        boolean exists;

        if (organizationId != null && excludeId != null) {
            exists = carrierRepositoryPort.existsByOrganizationIdAndTaxIdAndIdNot(organizationId, cleanedTaxId, excludeId);
        } else if (organizationId != null) {
            exists = carrierRepositoryPort.existsByOrganizationIdAndTaxId(organizationId, cleanedTaxId);
        } else if (excludeId != null) {
            exists = carrierRepositoryPort.existsByTaxIdAndIdNot(cleanedTaxId, excludeId);
        } else {
            exists = carrierRepositoryPort.existsByTaxId(cleanedTaxId);
        }

        if (exists) {
            throw new ValidationException("El RFC '" + cleanedTaxId + "' ya existe para otro transportista.");
        }
    }
}
