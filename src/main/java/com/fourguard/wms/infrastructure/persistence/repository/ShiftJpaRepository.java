package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.ShiftScopeType;
import com.fourguard.wms.domain.enums.ShiftStatus;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ShiftJpaRepository extends JpaRepository<ShiftEntity, UUID> {

    Optional<ShiftEntity> findByIdAndIsDeletedFalse(UUID id);

    Optional<ShiftEntity> findByCodeAndBranchIdAndIsDeletedFalse(String code, UUID branchId);

    boolean existsByCodeAndBranchIdAndIsDeletedFalse(String code, UUID branchId);

    boolean existsByCodeAndBranchIdAndIdNotAndIsDeletedFalse(String code, UUID branchId, UUID id);

    @Query("""
        SELECT DISTINCT s FROM ShiftEntity s JOIN s.operatingDays d
        WHERE s.isDeleted = false
          AND s.status = com.fourguard.wms.domain.enums.ShiftStatus.ACTIVE
          AND (:branchId IS NULL OR s.branch.id = :branchId)
          AND (:excludeId IS NULL OR s.id <> :excludeId)
          AND d IN :days
          AND (
              (s.isOvernight = false AND :isOvernightNew = false AND s.startTime < :endTime AND s.endTime > :startTime)
              OR (s.isOvernight = true OR :isOvernightNew = true)
          )
    """)
    List<ShiftEntity> findOverlappingShifts(
            @Param("branchId") UUID branchId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("days") Set<String> days,
            @Param("isOvernightNew") boolean isOvernightNew,
            @Param("excludeId") UUID excludeId
    );

    @Query("""
        SELECT DISTINCT s FROM ShiftEntity s LEFT JOIN s.operatingDays d
        WHERE s.isDeleted = false
          AND (CAST(:branchId AS string) IS NULL OR s.branch.id = :branchId)
          AND (CAST(:sectionId AS string) IS NULL OR s.warehouseSection.id = :sectionId)
          AND (:status IS NULL OR s.status = :status)
          AND (:scopeType IS NULL OR s.scopeType = :scopeType)
          AND (CAST(:day AS string) IS NULL OR d = :day)
          AND (CAST(:search AS string) IS NULL OR LOWER(s.name) LIKE :search OR LOWER(s.code) LIKE :search)
        ORDER BY s.createdAt DESC
    """)
    List<ShiftEntity> findWithFilters(
            @Param("branchId") UUID branchId,
            @Param("sectionId") UUID sectionId,
            @Param("status") ShiftStatus status,
            @Param("scopeType") ShiftScopeType scopeType,
            @Param("day") String day,
            @Param("search") String search
    );
}
