package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.WarehouseSectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseMapSectionJpaRepository extends JpaRepository<WarehouseSectionEntity, UUID> {

    @Query("""
        SELECT DISTINCT s FROM WarehouseSectionEntity s
        LEFT JOIN FETCH s.locations l
        WHERE s.branch.id = :branchId
        ORDER BY s.code ASC
    """)
    List<WarehouseSectionEntity> findSectionsWithLocationsByBranchId(@Param("branchId") UUID branchId);
}
