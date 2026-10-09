package com.fourguard.wms.application.dto.response.quality;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityMonthlyBoardResponse {

    private int year;
    private int month;
    private String monthName;
    private String branchName;
    
    // 10 Bento Grid KPI Cards
    private List<MonthlyKpiCardDto> kpiCards;

    // Resúmenes y desgloses analíticos
    private Map<String, Long> releasesByCollaborator;
    private Map<String, Long> rootCauseDistribution;
    private Map<String, Long> storageDeviationsByType;
    private Map<String, Long> inboundDeviationsByType;
    private Map<String, Long> clientClaimsByOrigin;
    private Map<String, Long> actionsTakenDistribution;

    // Totales clave
    private long totalInspectedLots;
    private long totalDeviations;
    private long totalDamagedPieces;
    private BigDecimal ptDamagedPieces;
    private BigDecimal packagingDamagedPieces;
    private BigDecimal greenCoffeeDamagedPieces;
    private BigDecimal totalNonQualityCost;

    // Desviaciones del período
    private List<QualityDeviationResponse> deviations;
}
