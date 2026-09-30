package com.fourguard.wms.application.dto.response.quality.ai;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityAiEvaluationResponse {

    private String verdict; // CONFORME, NO_CONFORME, ACONDICIONAMIENTO_REQUERIDO, BLOQUEO_PNC, ALERTA_AMBIENTAL
    private String recommendedAction;
    private String severity; // CRITICAL (RED), WARNING (YELLOW), INFO (BLUE), OK (GREEN)
    private List<String> violatedRules;
    private List<String> appliedInstructions; // IT01-PO-GC-8.6-01, IT02-PO-GC-8.6-02, IT01-PO-GC-8.6-02, etc.
    private Boolean requiresTraspaleo;
    private Integer recommendedPlayoWraps;
    private Boolean isDiurexAllowed;
    private Long evaluationLatencyMs; // Tiempo de respuesta en milisegundos (< 5ms)
}
