package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.ReceptionStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseReceptionJpaRepository extends JpaRepository<WarehouseReceptionEntity, UUID> {

    List<WarehouseReceptionEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<WarehouseReceptionEntity> findByOrganizationIdAndBranchIdOrderByCreatedAtDesc(UUID organizationId, UUID branchId);

    List<WarehouseReceptionEntity> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, ReceptionStatus status);

    Optional<WarehouseReceptionEntity> findByFolio(String folio);

    boolean existsByOrganizationIdAndDocNumber(UUID organizationId, String docNumber);

    @Query(value = "SELECT nextval('wms.seq_reception_folio')", nativeQuery = true)
    long getNextFolioSequenceValue();

    @Query("SELECT r FROM WarehouseReceptionEntity r WHERE r.organization.id = :orgId " +
           "AND (:branchId IS NULL OR r.branch.id = :branchId) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:search IS NULL OR LOWER(r.folio) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(r.docNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(r.client.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(r.driverName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY r.createdAt DESC")
    List<WarehouseReceptionEntity> findWithFilters(
            @Param("orgId") UUID orgId,
            @Param("branchId") UUID branchId,
            @Param("status") ReceptionStatus status,
            @Param("search") String search);
}
