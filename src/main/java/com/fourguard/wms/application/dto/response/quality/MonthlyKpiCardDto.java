package com.fourguard.wms.application.dto.response.quality;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyKpiCardDto {

    private int kpiNumber;
    private String id;
    private String title;
    private String category;
    private String value;
    private BigDecimal numericValue;
    private String unit;
    private String target;
    private BigDecimal targetValue;
    private BigDecimal compliancePercentage;
    private String status; // SUCCESS, WARNING, DANGER, INFO
    private BigDecimal previousMonthDiff;
    private String trend; // UP, DOWN, STABLE
    private String sublabel;
    private List<BigDecimal> sparklineData;
}
