package com.fourguard.wms.infrastructure.adapter.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourguard.wms.domain.ports.out.AiLlmPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbound Adapter — Integración con motor LLM local (Ollama / vLLM).
 * Diseñado con timeout estricto y fallback de alta resiliencia.
 */
@Component
@Slf4j
public class LocalOllamaAiAdapter implements AiLlmPort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String ollamaBaseUrl;
    private final String modelName;
    private final boolean isEnabled;

    public LocalOllamaAiAdapter(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ai.ollama.model:qwen2.5:7b}") String modelName,
            @Value("${ai.ollama.enabled:true}") boolean isEnabled) {

        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.modelName = modelName;
        this.isEnabled = isEnabled;
    }

    @Override
    public String generateResponse(String systemPrompt, String userPrompt, List<String> contextChunks) {
        if (!isEnabled) {
            log.info("[LocalOllamaAiAdapter] Inferencia LLM deshabilitada por configuración. Usando motor de reglas.");
            return null;
        }

        try {
            String fullPrompt = buildFullPrompt(systemPrompt, userPrompt, contextChunks);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("prompt", fullPrompt);
            requestBody.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    ollamaBaseUrl + "/api/generate",
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("response")) {
                    return root.get("response").asText();
                }
            }
        } catch (Exception e) {
            log.warn("[LocalOllamaAiAdapter] No se pudo conectar con Ollama local ({}: {}). Activando fallback de reglas.", ollamaBaseUrl, e.getMessage());
        }

        return null;
    }

    @Override
    public boolean isAvailable() {
        if (!isEnabled) return false;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(ollamaBaseUrl + "/api/tags", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private String buildFullPrompt(String systemPrompt, String userPrompt, List<String> contextChunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("<|system|>\n").append(systemPrompt).append("\n\n");

        if (contextChunks != null && !contextChunks.isEmpty()) {
            sb.append("### CONTEXTO NORMATIVO OFICIAL 4GUARD:\n");
            for (String chunk : contextChunks) {
                sb.append("- ").append(chunk).append("\n");
            }
            sb.append("\n");
        }

        sb.append("<|user|>\n").append(userPrompt).append("\n\n<|assistant|>\n");
        return sb.toString();
    }
}
