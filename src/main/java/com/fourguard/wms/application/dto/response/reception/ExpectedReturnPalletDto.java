package com.fourguard.wms.application.dto.response.reception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedReturnPalletDto {
    private UUID itemId;
    private String palletCode;
    private String lotNumber;
    private UUID skuId;
    private String skuCode;
    private String productName;
    private BigDecimal pieces;
    private LocalDate expirationDate;
    private String palletType;
}
