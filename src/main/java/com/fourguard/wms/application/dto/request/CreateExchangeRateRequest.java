package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.ExchangeRateSourceType;
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
public class CreateExchangeRateRequest {

    @NotNull(message = "El ID de la organización es obligatorio")
    private UUID organizationId;

    @NotNull(message = "La divisa de origen (fromCurrencyId) es obligatoria")
    private UUID fromCurrencyId;

    @NotNull(message = "La divisa de destino (toCurrencyId) es obligatoria")
    private UUID toCurrencyId;

    @NotNull(message = "La tasa de cambio (rate) es obligatoria")
    @DecimalMin(value = "0.000001", message = "La tasa debe ser mayor a 0")
    private BigDecimal rate;

    @NotNull(message = "La fecha de efectividad es obligatoria")
    private LocalDate effectiveDate;

    @Builder.Default
    private ExchangeRateSourceType sourceType = ExchangeRateSourceType.MANUAL;

    @Size(max = 255, message = "Las notas no pueden exceder 255 caracteres")
    private String notes;
}
