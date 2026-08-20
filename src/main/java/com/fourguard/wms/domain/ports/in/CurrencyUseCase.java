package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreateCurrencyRequest;
import com.fourguard.wms.application.dto.request.UpdateCurrencyRequest;
import com.fourguard.wms.application.dto.request.UpdateCurrencyStatusRequest;
import com.fourguard.wms.application.dto.response.CurrencyResponse;

import java.util.List;
import java.util.UUID;

public interface CurrencyUseCase {
    List<CurrencyResponse> getCurrencies(UUID organizationId);
    CurrencyResponse getCurrencyById(UUID id);
    CurrencyResponse createCurrency(CreateCurrencyRequest request);
    CurrencyResponse updateCurrency(UUID id, UpdateCurrencyRequest request);
    CurrencyResponse updateCurrencyStatus(UUID id, UpdateCurrencyStatusRequest request);
    CurrencyResponse setBaseCurrency(UUID id);
}
