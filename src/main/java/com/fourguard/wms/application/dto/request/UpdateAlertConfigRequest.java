package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAlertConfigRequest {

    @NotNull(message = "El ID de la regla es obligatorio")
    private UUID id;

    @NotBlank(message = "El nombre de la regla es obligatorio")
    private String name;

    @NotNull(message = "La categoría es obligatoria")
    private AlertCategory category;

    @NotNull(message = "El evento disparador es obligatorio")
    private AlertEvent event;

    @NotNull(message = "La prioridad es obligatoria")
    private AlertPriority priority;

    private AlertStatus status;

    @NotEmpty(message = "Debe especificar al menos un canal de notificación")
    private List<String> channels;

    @NotEmpty(message = "Debe especificar al menos un destinatario")
    private List<String> recipients;

    @NotNull(message = "La condición es obligatoria")
    private AlertCondition condition;

    @NotNull(message = "El valor límite es obligatorio")
    @DecimalMin(value = "0.01", message = "El valor límite debe ser positivo y mayor a 0")
    private BigDecimal value;

    @NotNull(message = "La unidad es obligatoria")
    private AlertUnit unit;

    private AlertRecurrence recurrence;

    private AlertEscalation escalation;

    @NotBlank(message = "La plantilla de mensaje es obligatoria")
    private String messageTemplate;

    private String description;
}
