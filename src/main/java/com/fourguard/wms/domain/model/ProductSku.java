package com.fourguard.wms.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProductSku {
    private UUID id;
    private Client client;
    private String code;
    private String name;
    private String description;
    private BigDecimal weight;
    private String unit;
    private String status;
    private Boolean isDeleted;
    private Long version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
