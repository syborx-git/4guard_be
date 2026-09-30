package com.fourguard.wms.application.dto.request.quality;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class VerificationSignaturesDto {
    private SignatureItemDto elaboratedBy;
    private SignatureItemDto reviewedBy;
    private SignatureItemDto approvedBy;
    private SignatureItemDto cleaningResponsible;
    private SignatureItemDto releaseResponsible;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SignatureItemDto {
        private String name;
        private String position;
        private String signedAt;
        private Boolean isSigned;
    }
}
