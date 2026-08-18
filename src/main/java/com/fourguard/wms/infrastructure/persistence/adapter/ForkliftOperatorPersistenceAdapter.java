package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.ForkliftOperatorRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ForkliftOperatorEntity;
import com.fourguard.wms.infrastructure.persistence.repository.ForkliftOperatorJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary Adapter — implements {@link ForkliftOperatorRepositoryPort} using Spring Data JPA.
 * Acts as the bridge between the domain port and the JPA repository (HU-142).
 */
@Component
@RequiredArgsConstructor
public class ForkliftOperatorPersistenceAdapter implements ForkliftOperatorRepositoryPort {

    private final ForkliftOperatorJpaRepository repository;

    @Override
    public Optional<ForkliftOperatorEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<ForkliftOperatorEntity> findActiveById(UUID id) {
        return repository.findByIdAndIsDeletedFalse(id);
    }

    @Override
    public List<ForkliftOperatorEntity> findByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationIdAndIsDeletedFalse(organizationId);
    }

    @Override
    public List<ForkliftOperatorEntity> findAll() {
        return repository.findAllByIsDeletedFalse();
    }

    @Override
    public ForkliftOperatorEntity save(ForkliftOperatorEntity entity) {
        return repository.save(entity);
    }

    @Override
    public void softDeleteById(UUID id, String deletedBy) {
        repository.softDeleteById(id, deletedBy, OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public boolean existsByOrganizationIdAndCode(UUID organizationId, String code) {
        return repository.existsByOrganizationIdAndCodeAndIsDeletedFalse(organizationId, code);
    }

    @Override
    public boolean existsByOrganizationIdAndCodeAndIdNot(UUID organizationId, String code, UUID excludeId) {
        return repository.existsByOrganizationIdAndCodeAndIdNotAndIsDeletedFalse(organizationId, code, excludeId);
    }

    @Override
    public boolean existsByOrganizationIdAndLicenseNumberDc3(UUID organizationId, String licenseNumberDc3) {
        return repository.existsByOrganizationIdAndLicenseNumberDc3IgnoreCaseAndIsDeletedFalse(organizationId, licenseNumberDc3);
    }

    @Override
    public boolean existsByOrganizationIdAndLicenseNumberDc3AndIdNot(UUID organizationId, String licenseNumberDc3, UUID excludeId) {
        return repository.existsByOrganizationIdAndLicenseNumberDc3IgnoreCaseAndIdNotAndIsDeletedFalse(organizationId, licenseNumberDc3, excludeId);
    }

    @Override
    public int countByOrganizationId(UUID organizationId) {
        return repository.countByOrganizationIdAndIsDeletedFalse(organizationId);
    }
}
