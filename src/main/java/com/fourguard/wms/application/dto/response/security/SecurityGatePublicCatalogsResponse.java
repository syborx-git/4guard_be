package com.fourguard.wms.application.dto.response.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Catálogos dinámicos públicos para auto-registro de transportistas en caseta")
public class SecurityGatePublicCatalogsResponse {

    @Schema(description = "Lista de Clientes Depositantes / Owners activos")
    private List<CatalogItemDto> clients;

    @Schema(description = "Lista de Líneas Transportistas / Fleteras activas")
    private List<CatalogItemDto> carriers;

    @Schema(description = "Alias para Lista de Líneas Transportistas")
    public List<CatalogItemDto> getCarrierLines() {
        return carriers;
    }

    @Schema(description = "Lista de Tipos de Transporte permitidos en planta")
    private List<String> transportTypes;

    @Schema(description = "Lista de Medidas y Dimensiones de Unidad / Caja estándar")
    private List<String> boxDimensions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CatalogItemDto {
        private String id;
        private String code;
        private String name;
        private String tradeName;
    }
}
