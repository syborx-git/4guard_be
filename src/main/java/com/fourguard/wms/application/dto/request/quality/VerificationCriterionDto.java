package com.fourguard.wms.application.dto.request.quality;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class VerificationCriterionDto {
    private String id;
    private String label;
    private String sublabel;
    private String value; // SI, NO, NA
    private String actionIfNo;
    private String responsible;
    private String instructionCode;
    private String observations;
    private Boolean isCritical;
}
