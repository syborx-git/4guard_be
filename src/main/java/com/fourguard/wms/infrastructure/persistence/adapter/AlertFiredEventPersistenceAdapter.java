package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.AlertFiredEventRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AlertFiredEventEntity;
import com.fourguard.wms.infrastructure.persistence.repository.AlertFiredEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AlertFiredEventPersistenceAdapter implements AlertFiredEventRepositoryPort {

    private final AlertFiredEventJpaRepository repository;

    @Override
    public AlertFiredEventEntity save(AlertFiredEventEntity entity) {
        return repository.save(entity);
    }

    @Override
    public Optional<AlertFiredEventEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<AlertFiredEventEntity> findByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationIdOrderByTriggeredAtDesc(organizationId);
    }

    @Override
    public List<AlertFiredEventEntity> findByAlertConfigurationId(UUID alertConfigurationId) {
        return repository.findByAlertConfigurationIdOrderByTriggeredAtDesc(alertConfigurationId);
    }
}
