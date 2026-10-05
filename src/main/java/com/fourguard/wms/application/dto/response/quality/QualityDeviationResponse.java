package com.fourguard.wms.application.dto.response.quality;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityDeviationResponse {

    private UUID id;
    private String folio;
    private String remisionNumber;
    private String skuId;
    private String skuDescription;
    private String uaCode;
    private String materialType;
    private LocalDate deviationDate;
    private LocalTime deviationTime;
    private UUID detectedById;
    private String detectedByName;
    private String responsibleCollaborator;
    private String bayLocationCode;
    private Integer damagedUnits;
    private BigDecimal materialCost;
    private String currency;
    private String conditionDeviation;
    private String rootCauseMotive;
    private String originArea;
    private List<String> evidencePhotoUrls;
    private String actionTaken;
    private String observations;
    private Boolean isResolved;
    private OffsetDateTime createdAt;
}
