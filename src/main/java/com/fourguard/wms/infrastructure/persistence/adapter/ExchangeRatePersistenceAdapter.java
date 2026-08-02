package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.enums.ExchangeRateStatus;
import com.fourguard.wms.domain.model.ExchangeRate;
import com.fourguard.wms.domain.ports.out.ExchangeRateRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.CurrencyEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ExchangeRateEntity;
import com.fourguard.wms.infrastructure.persistence.repository.CurrencyJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.ExchangeRateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExchangeRatePersistenceAdapter implements ExchangeRateRepositoryPort {

    private final ExchangeRateJpaRepository repository;
    private final CurrencyJpaRepository currencyRepository;

    @Override
    public ExchangeRate save(ExchangeRate domain) {
        ExchangeRateEntity entity = toEntity(domain);
        ExchangeRateEntity saved = repository.save(entity);
        return enrichDomain(toDomain(saved));
    }

    @Override
    public Optional<ExchangeRate> findById(UUID id) {
        return repository.findById(id).map(this::toDomain).map(this::enrichDomain);
    }

    @Override
    public Optional<ExchangeRate> findLatestRate(UUID organizationId, UUID fromCurrencyId, UUID toCurrencyId, LocalDate date) {
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        List<ExchangeRateEntity> rates = repository.findTopRates(organizationId, fromCurrencyId, toCurrencyId, searchDate);
        if (rates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(enrichDomain(toDomain(rates.get(0))));
    }

    @Override
    public List<ExchangeRate> findRatesWithFilters(UUID organizationId, UUID fromCurrencyId, UUID toCurrencyId, LocalDate date) {
        List<ExchangeRateEntity> entities = repository.findWithFilters(organizationId, fromCurrencyId, toCurrencyId, date);
        return enrichDomainList(entities.stream().map(this::toDomain).collect(Collectors.toList()));
    }

    @Override
    public List<ExchangeRate> findActiveRatesByOrganizationId(UUID organizationId) {
        List<ExchangeRateEntity> entities = repository.findAllByOrganizationIdAndStatus(organizationId, ExchangeRateStatus.ACTIVE);
        return enrichDomainList(entities.stream().map(this::toDomain).collect(Collectors.toList()));
    }

    private ExchangeRateEntity toEntity(ExchangeRate domain) {
        if (domain == null) return null;
        return ExchangeRateEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .fromCurrencyId(domain.getFromCurrencyId())
                .toCurrencyId(domain.getToCurrencyId())
                .rate(domain.getRate())
                .inverseRate(domain.getInverseRate())
                .effectiveDate(domain.getEffectiveDate())
                .sourceType(domain.getSourceType())
                .status(domain.getStatus())
                .notes(domain.getNotes())
                .createdAt(domain.getCreatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedAt(domain.getUpdatedAt())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }

    private ExchangeRate toDomain(ExchangeRateEntity entity) {
        if (entity == null) return null;
        return ExchangeRate.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .fromCurrencyId(entity.getFromCurrencyId())
                .toCurrencyId(entity.getToCurrencyId())
                .rate(entity.getRate())
                .inverseRate(entity.getInverseRate())
                .effectiveDate(entity.getEffectiveDate())
                .sourceType(entity.getSourceType())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private ExchangeRate enrichDomain(ExchangeRate rate) {
        if (rate == null) return null;
        if (rate.getFromCurrencyId() != null) {
            currencyRepository.findById(rate.getFromCurrencyId())
                    .ifPresent(c -> rate.setFromCurrencyCode(c.getCode()));
        }
        if (rate.getToCurrencyId() != null) {
            currencyRepository.findById(rate.getToCurrencyId())
                    .ifPresent(c -> rate.setToCurrencyCode(c.getCode()));
        }
        return rate;
    }

    private List<ExchangeRate> enrichDomainList(List<ExchangeRate> rates) {
        if (rates == null || rates.isEmpty()) return rates;
        List<UUID> currencyIds = rates.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getFromCurrencyId(), r.getToCurrencyId()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, String> currencyCodeMap = currencyRepository.findAllById(currencyIds)
                .stream()
                .collect(Collectors.toMap(CurrencyEntity::getId, CurrencyEntity::getCode));

        rates.forEach(r -> {
            r.setFromCurrencyCode(currencyCodeMap.get(r.getFromCurrencyId()));
            r.setToCurrencyCode(currencyCodeMap.get(r.getToCurrencyId()));
        });
        return rates;
    }
}
