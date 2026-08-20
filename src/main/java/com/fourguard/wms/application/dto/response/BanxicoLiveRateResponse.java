package com.fourguard.wms.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BanxicoLiveRateResponse {
    private String seriesId;
    private String currencyCode;
    private String seriesTitle;
    private BigDecimal rate;
    private String publicationDate;
    private String sourceType;
}
