package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.quality.ai.*;
import com.fourguard.wms.application.dto.response.quality.ai.*;

import java.util.UUID;

/**
 * Inbound Port — Casos de uso de Inteligencia Artificial y Soporte a Decisiones para Calidad QM.
 */
public interface QualityAiUseCase {

    /**
     * Evalúa en &lt; 1ms las reglas físicas y tolerancias oficiales (IT01, IT02, etc.)
     */
    QualityAiEvaluationResponse evaluateRule(QualityAiRuleEvaluationRequest request);

    /**
     * Responde dudas operativas, normativas o redacta dictámenes técnicos mediante el Asistente IA.
     */
    QualityAiChatResponse askAssistant(UUID organizationId, UUID branchId, UUID userId, QualityAiChatRequest request);

    /**
     * Calcula la cantidad exacta de muestra y protocolo de extracción según IT01-PO-GC-8.6-04.
     */
    SamplingCalculationResponse calculateSampling(SamplingCalculationRequest request);
}
