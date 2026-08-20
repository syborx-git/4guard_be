package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.response.CurrencyAuditResponse;
import com.fourguard.wms.application.mapper.CurrencyExchangeAuditMapper;
import com.fourguard.wms.domain.model.CurrencyExchangeAudit;
import com.fourguard.wms.domain.ports.in.CurrencyExchangeAuditUseCase;
import com.fourguard.wms.domain.ports.out.CurrencyExchangeAuditRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyExchangeAuditService implements CurrencyExchangeAuditUseCase {

    private final CurrencyExchangeAuditRepositoryPort auditRepositoryPort;
    private final CurrencyExchangeAuditMapper auditMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyAuditResponse> getAuditLogs(UUID organizationId, String entityType, UUID entityId, String action, OffsetDateTime startDate, OffsetDateTime endDate) {
        log.debug("Fetching currency exchange audit logs for organizationId: {}", organizationId);
        List<CurrencyExchangeAudit> auditLogs = auditRepositoryPort.findAuditLogs(organizationId, entityType, entityId, action, startDate, endDate);
        return auditMapper.toResponseList(auditLogs);
    }
}
