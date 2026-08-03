package com.fourguard.wms.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConvertCurrencyRequest {

    @NotNull(message = "El ID de la organización es obligatorio")
    private UUID organizationId;

    private UUID fromCurrencyId;
    private String fromCode;

    private UUID toCurrencyId;
    private String toCode;

    @NotNull(message = "El monto a convertir es obligatorio")
    @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
    private BigDecimal amount;

    private LocalDate date;
}
