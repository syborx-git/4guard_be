package com.fourguard.wms.application.dto.request.quality.ai;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityAiRuleEvaluationRequest {

    private UUID itemId;
    private String sku;
    private String productType; // PRODUCTO_TERMINADO, FRASCO_VIDRIO, ALIMENTO, LAMINADO

    // Parámetros de inspección física
    private Double palletTiltDegrees; // Inclinación en grados (máx 5.0)
    private Integer damagedPrimaryUnits; // Daños en empaque primario (máx 1)
    private Integer damagedSecondaryBoxes; // Daños en empaque secundario (máx 2)
    private Double filmOpeningCm; // Abertura en emplaye en cm (máx 5.0)
    private Boolean isUseDiurexRequested; // Intento de usar Diurex
    private Integer brokenBoardsCount; // Tablas o tacones rotos en tarima
    private Double relativeHumidityPercent; // Lectura de humedad relativa %
    private Double tarpTearLengthCm; // Rasgadura en lona de transporte en cm
    private Boolean isPestDetected; // Detección de plagas
    private Boolean isChemicalAromaDetected; // Detección de aroma químico/combustible
}
