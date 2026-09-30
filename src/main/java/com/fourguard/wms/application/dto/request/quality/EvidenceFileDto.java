package com.fourguard.wms.application.dto.request.quality;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EvidenceFileDto {
    private String id;
    private String name;
    private String size;
    private String type; // image, pdf, email
    private String url;
    private String uploadedAt;
}
