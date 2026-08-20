package com.fourguard.wms.application.dto.response;

import com.fourguard.wms.domain.enums.ExchangeRateSourceType;
import com.fourguard.wms.domain.enums.ExchangeRateStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateResponse {

    private UUID id;
    private UUID organizationId;
    private UUID fromCurrencyId;
    private String fromCurrencyCode;
    private UUID toCurrencyId;
    private String toCurrencyCode;
    private BigDecimal rate;
    private BigDecimal inverseRate;
    private LocalDate effectiveDate;
    private ExchangeRateSourceType sourceType;
    private ExchangeRateStatus status;
    private String notes;
    private OffsetDateTime createdAt;
    private String createdBy;
    private OffsetDateTime updatedAt;
    private String updatedBy;
}
