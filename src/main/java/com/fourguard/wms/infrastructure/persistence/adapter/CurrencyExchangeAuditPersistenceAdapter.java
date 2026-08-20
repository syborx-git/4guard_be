package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.enums.CurrencyAuditAction;
import com.fourguard.wms.domain.enums.CurrencyAuditEntityType;
import com.fourguard.wms.domain.model.CurrencyExchangeAudit;
import com.fourguard.wms.domain.ports.out.CurrencyExchangeAuditRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.CurrencyExchangeAuditEntity;
import com.fourguard.wms.infrastructure.persistence.repository.CurrencyExchangeAuditJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CurrencyExchangeAuditPersistenceAdapter implements CurrencyExchangeAuditRepositoryPort {

    private final CurrencyExchangeAuditJpaRepository repository;

    @Override
    public CurrencyExchangeAudit save(CurrencyExchangeAudit domain) {
        CurrencyExchangeAuditEntity entity = toEntity(domain);
        CurrencyExchangeAuditEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<CurrencyExchangeAudit> findAuditLogs(UUID organizationId, String entityTypeStr, UUID entityId, String actionStr, OffsetDateTime startDate, OffsetDateTime endDate) {
        CurrencyAuditEntityType entityType = null;
        if (entityTypeStr != null && !entityTypeStr.isBlank()) {
            try {
                entityType = CurrencyAuditEntityType.valueOf(entityTypeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        CurrencyAuditAction action = null;
        if (actionStr != null && !actionStr.isBlank()) {
            try {
                action = CurrencyAuditAction.valueOf(actionStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        List<CurrencyExchangeAuditEntity> entities = repository.findAuditLogsWithFilters(organizationId, entityType, entityId, action, startDate, endDate);
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private CurrencyExchangeAuditEntity toEntity(CurrencyExchangeAudit domain) {
        if (domain == null) return null;
        return CurrencyExchangeAuditEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .entityType(domain.getEntityType())
                .entityId(domain.getEntityId())
                .action(domain.getAction())
                .description(domain.getDescription())
                .previousValue(domain.getPreviousValue())
                .newValue(domain.getNewValue())
                .performedBy(domain.getPerformedBy())
                .performedAt(domain.getPerformedAt())
                .build();
    }

    private CurrencyExchangeAudit toDomain(CurrencyExchangeAuditEntity entity) {
        if (entity == null) return null;
        return CurrencyExchangeAudit.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .description(entity.getDescription())
                .previousValue(entity.getPreviousValue())
                .newValue(entity.getNewValue())
                .performedBy(entity.getPerformedBy())
                .performedAt(entity.getPerformedAt())
                .build();
    }
}
