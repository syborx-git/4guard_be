package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.quality.ai.*;
import com.fourguard.wms.application.dto.response.quality.ai.*;
import com.fourguard.wms.domain.ai.quality.QualityRulesEngine;
import com.fourguard.wms.domain.ports.in.QualityAiUseCase;
import com.fourguard.wms.domain.ports.out.AiLlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Application Service — Implementación de los Casos de Uso de IA para Calidad QM.
 * Aplica estrategia híbrida: Evaluación en memoria ultra-rápida (&lt; 1ms) + Asistente LLM local con contexto normativo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QualityAiService implements QualityAiUseCase {

    private final QualityRulesEngine rulesEngine;
    private final AiLlmPort aiLlmPort;

    private static final String SYSTEM_PROMPT = """
            Eres el Asistente Técnico y Normativo de Control de Calidad e Inocuidad (QM) de 4GUARD WMS.
            Tu objetivo es asesorar a los auditores, inspectores de calidad y supervisores de almacén con estricta adherencia a los procedimientos oficiales.
            Reglas inviolables:
            1. Cita siempre los códigos oficiales: IT01-PO-GC-8.6-01 (Aceptación/Rechazo), IT02-PO-GC-8.6-02 (Acondicionamiento/Traspaleo), F01-PO-GC-8.6-03 Rev. 03 (Verificación de Carga), IT01-PO-GC-8.6-02 (Almacenamiento), IT01-PO-GC-8.6-04 (Muestreo) y PO-GC-7.1.4-12 (Plagas).
            2. Nunca autorices el uso de cinta Diurex en Producto Terminado (PT). Solo se permite en frascos de vidrio vacíos > 5 cm.
            3. Tolerancia máxima de inclinación de tarima: 5.0°.
            4. Humedad relativa máxima permitida en almacén: 65% HR.
            5. En traspaleo: 3 vueltas de playo para Producto Terminado, 5 vueltas para frascos.
            6. Respuestas concisas, técnicas, profesionales y orientadas a la acción.
            """;

    @Override
    public QualityAiEvaluationResponse evaluateRule(QualityAiRuleEvaluationRequest request) {
        log.debug("[QualityAiService] Evaluando reglas físicas de inspección para SKU: {}", request.getSku());
        return rulesEngine.evaluatePhysicalInspection(request);
    }

    @Override
    public QualityAiChatResponse askAssistant(UUID organizationId, UUID branchId, UUID userId, QualityAiChatRequest request) {
        long startTime = System.currentTimeMillis();
        String message = request.getMessage() != null ? request.getMessage().toLowerCase(Locale.ROOT) : "";

        List<String> citedNorms = new ArrayList<>();
        String recommendedSeverity = "INFO";
        String recommendedDestination = "NONE";
        boolean isRulesEngineMatch = false;
        String answer;
        String modelUsed = "4guard-rules-engine";

        // ── 1. Match Rápido en Motor de Reglas Deterministas (< 1ms) ───────────
        if (message.contains("diurex") || message.contains("cinta")) {
            citedNorms.add("IT02-PO-GC-8.6-02");
            recommendedSeverity = "WARNING";
            isRulesEngineMatch = true;
            answer = "Está estrictamente **PROHIBIDO** utilizar cinta Diurex en Producto Terminado (PT) conforme a **IT02-PO-GC-8.6-02 (punto 2.4)**, ya que daña el empaque comercial y las etiquetas. El Diurex solo está autorizado en pallets de frascos de vidrio vacíos con aberturas mayores a 5 cm. Para producto terminado debes aplicar emplayado con film plástico (3 vueltas en base, 70% altura y 3 en corona).";
        } else if (message.contains("inclinac") || message.contains("ladead") || message.contains("grados")) {
            citedNorms.add("IT01-PO-GC-8.6-01");
            citedNorms.add("IT02-PO-GC-8.6-02");
            recommendedSeverity = "WARNING";
            isRulesEngineMatch = true;
            answer = "Conforme a la Tabla 1 de **IT01-PO-GC-8.6-01**, la tolerancia máxima de inclinación permitida es de **5.0°**. Todo pallet con inclinación superior se considera inestable y con riesgo de colapso, por lo que debe ser bloqueado para acondicionamiento y re-emplayado inmediato bajo **IT02-PO-GC-8.6-02** antes de autorizar su almacenamiento o embarque.";
        } else if (message.contains("humedad") || message.contains("termohigr")) {
            citedNorms.add("IT01-PO-GC-8.6-02");
            recommendedSeverity = "WARNING";
            isRulesEngineMatch = true;
            answer = "El límite crítico de humedad relativa es de **65% HR** conforme al instructivo **IT01-PO-GC-8.6-02**. Los horarios oficiales de lectura de termohigrómetros son: Turno 1 (08:00 a 09:00 hrs), Turno Intermedio (14:00 hrs) y Turno 2 (20:00 a 21:00 hrs). Si la lectura excede el 65% HR, se debe reportar de inmediato al Gerente de Calidad en el canal 'Incidencias de calidad'.";
        } else if (message.contains("plaga") || message.contains("insect") || message.contains("rat") || message.contains("excreta")) {
            citedNorms.add("PO-GC-7.1.4-12");
            citedNorms.add("IT01-PO-GC-8.6-01");
            recommendedSeverity = "CRITICAL";
            isRulesEngineMatch = true;
            answer = "Existe **tolerancia CERO** a la presencia o indicios de plagas conforme a **IT01-PO-GC-8.6-01** y **PO-GC-7.1.4-12**. Acción inmediata: Bloqueo de Producto No Conforme (PNC) con severidad **CRITICAL (Rojo)**, inmovilización en WMS, separación de lote y reporte formal a Dirección General y Cliente.";
        } else if (message.contains("playo") || message.contains("vueltas") || message.contains("traspaleo")) {
            citedNorms.add("IT02-PO-GC-8.6-02");
            recommendedSeverity = "INFO";
            isRulesEngineMatch = true;
            answer = "En maniobras de acondicionamiento y traspaleo bajo **IT02-PO-GC-8.6-02**: Para **Producto Terminado (PT)** se aplican mínimo **3 vueltas de playo**; para **Frascos de vidrio vacíos** se aplican mínimo **5 vueltas de playo**. El traspaleo de frascos debe realizarse a un máximo de 3 piezas por mano con guantes limpios.";
        } else if (message.contains("tabla") || message.contains("tacon") || message.contains("tarima rota")) {
            citedNorms.add("IT02-PO-GC-8.6-02");
            recommendedSeverity = "WARNING";
            isRulesEngineMatch = true;
            answer = "Bajo **IT02-PO-GC-8.6-02 (punto 2.5)**: Si a la tarima le falta únicamente **1 tabla o 1 tacón**, se permite su reparación in situ colocando el repuesto con martillo y clavos con apoyo del montacarguista. Si presenta **2 o más tablas o tacones dañados**, está estrictamente prohibido repararla y se debe ejecutar un **traspaleo completo** a una tarima nueva.";
        } else {
            // ── 2. Consulta al Motor LLM Local (con contexto inyectado) ─────────
            List<String> context = List.of(
                    "IT01-PO-GC-8.6-01: Criterios de Aceptación y Rechazo en Inspección Visual.",
                    "IT02-PO-GC-8.6-02: Acondicionamiento y Traspaleo de Material (Prohibido Diurex en PT, vueltas de playo: 3 PT / 5 frascos).",
                    "F01-PO-GC-8.6-03 Rev. 03: Verificación Oficial de Carga y 18 criterios normativos.",
                    "IT01-PO-GC-8.6-02: Condiciones de Almacenamiento y Humedad máx 65% HR.",
                    "IT01-PO-GC-8.6-04 Rev. 02: Muestreo de Materiales y Café Verde (3.0 kg en diagonal con calador).",
                    "PO-GC-7.1.4-12: Manejo Integrado de Plagas y NOM-256-SSA1-2012."
            );

            String llmResponse = aiLlmPort.generateResponse(SYSTEM_PROMPT, request.getMessage(), context);
            if (llmResponse != null && !llmResponse.isBlank()) {
                answer = llmResponse;
                modelUsed = "ollama/qwen2.5";
                citedNorms.add("4GUARD-SGC-NORMAS");
            } else {
                answer = "Para atender tu consulta sobre calidad operativa: Por favor verifica si tu caso corresponde a inspección en descarga (IT01-PO-GC-8.6-01), acondicionamiento o emplayado (IT02-PO-GC-8.6-02), o verificación de carga en andén (F01-PO-GC-8.6-03 Rev. 03). También puedes usar el endpoint de evaluación física de reglas.";
                citedNorms.add("IT01-PO-GC-8.6-01");
            }
        }

        long latency = System.currentTimeMillis() - startTime;

        return QualityAiChatResponse.builder()
                .response(answer)
                .citedNorms(citedNorms)
                .recommendedSeverity(recommendedSeverity)
                .recommendedDestination(recommendedDestination)
                .isRulesEngineResolution(isRulesEngineMatch)
                .modelUsed(modelUsed)
                .latencyMs(latency)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    @Override
    public SamplingCalculationResponse calculateSampling(SamplingCalculationRequest request) {
        log.debug("[QualityAiService] Calculando plan de muestreo para material: {}", request.getMaterialType());
        return rulesEngine.calculateSampling(request);
    }
}
