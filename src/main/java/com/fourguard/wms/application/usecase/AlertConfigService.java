package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.AlertConfigFilterRequest;
import com.fourguard.wms.application.dto.request.CreateAlertConfigRequest;
import com.fourguard.wms.application.dto.request.UpdateAlertConfigRequest;
import com.fourguard.wms.application.dto.request.UpdateAlertConfigStatusRequest;
import com.fourguard.wms.application.dto.response.AlertConfigResponse;
import com.fourguard.wms.application.dto.response.audit.AlertConfigAuditResponse;
import com.fourguard.wms.application.mapper.AlertConfigMapper;
import com.fourguard.wms.domain.enums.AlertStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.AlertConfigUseCase;
import com.fourguard.wms.domain.ports.out.AlertConfigRepositoryPort;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AlertConfigEntity;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.repository.OrganizationJpaRepository;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertConfigService implements AlertConfigUseCase {

    private final AlertConfigRepositoryPort alertConfigRepositoryPort;
    private final OrganizationJpaRepository organizationJpaRepository;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final AlertConfigMapper alertConfigMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;

    @Override
    @Transactional
    public AlertConfigResponse createAlertConfig(CreateAlertConfigRequest request) {
        log.info("Creating alert configuration rule '{}'", request.getName());

        String currentUser = securityAuditHelper.getCurrentUsername();
        OrganizationEntity org = resolveOrganization(request.getOrganizationId(), currentUser);

        if (alertConfigRepositoryPort.existsByOrganizationIdAndNameAndIsDeletedFalse(org.getId(), request.getName())) {
            throw new ValidationException("Ya existe una regla de alerta con el nombre '" + request.getName() + "' en esta organización.");
        }

        if (request.getValue() == null || request.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El valor umbral debe ser un número positivo mayor a cero.");
        }

        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            throw new ValidationException("Debe especificar al menos un canal de notificación.");
        }

        if (request.getRecipients() == null || request.getRecipients().isEmpty()) {
            throw new ValidationException("Debe especificar al menos un destinatario.");
        }

        AlertConfigEntity entity = alertConfigMapper.toEntity(request);
        entity.setOrganization(org);
        entity.setStatus(request.getStatus() != null ? request.getStatus() : AlertStatus.ACTIVE);
        entity.setIsDeleted(false);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        AlertConfigEntity saved = alertConfigRepositoryPort.save(entity);

        logAuditChange(currentUser, "ALERT_CONFIG_CREATED", saved.getId(), null, saved);

        return alertConfigMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AlertConfigResponse updateAlertConfig(UUID id, UpdateAlertConfigRequest request) {
        log.info("Updating alert configuration rule ID: {}", id);

        AlertConfigEntity existing = alertConfigRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Regla de alerta no encontrada con ID: " + id));

        UUID orgId = existing.getOrganization().getId();

        if (alertConfigRepositoryPort.existsByOrganizationIdAndNameAndIdNotAndIsDeletedFalse(orgId, request.getName(), id)) {
            throw new ValidationException("Ya existe otra regla de alerta con el nombre '" + request.getName() + "' en esta organización.");
        }

        if (request.getValue() == null || request.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El valor umbral debe ser un número positivo mayor a cero.");
        }

        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            throw new ValidationException("Debe especificar al menos un canal de notificación.");
        }

        if (request.getRecipients() == null || request.getRecipients().isEmpty()) {
            throw new ValidationException("Debe especificar al menos un destinatario.");
        }

        AlertConfigEntity snapshot = cloneForAudit(existing);

        alertConfigMapper.updateEntityFromDto(request, existing);
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        AlertConfigEntity saved = alertConfigRepositoryPort.save(existing);

        logAuditChange(currentUser, "ALERT_CONFIG_UPDATED", saved.getId(), snapshot, saved);

        return alertConfigMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AlertConfigResponse updateAlertConfigStatus(UUID id, UpdateAlertConfigStatusRequest request) {
        log.info("Updating status for alert configuration rule ID: {} to {}", id, request.getStatus());

        AlertConfigEntity existing = alertConfigRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Regla de alerta no encontrada con ID: " + id));

        AlertConfigEntity snapshot = cloneForAudit(existing);
        existing.setStatus(request.getStatus());

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        AlertConfigEntity saved = alertConfigRepositoryPort.save(existing);

        logAuditChange(currentUser, "ALERT_CONFIG_STATUS_UPDATED", saved.getId(), snapshot, saved);

        return alertConfigMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertConfigResponse getAlertConfigById(UUID id) {
        log.debug("Fetching alert configuration rule by ID: {}", id);
        return alertConfigRepositoryPort.findById(id)
                .map(alertConfigMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Regla de alerta no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertConfigResponse> getAlertConfigs(AlertConfigFilterRequest filter) {
        log.debug("Fetching alert configurations with filter: {}", filter);

        if (filter != null && filter.getOrganizationId() == null) {
            String currentUser = securityAuditHelper.getCurrentUsername();
            UserEntity user = userRepositoryPort.findByUsername(currentUser).orElse(null);
            if (user != null && user.getOrganization() != null) {
                filter.setOrganizationId(user.getOrganization().getId());
            }
        }

        return alertConfigRepositoryPort.findAll(filter).stream()
                .map(alertConfigMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAlertConfig(UUID id) {
        log.info("Soft deleting alert configuration rule ID: {}", id);

        AlertConfigEntity existing = alertConfigRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Regla de alerta no encontrada con ID: " + id));

        AlertConfigEntity snapshot = cloneForAudit(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();

        alertConfigRepositoryPort.softDelete(id);

        AlertConfigEntity afterSnapshot = cloneForAudit(existing);
        afterSnapshot.setIsDeleted(true);
        afterSnapshot.setStatus(AlertStatus.INACTIVE);
        afterSnapshot.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));

        logAuditChange(currentUser, "ALERT_CONFIG_DELETED", id, snapshot, afterSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertConfigAuditResponse> getAlertConfigAuditLogs(UUID id) {
        log.debug("Fetching audit logs for alert config ID: {}", id);

        if (alertConfigRepositoryPort.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Regla de alerta no encontrada con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("ALERT_CONFIG", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(UserEntity::getUsername)
                                .orElse("UNKNOWN");
                    }
                    List<AlertConfigAuditResponse.AuditDetailResponse> details = logEntry.getDetails().stream()
                            .map(d -> AlertConfigAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return AlertConfigAuditResponse.builder()
                            .logId(logEntry.getLogId())
                            .action(logEntry.getAction())
                            .username(username)
                            .createdAt(logEntry.getCreatedAt())
                            .details(details)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OrganizationEntity resolveOrganization(UUID reqOrgId, String username) {
        if (reqOrgId != null) {
            return organizationJpaRepository.findById(reqOrgId)
                    .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + reqOrgId));
        }

        UserEntity user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + username));
        if (user.getOrganization() == null) {
            throw new ValidationException("El usuario no pertenece a ninguna organización válida.");
        }
        return user.getOrganization();
    }

    private AlertConfigEntity cloneForAudit(AlertConfigEntity source) {
        if (source == null) return null;
        return AlertConfigEntity.builder()
                .id(source.getId())
                .name(source.getName())
                .category(source.getCategory())
                .event(source.getEvent())
                .priority(source.getPriority())
                .status(source.getStatus())
                .channels(source.getChannels() != null ? new java.util.ArrayList<>(source.getChannels()) : null)
                .recipients(source.getRecipients() != null ? new java.util.ArrayList<>(source.getRecipients()) : null)
                .condition(source.getCondition())
                .value(source.getValue())
                .unit(source.getUnit())
                .recurrence(source.getRecurrence())
                .escalation(source.getEscalation())
                .messageTemplate(source.getMessageTemplate())
                .description(source.getDescription())
                .isDeleted(source.getIsDeleted())
                .version(source.getVersion())
                .organization(source.getOrganization())
                .build();
    }

    private void logAuditChange(String username, String action, UUID entityId, AlertConfigEntity before, AlertConfigEntity after) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                Map<String, Object> beforeState = buildAuditState(before);
                Map<String, Object> afterState = buildAuditState(after);
                auditService.log(actor, action, "ALERT_CONFIG", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for alert config operation", e);
        }
    }

    private Map<String, Object> buildAuditState(AlertConfigEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("id", entity.getId() != null ? entity.getId().toString() : null);
        state.put("name", entity.getName());
        state.put("category", entity.getCategory() != null ? entity.getCategory().name() : null);
        state.put("event", entity.getEvent() != null ? entity.getEvent().name() : null);
        state.put("priority", entity.getPriority() != null ? entity.getPriority().name() : null);
        state.put("status", entity.getStatus() != null ? entity.getStatus().name() : null);
        state.put("channels", entity.getChannels() != null ? entity.getChannels().toString() : null);
        state.put("recipients", entity.getRecipients() != null ? entity.getRecipients().toString() : null);
        state.put("condition", entity.getCondition() != null ? entity.getCondition().name() : null);
        state.put("value", entity.getValue() != null ? entity.getValue().toString() : null);
        state.put("unit", entity.getUnit() != null ? entity.getUnit().name() : null);
        state.put("recurrence", entity.getRecurrence() != null ? entity.getRecurrence().name() : null);
        state.put("escalation", entity.getEscalation() != null ? entity.getEscalation().name() : null);
        state.put("isDeleted", entity.getIsDeleted());
        if (entity.getOrganization() != null) state.put("organizationId", entity.getOrganization().getId().toString());
        return state;
    }
}
