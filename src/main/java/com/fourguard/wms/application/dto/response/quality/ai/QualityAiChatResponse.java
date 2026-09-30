package com.fourguard.wms.application.dto.response.quality.ai;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityAiChatResponse {

    private String response;
    private List<String> citedNorms; // ej. IT01-PO-GC-8.6-01, IT02-PO-GC-8.6-02, F01-PO-GC-8.6-03
    private String recommendedSeverity; // CRITICAL, WARNING, INFO, NONE
    private String recommendedDestination; // DISTRIBUTION, DESTRUCTION, RETURN, NONE
    private Boolean isRulesEngineResolution; // True si se resolvió en memoria en 0ms
    private String modelUsed; // e.g. "4guard-rules-engine" or "ollama/qwen2.5"
    private Long latencyMs;
    private OffsetDateTime timestamp;
}
