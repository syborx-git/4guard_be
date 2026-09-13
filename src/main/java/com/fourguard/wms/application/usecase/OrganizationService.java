package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateOrganizationRequest;
import com.fourguard.wms.application.dto.request.UpdateOrganizationRequest;
import com.fourguard.wms.application.dto.response.OrganizationResponse;
import com.fourguard.wms.application.dto.response.audit.OrganizationAuditResponse;
import com.fourguard.wms.application.mapper.OrganizationMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.CreateOrganizationUseCase;
import com.fourguard.wms.domain.ports.in.DeleteOrganizationUseCase;
import com.fourguard.wms.domain.ports.in.GetOrganizationUseCase;
import com.fourguard.wms.domain.ports.in.UpdateOrganizationUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
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
public class OrganizationService implements CreateOrganizationUseCase, UpdateOrganizationUseCase, GetOrganizationUseCase, DeleteOrganizationUseCase {

    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final OrganizationMapper organizationMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;

    @Override
    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        log.info("Creating organization with code: {}", request.getCode());
        if (organizationRepositoryPort.existsByCode(request.getCode())) {
            throw new ValidationException("El código de organización ya existe: " + request.getCode());
        }

        OrganizationEntity entity = organizationMapper.toEntity(request);
        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);

        OrganizationEntity saved = organizationRepositoryPort.save(entity);

        // Audit log
        Map<String, Object> afterState = buildAuditState(saved);
        logAuditChange(currentUser, "ORGANIZATION_CREATED", saved.getId(), null, afterState);

        return organizationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OrganizationResponse updateOrganization(UpdateOrganizationRequest request) {
        log.info("Updating organization with ID: {}", request.getId());
        OrganizationEntity existing = organizationRepositoryPort.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + request.getId()));

        Map<String, Object> beforeState = buildAuditState(existing);

        organizationMapper.updateEntityFromDto(request, existing);

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        OrganizationEntity saved = organizationRepositoryPort.save(existing);
        Map<String, Object> afterState = buildAuditState(saved);

        // Audit log
        logAuditChange(currentUser, "ORGANIZATION_UPDATED", saved.getId(), beforeState, afterState);

        return organizationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(UUID id) {
        log.debug("Fetching organization with ID: {}", id);
        return organizationRepositoryPort.findById(id)
                .map(organizationMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAllOrganizations() {
        log.debug("Fetching all organizations");
        return organizationRepositoryPort.findAll().stream()
                .map(organizationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationAuditResponse> getOrganizationAuditLogs(UUID id) {
        log.debug("Fetching audit logs for organization: {}", id);
        if (organizationRepositoryPort.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Organización no encontrada con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("ORGANIZATION", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(u -> u.getUsername())
                                .orElse("UNKNOWN");
                    }
                    List<OrganizationAuditResponse.AuditDetailResponse> detailResponses = logEntry.getDetails().stream()
                            .map(d -> OrganizationAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return OrganizationAuditResponse.builder()
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
    public void deleteOrganization(UUID id) {
        log.info("Deleting organization with ID: {}", id);
        OrganizationEntity existing = organizationRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + id));

        Map<String, Object> beforeState = buildAuditState(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();

        organizationRepositoryPort.deleteById(id);

        // Audit log
        logAuditChange(currentUser, "ORGANIZATION_DELETED", id, beforeState, null);
    }

    // ── Audit Helpers ─────────────────────────────────────────────────────────

    private void logAuditChange(String username, String action, UUID entityId, Map<String, Object> beforeState, Map<String, Object> afterState) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                auditService.log(actor, action, "ORGANIZATION", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for organization operation", e);
        }
    }

    private Map<String, Object> buildAuditState(OrganizationEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("id", entity.getId() != null ? entity.getId().toString() : null);
        state.put("name", entity.getName());
        state.put("code", entity.getCode());
        state.put("taxId", entity.getTaxId());
        state.put("type", entity.getType() != null ? entity.getType().name() : null);
        state.put("status", entity.getStatus() != null ? entity.getStatus().name() : null);
        if (entity.getSettings() != null) {
            entity.getSettings().forEach(s -> state.put("setting." + s.getSettingKey(), s.getSettingValue()));
        }
        return state;
    }
}
