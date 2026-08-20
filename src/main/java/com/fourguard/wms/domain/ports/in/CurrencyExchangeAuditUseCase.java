package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.response.CurrencyAuditResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CurrencyExchangeAuditUseCase {
    List<CurrencyAuditResponse> getAuditLogs(UUID organizationId, String entityType, UUID entityId, String action, OffsetDateTime startDate, OffsetDateTime endDate);
}
