package com.fourguard.wms.application.dto.response.reception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionLotResponse {

    private UUID id;
    private UUID receptionId;
    private String lotNumber;
    private UUID skuId;
    private String skuCode;
    private String productName;
    private LocalDate elaborationDate;
    private LocalDate expirationDate;
    private Integer shelfLifeDaysRemaining;
    private String shelfLifeStatus;
    private String status;
    private String observations;
    private Integer palletsCount;
    private Double piecesCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
