package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.OutboundStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseOutboundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseOutboundJpaRepository extends
        JpaRepository<WarehouseOutboundEntity, UUID>,
        JpaSpecificationExecutor<WarehouseOutboundEntity> {

    List<WarehouseOutboundEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<WarehouseOutboundEntity> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, OutboundStatus status);

    @Query(value = "SELECT nextval('wms.seq_outbound_folio')", nativeQuery = true)
    long getNextFolioSequenceValue();
}
