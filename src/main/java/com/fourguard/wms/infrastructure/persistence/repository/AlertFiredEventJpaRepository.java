package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.AlertFiredEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertFiredEventJpaRepository extends JpaRepository<AlertFiredEventEntity, UUID> {

    List<AlertFiredEventEntity> findByOrganizationIdOrderByTriggeredAtDesc(UUID organizationId);

    List<AlertFiredEventEntity> findByAlertConfigurationIdOrderByTriggeredAtDesc(UUID alertConfigurationId);
}
