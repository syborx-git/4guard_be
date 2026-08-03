package com.fourguard.wms.domain.model;

import com.fourguard.wms.domain.enums.CurrencyAuditAction;
import com.fourguard.wms.domain.enums.CurrencyAuditEntityType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CurrencyExchangeAudit {

    private UUID id;
    private UUID organizationId;
    private CurrencyAuditEntityType entityType;
    private UUID entityId;
    private CurrencyAuditAction action;
    private String description;
    private String previousValue;
    private String newValue;
    private String performedBy;
    private OffsetDateTime performedAt;
}
