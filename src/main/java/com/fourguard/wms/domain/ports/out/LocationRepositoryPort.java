package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.LocationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port OUT — Location repository contract. */
public interface LocationRepositoryPort {
    Optional<LocationEntity> findById(UUID id);
    List<LocationEntity>     findByBranchId(UUID branchId);
    List<LocationEntity>     findAvailableByBranchId(UUID branchId);
    Optional<LocationEntity> findByBranchIdAndCode(UUID branchId, String code);
    Optional<LocationEntity> findFirstByCode(String code);
    LocationEntity           save(LocationEntity location);
    void                     deleteById(UUID id);
    List<LocationEntity>     findAll();

    /** Finds all locations associated with a warehouse section. */
    List<LocationEntity> findBySectionId(UUID sectionId);

    /**
     * Returns true if there is a location with the given {@code code} whose ID is
     * different from {@code excludeId}. Used to detect duplicate codes on update.
     */
    boolean existsByCodeAndIdNot(String code, UUID excludeId);
}


