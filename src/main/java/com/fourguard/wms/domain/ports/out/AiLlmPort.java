package com.fourguard.wms.domain.ports.out;

import java.util.List;

/**
 * Outbound Port — Contrato de comunicación hacia el motor de LLM local o servicio de inferencia.
 */
public interface AiLlmPort {

    /**
     * Genera una respuesta enriquecida a partir del prompt y el contexto normativo inyectado.
     *
     * @param systemPrompt Instrucciones maestras de rol y guardrails.
     * @param userPrompt Consulta del usuario.
     * @param contextChunks Fragmentos normativos relevantes extraídos de la base de conocimiento.
     * @return Texto generado por el LLM.
     */
    String generateResponse(String systemPrompt, String userPrompt, List<String> contextChunks);

    /**
     * Indica si el motor de LLM local se encuentra activo y listo para responder.
     */
    boolean isAvailable();
}
