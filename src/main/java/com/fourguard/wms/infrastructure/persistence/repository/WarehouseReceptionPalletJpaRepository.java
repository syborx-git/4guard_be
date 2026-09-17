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

    Optional<WarehouseReceptionPalletEntity> findByReceptionIdAndPalletCode(UUID receptionId, String palletCode);

    boolean existsByReceptionIdAndPalletCode(UUID receptionId, String palletCode);

    int countByReceptionId(UUID receptionId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(p.palletNumber), 0) FROM WarehouseReceptionPalletEntity p")
    int findMaxPalletNumber();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(p.palletNumber), 0) FROM WarehouseReceptionPalletEntity p WHERE p.reception.organization.id = :orgId AND p.reception.branch.id = :branchId")
    int findMaxPalletNumberByOrgAndBranch(@org.springframework.data.repository.query.Param("orgId") UUID orgId, @org.springframework.data.repository.query.Param("branchId") UUID branchId);
}
