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

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE LocationEntity l SET l.currentOccupancy = GREATEST(0, COALESCE(l.currentOccupancy, 0) - :count) WHERE l.id = :locationId")
    int decrementOccupancy(UUID locationId, int count);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE LocationEntity l SET l.currentOccupancy = COALESCE(l.currentOccupancy, 0) + :count WHERE l.id = :locationId")
    int incrementOccupancy(UUID locationId, int count);
}

