package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.ConvertCurrencyRequest;
import com.fourguard.wms.application.dto.request.CreateExchangeRateRequest;
import com.fourguard.wms.application.dto.response.ConvertCurrencyResponse;
import com.fourguard.wms.application.dto.response.ExchangeRateResponse;
import com.fourguard.wms.application.dto.response.ParityMatrixResponse;

import com.fourguard.wms.application.dto.response.BanxicoLiveRateResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExchangeRateUseCase {
    List<ExchangeRateResponse> getExchangeRates(UUID organizationId, String fromCode, String toCode, LocalDate date);
    ParityMatrixResponse getLatestParityMatrix(UUID organizationId);
    ExchangeRateResponse saveExchangeRate(CreateExchangeRateRequest request);
    ConvertCurrencyResponse convert(ConvertCurrencyRequest request);
    List<ExchangeRateResponse> syncBanxicoRates(UUID organizationId);
    BanxicoLiveRateResponse fetchLiveBanxicoRateBySeries(String seriesId);
}
