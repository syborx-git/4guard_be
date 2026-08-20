package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.OutboundStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseOutboundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseOutboundJpaRepository extends JpaRepository<WarehouseOutboundEntity, UUID> {

    List<WarehouseOutboundEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<WarehouseOutboundEntity> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, OutboundStatus status);

    @Query(value = "SELECT nextval('wms.seq_outbound_folio')", nativeQuery = true)
    long getNextFolioSequenceValue();

    @Query("SELECT o FROM WarehouseOutboundEntity o WHERE o.organization.id = :orgId " +
           "AND (:branchId IS NULL OR o.branch.id = :branchId) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:search IS NULL OR LOWER(o.folio) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(o.remisionNo) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(o.client.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(o.driverName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY o.createdAt DESC")
    List<WarehouseOutboundEntity> findWithFilters(
            @Param("orgId") UUID orgId,
            @Param("branchId") UUID branchId,
            @Param("status") OutboundStatus status,
            @Param("search") String search);
}
