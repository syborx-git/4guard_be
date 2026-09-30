package com.fourguard.wms.application.dto.request.quality.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityAiChatRequest {

    @NotBlank(message = "El mensaje o pregunta no puede estar vacío")
    private String message;

    private String stage; // INBOUND_UNLOAD, STORAGE, OUTBOUND_LOAD, TEST_MATERIAL
    private UUID itemId;
    private String sku;
    private String batchNumber;
    private String conversationId;
}
