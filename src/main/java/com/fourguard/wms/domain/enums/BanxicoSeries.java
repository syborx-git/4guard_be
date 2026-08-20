package com.fourguard.wms.domain.enums;

import lombok.Getter;

@Getter
public enum BanxicoSeries {
    USD("SF57805", "USD", "Dólar Estadounidense FIX"),
    EUR("SF46410", "EUR", "Euro");

    private final String seriesId;
    private final String currencyCode;
    private final String description;

    BanxicoSeries(String seriesId, String currencyCode, String description) {
        this.seriesId = seriesId;
        this.currencyCode = currencyCode;
        this.description = description;
    }
}
