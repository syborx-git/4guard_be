package com.fourguard.wms.application.dto.response.quality;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QualityDashboardKpisResponse {
    private long totalActiveBlocks;
    private long totalBlocked;
    private long totalUnderInspection;
    private long totalReleases;
    private long distributionReleases;
    private long destructionReleases;
    private long returnReleases;
    private long totalVerifications;
    private long approvedVerifications;
    private long pendingVerifications;
    private long totalClaims;
    private BigDecimal totalDamagedQty;
    private BigDecimal totalLostQty;
    private BigDecimal totalClaimsCost;
}
