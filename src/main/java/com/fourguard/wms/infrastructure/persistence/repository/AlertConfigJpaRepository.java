package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.AlertConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AlertConfigJpaRepository extends JpaRepository<AlertConfigEntity, UUID>, JpaSpecificationExecutor<AlertConfigEntity> {

    Optional<AlertConfigEntity> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByOrganizationIdAndNameAndIsDeletedFalse(UUID organizationId, String name);

    boolean existsByOrganizationIdAndNameAndIdNotAndIsDeletedFalse(UUID organizationId, String name, UUID id);
}
