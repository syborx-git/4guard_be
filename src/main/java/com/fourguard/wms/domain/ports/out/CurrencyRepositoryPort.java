package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.domain.model.Currency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurrencyRepositoryPort {
    Currency save(Currency currency);
    Optional<Currency> findById(UUID id);
    Optional<Currency> findByOrganizationIdAndCode(UUID organizationId, String code);
    Optional<Currency> findBaseCurrencyByOrganizationId(UUID organizationId);
    List<Currency> findAllByOrganizationId(UUID organizationId);
    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);
}
