package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.ReceptionStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseReceptionJpaRepository extends
        JpaRepository<WarehouseReceptionEntity, UUID>,
        JpaSpecificationExecutor<WarehouseReceptionEntity> {

    List<WarehouseReceptionEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<WarehouseReceptionEntity> findByOrganizationIdAndBranchIdOrderByCreatedAtDesc(UUID organizationId, UUID branchId);

    List<WarehouseReceptionEntity> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, ReceptionStatus status);

    Optional<WarehouseReceptionEntity> findByFolio(String folio);

    boolean existsByOrganizationIdAndDocNumber(UUID organizationId, String docNumber);

    @Query(value = "SELECT nextval('wms.seq_reception_folio')", nativeQuery = true)
    long getNextFolioSequenceValue();
}
