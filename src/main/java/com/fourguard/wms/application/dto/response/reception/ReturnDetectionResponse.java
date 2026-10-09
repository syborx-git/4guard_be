package com.fourguard.wms.application.dto.response.reception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDetectionResponse {
    private boolean isReturn;
    private UUID sourceOutboundId;
    private String sourceOutboundFolio;
    private String remisionNo;
    private UUID clientId;
    private String clientName;
    private UUID carrierId;
    private String carrierName;
    private String driverName;
    private String tractorPlates;
    private String boxPlates;
    private OffsetDateTime dispatchedAt;
    private Integer totalPallets;
    private BigDecimal totalPieces;
    private String destinationName;
    @Builder.Default
    private List<ExpectedReturnPalletDto> expectedPallets = new ArrayList<>();
}
