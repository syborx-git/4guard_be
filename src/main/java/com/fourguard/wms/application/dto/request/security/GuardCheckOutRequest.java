package com.fourguard.wms.application.dto.request.security;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud de Check-Out y Salida de Planta por parte de Caseta de Seguridad")
public class GuardCheckOutRequest {

    @Schema(description = "Hora de salida registrada por caseta", example = "14:35:00")
    @JsonFormat(pattern = "HH:mm[:ss]")
    private LocalTime departureTime;

    @Schema(description = "Observaciones finales de salida registradas por el guardia")
    private String exitObservations;

    @Schema(description = "Sellos / Cinchos de salida colocados o verificados")
    private List<String> exitSealNumbers;

    @Schema(description = "Notas internas adicionales del guardia")
    private String guardNotes;
}
