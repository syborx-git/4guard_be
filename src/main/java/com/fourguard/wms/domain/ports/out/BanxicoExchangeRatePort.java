package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.domain.enums.BanxicoSeries;

import com.fourguard.wms.application.dto.response.BanxicoLiveRateResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public interface BanxicoExchangeRatePort {
    Optional<BigDecimal> fetchLatestRate(BanxicoSeries series);
    Map<BanxicoSeries, BigDecimal> fetchAllLatestRates();
    Optional<BanxicoLiveRateResponse> fetchLiveRateBySeriesId(String seriesId);
}
