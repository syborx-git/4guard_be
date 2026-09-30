package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.UaMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UaMappingJpaRepository extends JpaRepository<UaMappingEntity, UUID> {
    List<UaMappingEntity> findByReceptionId(UUID receptionId);
    List<UaMappingEntity> findByPalletId(UUID palletId);
    Optional<UaMappingEntity> findByInternalUaCode(String internalUaCode);
    Optional<UaMappingEntity> findBySupplierUaCode(String supplierUaCode);
}
