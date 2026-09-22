package com.fourguard.wms.application.dto.response.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatBlockReasonResponse {
    private String code;
    private String description;
    private String category;
}
