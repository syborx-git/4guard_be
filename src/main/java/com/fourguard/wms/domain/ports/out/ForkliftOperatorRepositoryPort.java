package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.ForkliftOperatorEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary (Driven) Port — Repository contract for Forklift Operator persistence (HU-142).
 * Implemented by {@code ForkliftOperatorPersistenceAdapter}.
 */
public interface ForkliftOperatorRepositoryPort {

    Optional<ForkliftOperatorEntity> findById(UUID id);

    Optional<ForkliftOperatorEntity> findActiveById(UUID id);

    List<ForkliftOperatorEntity> findByOrganizationId(UUID organizationId);

    List<ForkliftOperatorEntity> findAll();

    ForkliftOperatorEntity save(ForkliftOperatorEntity entity);

    void softDeleteById(UUID id, String deletedBy);

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeAndIdNot(UUID organizationId, String code, UUID excludeId);

    boolean existsByOrganizationIdAndLicenseNumberDc3(UUID organizationId, String licenseNumberDc3);

    boolean existsByOrganizationIdAndLicenseNumberDc3AndIdNot(UUID organizationId, String licenseNumberDc3, UUID excludeId);

    /** Returns the highest sequential code number for a given organization, used for code auto-generation. */
    int countByOrganizationId(UUID organizationId);
}
