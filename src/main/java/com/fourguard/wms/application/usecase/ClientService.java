package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.common.ClientContactDto;
import com.fourguard.wms.application.dto.common.PhysicalDestinationDto;
import com.fourguard.wms.application.dto.request.CreateClientRequest;
import com.fourguard.wms.application.dto.request.UpdateClientRequest;
import com.fourguard.wms.application.dto.response.ClientResponse;
import com.fourguard.wms.application.dto.response.audit.ClientAuditResponse;
import com.fourguard.wms.application.mapper.ClientMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.ClientUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.ClientDestinationRepositoryPort;
import com.fourguard.wms.domain.ports.out.ClientRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientContactEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientDestinationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de Aplicación — Gestión de Clientes Depositantes / Owners 3PL.
 *
 * Implementa la lógica de negocio completa:
 * - CRUD del cliente con sus colecciones anidadas (contactos y destinos) en cascada.
 * - Validación de unicidad de RFC / External ID y código de destino.
 * - Toggle de estado (ACTIVE ↔ INACTIVE) con baja lógica.
 * - Gestión granular de destinos físicos (Ship-to Locations).
 * - Auditoría transaccional detallada con deltas before/after.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService implements ClientUseCase {

    private static final Set<String> GENERIC_RFCS = Set.of("XAXX010101000", "XEXX010101000");

    private final ClientRepositoryPort            clientRepositoryPort;
    private final ClientDestinationRepositoryPort destinationRepositoryPort;
    private final OrganizationRepositoryPort      organizationRepositoryPort;
    private final ClientMapper                    clientMapper;
    private final SecurityAuditHelper             securityAuditHelper;
    private final AuditService                    auditService;
    private final AuditLogRepositoryPort          auditLogRepositoryPort;
    private final UserRepositoryPort              userRepositoryPort;

    // ── CRUD Principal ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        log.info("Creating client: [{}] under org: [{}]", request.getName(), request.getOrganizationId());

        OrganizationEntity organization = findOrg(request.getOrganizationId());
        validateExternalIdUniqueness(request.getOrganizationId(), request.getExternalId(), null);
        validateTaxIdUniqueness(request.getOrganizationId(), request.getTaxId(), null);

        ClientEntity entity = clientMapper.toEntity(request);
        entity.setOrganization(organization);
        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);

        // Sincronizar contactos en cascada
        if (request.getContacts() != null) {
            request.getContacts().forEach(dto -> {
                ClientContactEntity contact = clientMapper.toContactEntity(dto);
                contact.setId(null);
                entity.addContact(contact);
            });
        }

        // Sincronizar destinos en cascada y validar códigos únicos
        if (request.getDestinations() != null) {
            request.getDestinations().forEach(dto -> {
                validateDestinationCodeForNew(entity, dto.getDestinationCode());
                ClientDestinationEntity dest = clientMapper.toDestinationEntity(dto);
                dest.setId(null);
                if (dest.getStatus() == null || dest.getStatus().isBlank()) {
                    dest.setStatus("ACTIVO");
                }
                entity.addDestination(dest);
            });
        }

        ClientEntity saved = clientRepositoryPort.save(entity);
        logAuditChange(currentUser, "CLIENT_CREATED", saved.getId(), null, buildAuditState(saved));

        return clientMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ClientResponse updateClient(UpdateClientRequest request) {
        log.info("Updating client with ID: [{}]", request.getId());

        ClientEntity existing = findClient(request.getId());
        OrganizationEntity organization = findOrg(request.getOrganizationId());
        validateExternalIdUniqueness(request.getOrganizationId(), request.getExternalId(), request.getId());
        validateTaxIdUniqueness(request.getOrganizationId(), request.getTaxId(), request.getId());

        Map<String, Object> beforeState = buildAuditState(existing);
        clientMapper.updateEntityFromDto(request, existing);
        existing.setOrganization(organization);
        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        // ── Sincronización inteligente de Contactos (orphanRemoval eliminará los huérfanos) ──
        syncContacts(existing, request.getContacts());

        // ── Sincronización inteligente de Destinos ────────────────────────────
        syncDestinations(existing, request.getDestinations());

        ClientEntity saved = clientRepositoryPort.save(existing);
        logAuditChange(currentUser, "CLIENT_UPDATED", saved.getId(), beforeState, buildAuditState(saved));

        return clientMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientById(UUID id) {
        log.debug("Fetching client with ID: [{}]", id);
        return clientMapper.toResponse(findClient(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> getClientsByOrganizationId(UUID organizationId) {
        log.debug("Fetching clients by org: [{}]", organizationId);
        return clientRepositoryPort.findByOrganizationId(organizationId).stream()
                .map(clientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {
        log.debug("Fetching all clients");
        return clientRepositoryPort.findAll().stream()
                .map(clientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteClient(UUID id) {
        log.info("Deleting client with ID: [{}]", id);
        ClientEntity existing = findClient(id);
        Map<String, Object> beforeState = buildAuditState(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();
        clientRepositoryPort.deleteById(id);
        logAuditChange(currentUser, "CLIENT_DELETED", id, beforeState, null);
    }

    @Override
    @Transactional
    public ClientResponse toggleClientStatus(UUID id) {
        log.info("Toggling status for client ID: [{}]", id);
        ClientEntity existing = findClient(id);
        Map<String, Object> beforeState = buildAuditState(existing);
        String newStatus = "ACTIVE".equals(existing.getStatus()) ? "INACTIVE" : "ACTIVE";
        existing.setStatus(newStatus);
        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);
        ClientEntity saved = clientRepositoryPort.save(existing);
        logAuditChange(currentUser, "CLIENT_STATUS_CHANGED", id, beforeState, buildAuditState(saved));
        return clientMapper.toResponse(saved);
    }

    // ── Auditoría ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ClientAuditResponse> getClientAuditLogs(UUID id) {
        log.debug("Fetching audit logs for client: [{}]", id);
        if (clientRepositoryPort.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Cliente no encontrado con ID: " + id);
        }
        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("CLIENT", id);
        return logs.stream().map(logEntry -> {
            String username = resolveUsername(logEntry.getUserId());
            List<ClientAuditResponse.AuditDetailResponse> details = logEntry.getDetails().stream()
                    .map(d -> ClientAuditResponse.AuditDetailResponse.builder()
                            .fieldName(d.getFieldName())
                            .oldValue(d.getOldValue())
                            .newValue(d.getNewValue())
                            .build())
                    .collect(Collectors.toList());
            return ClientAuditResponse.builder()
                    .logId(logEntry.getLogId())
                    .action(logEntry.getAction())
                    .username(username)
                    .createdAt(logEntry.getCreatedAt())
                    .details(details)
                    .build();
        }).collect(Collectors.toList());
    }

    // ── Gestión Granular de Destinos Físicos ──────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PhysicalDestinationDto> getClientDestinations(UUID clientId) {
        findClient(clientId); // Valida que el cliente existe
        return clientMapper.toDestinationDtoList(destinationRepositoryPort.findByClientId(clientId));
    }

    @Override
    @Transactional
    public PhysicalDestinationDto addDestination(UUID clientId, PhysicalDestinationDto dto) {
        ClientEntity client = findClient(clientId);

        if (destinationRepositoryPort.existsByClientIdAndDestinationCode(clientId, dto.getDestinationCode())) {
            throw new ValidationException("El código de destino '" + dto.getDestinationCode()
                    + "' ya existe para este cliente.");
        }

        ClientDestinationEntity dest = clientMapper.toDestinationEntity(dto);
        dest.setClient(client);
        if (dest.getStatus() == null || dest.getStatus().isBlank()) {
            dest.setStatus("ACTIVO");
        }
        ClientDestinationEntity saved = destinationRepositoryPort.save(dest);

        String currentUser = securityAuditHelper.getCurrentUsername();
        logAuditChange(currentUser, "CLIENT_DESTINATION_ADDED", clientId,
                null, Map.of("destinationCode", saved.getDestinationCode(), "plantName", saved.getPlantName()));

        return clientMapper.toDestinationDto(saved);
    }

    @Override
    @Transactional
    public PhysicalDestinationDto updateDestination(UUID clientId, UUID destinationId, PhysicalDestinationDto dto) {
        findClient(clientId);
        ClientDestinationEntity existing = destinationRepositoryPort.findById(destinationId)
                .orElseThrow(() -> new EntityNotFoundException("Destino no encontrado con ID: " + destinationId));

        if (!existing.getClient().getId().equals(clientId)) {
            throw new ValidationException("El destino no pertenece al cliente especificado.");
        }

        if (!existing.getDestinationCode().equals(dto.getDestinationCode())
                && destinationRepositoryPort.existsByClientIdAndDestinationCodeAndIdNot(
                        clientId, dto.getDestinationCode(), destinationId)) {
            throw new ValidationException("El código de destino '" + dto.getDestinationCode()
                    + "' ya existe para este cliente.");
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        Map<String, Object> before = Map.of("destinationCode", existing.getDestinationCode(),
                "plantName", existing.getPlantName(), "status", existing.getStatus());

        existing.setDestinationCode(dto.getDestinationCode());
        existing.setPlantName(dto.getPlantName());
        existing.setFullAddress(dto.getFullAddress());
        existing.setContactPerson(dto.getContactPerson());
        existing.setPhone(dto.getPhone());
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            existing.setStatus(dto.getStatus());
        }
        existing.setNotes(dto.getNotes());
        existing.setUpdatedBy(currentUser);

        ClientDestinationEntity saved = destinationRepositoryPort.save(existing);
        logAuditChange(currentUser, "CLIENT_DESTINATION_UPDATED", clientId, before,
                Map.of("destinationCode", saved.getDestinationCode(), "plantName", saved.getPlantName()));

        return clientMapper.toDestinationDto(saved);
    }

    @Override
    @Transactional
    public void deleteDestination(UUID clientId, UUID destinationId) {
        findClient(clientId);
        ClientDestinationEntity existing = destinationRepositoryPort.findById(destinationId)
                .orElseThrow(() -> new EntityNotFoundException("Destino no encontrado con ID: " + destinationId));

        if (!existing.getClient().getId().equals(clientId)) {
            throw new ValidationException("El destino no pertenece al cliente especificado.");
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        logAuditChange(currentUser, "CLIENT_DESTINATION_DELETED", clientId,
                Map.of("destinationCode", existing.getDestinationCode(), "plantName", existing.getPlantName()), null);

        destinationRepositoryPort.deleteById(destinationId);
    }

    // ── Helpers Privados ──────────────────────────────────────────────────────

    private ClientEntity findClient(UUID id) {
        return clientRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));
    }

    private OrganizationEntity findOrg(UUID orgId) {
        return organizationRepositoryPort.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + orgId));
    }

    private void validateExternalIdUniqueness(UUID orgId, String externalId, UUID currentId) {
        if (externalId == null || externalId.isBlank()) return;
        String norm = externalId.trim().toUpperCase();
        boolean exists = currentId == null
                ? clientRepositoryPort.existsByOrganizationIdAndExternalId(orgId, norm)
                : clientRepositoryPort.existsByOrganizationIdAndExternalIdAndIdNot(orgId, norm, currentId);
        if (exists) {
            throw new ValidationException("El código de cliente / RFC externo '" + norm
                    + "' ya existe para otro cliente en esta organización.");
        }
    }

    private void validateTaxIdUniqueness(UUID orgId, String taxId, UUID currentId) {
        if (taxId == null || taxId.isBlank()) return;
        String norm = taxId.trim().toUpperCase();
        if (GENERIC_RFCS.contains(norm)) return;
        boolean exists = currentId == null
                ? clientRepositoryPort.existsByOrganizationIdAndTaxId(orgId, norm)
                : clientRepositoryPort.existsByOrganizationIdAndTaxIdAndIdNot(orgId, norm, currentId);
        if (exists) {
            throw new ValidationException("El RFC / Tax ID '" + norm
                    + "' ya se encuentra registrado para otro cliente en esta organización.");
        }
    }

    private void validateDestinationCodeForNew(ClientEntity entity, String code) {
        if (code == null || code.isBlank()) return;
        boolean duplicate = entity.getDestinations().stream()
                .anyMatch(d -> code.equalsIgnoreCase(d.getDestinationCode()));
        if (duplicate) {
            throw new ValidationException("El código de destino '" + code
                    + "' está duplicado dentro de la misma solicitud.");
        }
    }

    /**
     * Sincronización inteligente de la colección de Contactos.
     * - Conserva entidades existentes (actualizando sus campos).
     * - Agrega nuevos contactos (sin ID).
     * - orphanRemoval = true eliminará del lado JPA los que no estén en la nueva lista.
     */
    private void syncContacts(ClientEntity entity, List<ClientContactDto> incoming) {
        if (incoming == null) return;

        // IDs recibidos del FE
        Set<UUID> incomingIds = incoming.stream()
                .filter(dto -> dto.getId() != null)
                .map(ClientContactDto::getId)
                .collect(Collectors.toSet());

        // Eliminar los que ya no vienen (orphanRemoval los borrará en BD)
        entity.getContacts().removeIf(existing ->
                existing.getId() != null && !incomingIds.contains(existing.getId()));

        // Construir mapa de existentes por ID
        Map<UUID, ClientContactEntity> existingById = entity.getContacts().stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(ClientContactEntity::getId, c -> c));

        incoming.forEach(dto -> {
            if (dto.getId() != null && existingById.containsKey(dto.getId())) {
                // Actualizar campos del contacto existente
                ClientContactEntity existing = existingById.get(dto.getId());
                existing.setName(dto.getName());
                existing.setDepartment(dto.getDepartment());
                existing.setPhone(dto.getPhone());
                existing.setEmail(dto.getEmail());
                existing.setIsPrimary(Boolean.TRUE.equals(dto.getIsPrimary()));
            } else {
                // Agregar nuevo contacto (asegurar id nulo para auto-generación de UUID)
                ClientContactEntity newContact = clientMapper.toContactEntity(dto);
                newContact.setId(null);
                entity.addContact(newContact);
            }
        });
    }

    /**
     * Sincronización inteligente de la colección de Destinos Físicos.
     * Misma estrategia que syncContacts.
     */
    private void syncDestinations(ClientEntity entity, List<PhysicalDestinationDto> incoming) {
        if (incoming == null) return;

        Set<UUID> incomingIds = incoming.stream()
                .filter(dto -> dto.getId() != null)
                .map(PhysicalDestinationDto::getId)
                .collect(Collectors.toSet());

        entity.getDestinations().removeIf(existing ->
                existing.getId() != null && !incomingIds.contains(existing.getId()));

        Map<UUID, ClientDestinationEntity> existingById = entity.getDestinations().stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(ClientDestinationEntity::getId, d -> d));

        incoming.forEach(dto -> {
            if (dto.getId() != null && existingById.containsKey(dto.getId())) {
                ClientDestinationEntity existing = existingById.get(dto.getId());
                existing.setDestinationCode(dto.getDestinationCode());
                existing.setPlantName(dto.getPlantName());
                existing.setFullAddress(dto.getFullAddress());
                existing.setContactPerson(dto.getContactPerson());
                existing.setPhone(dto.getPhone());
                if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
                    existing.setStatus(dto.getStatus());
                }
                existing.setNotes(dto.getNotes());
            } else {
                ClientDestinationEntity newDest = clientMapper.toDestinationEntity(dto);
                newDest.setId(null);
                if (newDest.getStatus() == null || newDest.getStatus().isBlank()) {
                    newDest.setStatus("ACTIVO");
                }
                entity.addDestination(newDest);
            }
        });
    }

    private Map<String, Object> buildAuditState(ClientEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("name", entity.getName());
        state.put("externalId", entity.getExternalId());
        state.put("taxId", entity.getTaxId());
        state.put("address", entity.getAddress());
        state.put("phone", entity.getPhone());
        state.put("status", entity.getStatus());
        state.put("contactsCount", entity.getContacts() != null ? entity.getContacts().size() : 0);
        state.put("destinationsCount", entity.getDestinations() != null ? entity.getDestinations().size() : 0);
        if (entity.getOrganization() != null) {
            state.put("organizationId", entity.getOrganization().getId());
            state.put("organizationName", entity.getOrganization().getName());
        }
        return state;
    }

    private void logAuditChange(String username, String action, UUID entityId,
                                Map<String, Object> before, Map<String, Object> after) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                auditService.log(actor, action, "CLIENT", entityId, before, after);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for client operation [{}]", action, e);
        }
    }

    private String resolveUsername(UUID userId) {
        if (userId == null) return "SYSTEM";
        return userRepositoryPort.findById(userId)
                .map(UserEntity::getUsername)
                .orElse("UNKNOWN");
    }
}
