package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.LocationStatus;
import com.fourguard.wms.infrastructure.persistence.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseMapLocationJpaRepository extends JpaRepository<LocationEntity, UUID> {

    @Query("""
        SELECT l FROM LocationEntity l
        WHERE l.section.id = :sectionId
        ORDER BY l.code ASC
    """)
    List<LocationEntity> findBySectionIdOrderByCodeAsc(@Param("sectionId") UUID sectionId);

    @Query("""
        SELECT l FROM LocationEntity l
        WHERE l.section.id = :sectionId
          AND (:status IS NULL OR l.status = :status)
          AND (:search IS NULL OR LOWER(l.code) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY l.code ASC
    """)
    List<LocationEntity> findFilteredPositions(
        @Param("sectionId") UUID sectionId,
        @Param("status") LocationStatus status,
        @Param("search") String search
    );

    @Query("SELECT COUNT(l) FROM LocationEntity l WHERE l.section.id = :sectionId AND l.status = com.fourguard.wms.domain.enums.LocationStatus.BLOCKED")
    long countBlockedBySection(@Param("sectionId") UUID sectionId);

    @Query("SELECT COUNT(l) FROM LocationEntity l WHERE l.section.id = :sectionId AND l.currentOccupancy > 0")
    long countOccupiedBySection(@Param("sectionId") UUID sectionId);
}
