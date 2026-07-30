package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.CarrierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarrierJpaRepository extends JpaRepository<CarrierEntity, UUID> {
    List<CarrierEntity> findByOrganizationId(UUID organizationId);
    boolean existsByOrganizationIdAndName(UUID organizationId, String name);
    boolean existsByTaxIdIgnoreCase(String taxId);
    boolean existsByTaxIdIgnoreCaseAndIdNot(String taxId, UUID id);
    boolean existsByOrganizationIdAndTaxIdIgnoreCase(UUID organizationId, String taxId);
    boolean existsByOrganizationIdAndTaxIdIgnoreCaseAndIdNot(UUID organizationId, String taxId, UUID id);
}
