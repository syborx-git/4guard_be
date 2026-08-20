package com.fourguard.wms.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCurrencyRequest {

    @NotNull(message = "El ID de la organización es obligatorio")
    private UUID organizationId;

    @NotBlank(message = "El código ISO de la divisa es obligatorio")
    @Size(min = 3, max = 3, message = "El código ISO de la divisa debe tener exactamente 3 caracteres")
    private String code;

    @NotBlank(message = "El nombre de la divisa es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String name;

    @NotBlank(message = "El símbolo es obligatorio")
    @Size(max = 10, message = "El símbolo no puede exceder 10 caracteres")
    private String symbol;

    @Builder.Default
    private Boolean isBase = false;

    @Min(value = 0, message = "Los decimales no pueden ser negativos")
    @Max(value = 8, message = "Los decimales no pueden exceder 8")
    @Builder.Default
    private Integer decimalPlaces = 2;
}
