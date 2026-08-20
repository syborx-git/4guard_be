package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionPalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseReceptionPalletJpaRepository extends JpaRepository<WarehouseReceptionPalletEntity, UUID> {

    List<WarehouseReceptionPalletEntity> findByReceptionIdOrderByPalletNumberAsc(UUID receptionId);

    Optional<WarehouseReceptionPalletEntity> findByReceptionIdAndId(UUID receptionId, UUID palletId);

    boolean existsByReceptionIdAndPalletCode(UUID receptionId, String palletCode);

    int countByReceptionId(UUID receptionId);
}
