package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LocationJpaRepository extends JpaRepository<LocationEntity, UUID> {
    List<LocationEntity> findByBranchId(UUID branchId);
    List<LocationEntity> findBySectionId(UUID sectionId);
    List<LocationEntity> findByBranchIdAndIsBlockedFalse(UUID branchId);
    java.util.Optional<LocationEntity> findByBranchIdAndCode(UUID branchId, String code);
    java.util.Optional<LocationEntity> findFirstByCode(String code);

    /** Checks whether another location (different ID) already uses the given code. */
    boolean existsByCodeAndIdNot(String code, UUID id);
}

