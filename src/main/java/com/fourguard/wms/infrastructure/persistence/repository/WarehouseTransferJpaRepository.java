package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.TransferStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseTransferJpaRepository extends
        JpaRepository<WarehouseTransferEntity, UUID>,
        JpaSpecificationExecutor<WarehouseTransferEntity> {

    List<WarehouseTransferEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<WarehouseTransferEntity> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, TransferStatus status);

    @Query(value = "SELECT nextval('wms.seq_transfer_folio')", nativeQuery = true)
    long getNextFolioSequenceValue();
}
