package com.fourguard.wms.application.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConvertCurrencyResponse {

    private UUID fromCurrencyId;
    private String fromCode;

    private UUID toCurrencyId;
    private String toCode;

    private BigDecimal originalAmount;
    private BigDecimal convertedAmount;
    private BigDecimal rateUsed;
    private LocalDate effectiveDate;
    private String conversionPath;
}
