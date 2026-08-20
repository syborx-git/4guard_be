package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.ShiftScopeType;
import com.fourguard.wms.domain.enums.ShiftStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

@Value
@Builder
public class CreateShiftRequest {

    @NotBlank(message = "El código del turno es requerido")
    @Size(max = 30, message = "El código no puede superar 30 caracteres")
    String code;

    @NotBlank(message = "El nombre del turno es requerido")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String name;

    String description;

    @NotNull(message = "La hora de inicio es requerida")
    LocalTime startTime;

    @NotNull(message = "La hora de fin es requerida")
    LocalTime endTime;

    @Min(value = 0, message = "El descanso no puede ser negativo")
    Integer restBreakMinutes;

    @Min(value = 0, message = "La tolerancia no puede ser negativa")
    Integer toleranceMinutes;

    @NotNull(message = "El alcance (scopeType) es requerido")
    ShiftScopeType scopeType;

    ShiftStatus status;

    UUID branchId;

    UUID warehouseSectionId;

    @NotEmpty(message = "Debe especificar al menos un día operativo de la semana")
    Set<String> operatingDays;
}
