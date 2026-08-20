package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.TransferStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseTransferJpaRepository extends JpaRepository<WarehouseTransferEntity, UUID> {

    List<WarehouseTransferEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<WarehouseTransferEntity> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, TransferStatus status);

    @Query(value = "SELECT nextval('wms.seq_transfer_folio')", nativeQuery = true)
    long getNextFolioSequenceValue();

    @Query("SELECT t FROM WarehouseTransferEntity t WHERE t.organization.id = :orgId " +
           "AND (:branchId IS NULL OR t.branch.id = :branchId) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:search IS NULL OR LOWER(t.folio) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(t.originLocation.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(t.destinationLocation.code) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.createdAt DESC")
    List<WarehouseTransferEntity> findWithFilters(
            @Param("orgId") UUID orgId,
            @Param("branchId") UUID branchId,
            @Param("status") TransferStatus status,
            @Param("search") String search);
}
