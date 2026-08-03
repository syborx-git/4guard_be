package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurrencyJpaRepository extends JpaRepository<CurrencyEntity, UUID> {
    Optional<CurrencyEntity> findByOrganizationIdAndCode(UUID organizationId, String code);
    Optional<CurrencyEntity> findByOrganizationIdAndIsBaseTrue(UUID organizationId);
    List<CurrencyEntity> findAllByOrganizationId(UUID organizationId);
    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);
}
