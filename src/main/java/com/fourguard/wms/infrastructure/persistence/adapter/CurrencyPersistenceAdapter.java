package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.model.Currency;
import com.fourguard.wms.domain.ports.out.CurrencyRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.CurrencyEntity;
import com.fourguard.wms.infrastructure.persistence.repository.CurrencyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CurrencyPersistenceAdapter implements CurrencyRepositoryPort {

    private final CurrencyJpaRepository repository;

    @Override
    public Currency save(Currency currency) {
        CurrencyEntity entity = toEntity(currency);
        CurrencyEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Currency> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Currency> findByOrganizationIdAndCode(UUID organizationId, String code) {
        return repository.findByOrganizationIdAndCode(organizationId, code).map(this::toDomain);
    }

    @Override
    public Optional<Currency> findBaseCurrencyByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationIdAndIsBaseTrue(organizationId).map(this::toDomain);
    }

    @Override
    public List<Currency> findAllByOrganizationId(UUID organizationId) {
        return repository.findAllByOrganizationId(organizationId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByOrganizationIdAndCode(UUID organizationId, String code) {
        return repository.existsByOrganizationIdAndCode(organizationId, code);
    }

    private CurrencyEntity toEntity(Currency domain) {
        if (domain == null) return null;
        return CurrencyEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .code(domain.getCode())
                .name(domain.getName())
                .symbol(domain.getSymbol())
                .isBase(domain.getIsBase())
                .status(domain.getStatus())
                .decimalPlaces(domain.getDecimalPlaces())
                .createdAt(domain.getCreatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedAt(domain.getUpdatedAt())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }

    private Currency toDomain(CurrencyEntity entity) {
        if (entity == null) return null;
        return Currency.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .code(entity.getCode())
                .name(entity.getName())
                .symbol(entity.getSymbol())
                .isBase(entity.getIsBase())
                .status(entity.getStatus())
                .decimalPlaces(entity.getDecimalPlaces())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
