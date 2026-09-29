package com.fourguard.wms.application.dto.request.quality;

import com.fourguard.wms.domain.enums.ReleaseAuthorizerType;
import com.fourguard.wms.domain.enums.ReleaseDestination;
import com.fourguard.wms.domain.enums.ReleaseSupportType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateQualityReleaseRequest {

    @NotNull(message = "El ID del bloqueo es obligatorio")
    private UUID blockId;

    @NotNull(message = "El tipo de autorizador es obligatorio")
    private ReleaseAuthorizerType authorizerType;

    @NotNull(message = "El tipo de soporte documental es obligatorio")
    private ReleaseSupportType supportType;

    private String supportCustomType;

    @NotBlank(message = "El asunto o referencia de soporte es obligatorio")
    @Size(max = 255)
    private String supportSubject;

    private String supportFileName;

    @NotBlank(message = "El nombre de quien autoriza es obligatorio")
    @Size(max = 150)
    private String authorizedByName;

    @NotBlank(message = "El puesto del autorizador es obligatorio")
    @Size(max = 150)
    private String authorizedByPosition;

    @NotNull(message = "El destino final es obligatorio (DISTRIBUTION, DESTRUCTION, RETURN)")
    private ReleaseDestination destination;

    @NotBlank(message = "El dictamen / notas de decisión son obligatorios")
    @Size(min = 10, max = 2000)
    private String decisionNotes;

    private List<EvidenceFileDto> evidenceFiles;
}
