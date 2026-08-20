package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.domain.enums.ExchangeRateStatus;
import com.fourguard.wms.domain.model.ExchangeRate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepositoryPort {
    ExchangeRate save(ExchangeRate exchangeRate);
    Optional<ExchangeRate> findById(UUID id);
    Optional<ExchangeRate> findLatestRate(UUID organizationId, UUID fromCurrencyId, UUID toCurrencyId, LocalDate date);
    List<ExchangeRate> findRatesWithFilters(UUID organizationId, UUID fromCurrencyId, UUID toCurrencyId, LocalDate date);
    List<ExchangeRate> findActiveRatesByOrganizationId(UUID organizationId);
}
