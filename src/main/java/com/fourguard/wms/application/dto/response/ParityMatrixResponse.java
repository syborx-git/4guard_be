package com.fourguard.wms.application.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParityMatrixResponse {

    private UUID organizationId;
    private CurrencyResponse baseCurrency;
    private List<ExchangeRateResponse> activeRates;
}
