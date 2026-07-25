package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateClientRequest;
import com.fourguard.wms.application.dto.request.UpdateClientRequest;
import com.fourguard.wms.application.dto.response.ClientResponse;
import com.fourguard.wms.application.dto.response.audit.ClientAuditResponse;
import com.fourguard.wms.application.mapper.ClientMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.in.ClientUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.ClientRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService implements ClientUseCase {

    private final ClientRepositoryPort clientRepositoryPort;
    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final ClientMapper clientMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        log.info("Creating client: {} under organization: {}", request.getName(), request.getOrganizationId());
        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + request.getOrganizationId()));

        ClientEntity entity = clientMapper.toEntity(request);
        entity.setOrganization(organization);
        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);
        ClientEntity saved = clientRepositoryPort.save(entity);

        // Audit log
        Map<String, Object> afterState = buildAuditState(saved);
        logAuditChange(currentUser, "CLIENT_CREATED", saved.getId(), null, afterState);

        return clientMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ClientResponse updateClient(UpdateClientRequest request) {
        log.info("Updating client with ID: {}", request.getId());
        ClientEntity existing = clientRepositoryPort.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + request.getId()));

        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + request.getOrganizationId()));

        Map<String, Object> beforeState = buildAuditState(existing);

        clientMapper.updateEntityFromDto(request, existing);
        existing.setOrganization(organization);
        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);
        ClientEntity saved = clientRepositoryPort.save(existing);
        Map<String, Object> afterState = buildAuditState(saved);

        // Audit log
        logAuditChange(currentUser, "CLIENT_UPDATED", saved.getId(), beforeState, afterState);

        return clientMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientById(UUID id) {
        log.debug("Fetching client with ID: {}", id);
        return clientRepositoryPort.findById(id)
                .map(clientMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> getClientsByOrganizationId(UUID organizationId) {
        log.debug("Fetching clients by organization ID: {}", organizationId);
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
    @Transactional(readOnly = true)
    public List<ClientAuditResponse> getClientAuditLogs(UUID id) {
        log.debug("Fetching audit logs for client: {}", id);
        if (!clientRepositoryPort.findById(id).isPresent()) {
            throw new EntityNotFoundException("Cliente no encontrado con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("CLIENT", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(UserEntity::getUsername)
                                .orElse("UNKNOWN");
                    }
                    List<ClientAuditResponse.AuditDetailResponse> detailResponses = logEntry.getDetails().stream()
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
                            .details(detailResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteClient(UUID id) {
        log.info("Deleting client with ID: {}", id);
        ClientEntity existing = clientRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));

        Map<String, Object> beforeState = buildAuditState(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();

        clientRepositoryPort.deleteById(id);

        // Audit log
        logAuditChange(currentUser, "CLIENT_DELETED", id, beforeState, null);
    }

    // ── Audit Helpers ─────────────────────────────────────────────────────────

    private Map<String, Object> buildAuditState(ClientEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("name", entity.getName());
        state.put("externalId", entity.getExternalId());
        state.put("status", entity.getStatus());
        if (entity.getOrganization() != null) {
            state.put("organizationId", entity.getOrganization().getId());
            state.put("organizationName", entity.getOrganization().getName());
        }
        return state;
    }

    private void logAuditChange(String username, String action, UUID entityId, Map<String, Object> beforeState, Map<String, Object> afterState) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                auditService.log(actor, action, "CLIENT", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for client operation", e);
        }
    }
}
