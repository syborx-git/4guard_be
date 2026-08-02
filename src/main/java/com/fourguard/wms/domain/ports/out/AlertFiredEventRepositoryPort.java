package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.AlertFiredEventEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertFiredEventRepositoryPort {

    AlertFiredEventEntity save(AlertFiredEventEntity entity);

    Optional<AlertFiredEventEntity> findById(UUID id);

    List<AlertFiredEventEntity> findByOrganizationId(UUID organizationId);

    List<AlertFiredEventEntity> findByAlertConfigurationId(UUID alertConfigurationId);
}
