package com.fourguard.wms.application.dto.response.quality.ai;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SamplingCalculationResponse {

    private String materialType;
    private String samplingStandardUnit;
    private Double requiredSampleQuantity;
    private String unitOfMeasure;
    private String samplingMethod; // e.g. "Calador en diagonal a 1-2 sacos por tarima" o "Aleatorio inicio/mitad/fin"
    private String requiredEquipment; // e.g. "Bolsa estéril, guantes anticorte, calador, formato F02"
    private String destinationTransport; // e.g. "Transportes Aguillón / Calmo vía Seguridad Patrimonial"
    private List<String> mandatoryInstructions;
    private String officialNormReference; // IT01-PO-GC-8.6-04 Rev. 02
}
