package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.domain.model.CurrencyExchangeAudit;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CurrencyExchangeAuditRepositoryPort {
    CurrencyExchangeAudit save(CurrencyExchangeAudit audit);
    List<CurrencyExchangeAudit> findAuditLogs(UUID organizationId, String entityType, UUID entityId, String action, OffsetDateTime startDate, OffsetDateTime endDate);
}
