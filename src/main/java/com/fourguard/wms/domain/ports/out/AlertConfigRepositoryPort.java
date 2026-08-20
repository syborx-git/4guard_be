package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.application.dto.request.AlertConfigFilterRequest;
import com.fourguard.wms.infrastructure.persistence.entity.AlertConfigEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertConfigRepositoryPort {

    AlertConfigEntity save(AlertConfigEntity entity);

    Optional<AlertConfigEntity> findById(UUID id);

    List<AlertConfigEntity> findAll(AlertConfigFilterRequest filter);

    boolean existsByOrganizationIdAndNameAndIsDeletedFalse(UUID organizationId, String name);

    boolean existsByOrganizationIdAndNameAndIdNotAndIsDeletedFalse(UUID organizationId, String name, UUID id);

    void softDelete(UUID id);
}
