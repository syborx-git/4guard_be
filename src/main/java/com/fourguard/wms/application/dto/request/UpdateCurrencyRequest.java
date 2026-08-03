package com.fourguard.wms.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCurrencyRequest {

    @NotBlank(message = "El nombre de la divisa es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String name;

    @NotBlank(message = "El símbolo es obligatorio")
    @Size(max = 10, message = "El símbolo no puede exceder 10 caracteres")
    private String symbol;

    @Min(value = 0, message = "Los decimales no pueden ser negativos")
    @Max(value = 8, message = "Los decimales no pueden exceder 8")
    private Integer decimalPlaces;
}
