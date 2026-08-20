package com.fourguard.wms.application.dto.request.reception;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for closing and completing a warehouse reception (F01).
 * Requires warehouse leader / supervisor authorization credentials.
 */
@Data
public class CompleteReceptionRequest {

    @NotBlank(message = "leaderUsername es obligatorio")
    private String leaderUsername;

    @NotBlank(message = "leaderPassword es obligatorio")
    private String leaderPassword;

    private String observations;
}
