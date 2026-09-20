package com.fourguard.wms.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/**
 * Request DTO for updating an existing Supplier. Same structure as create, plus
 * id validation.
 */
@Value
@Builder
public class UpdateSupplierRequest {

    @NotBlank(message = "La razón social fiscal es requerida")
    @Size(min = 3, max = 250)
    @Schema(description = "Razón social fiscal del proveedor")
    String legalName;

    @Size(max = 150)
    @Schema(description = "Nombre comercial o marca del proveedor")
    String commercialName;

    @NotBlank(message = "El RFC / Tax ID es requerido")
    @Size(min = 3, max = 20)
    @Schema(description = "RFC (MX) o Tax ID del proveedor")
    String taxId;

    @NotBlank(message = "El tipo de proveedor es requerido")
    @Schema(description = "Código de tipo (FK → cat_supplier_types.code)", example = "PACKAGING")
    String type;

    @Schema(description = "¿Es proveedor preferente?")
    Boolean preferred;

    @NotBlank(message = "El scope es requerido")
    @Schema(description = "Alcance 3PL: GLOBAL | CLIENT | WAREHOUSE")
    String scopeType;

    @Schema(description = "UUID o Código del cliente (requerido si scopeType=CLIENT)")
    String clientId;

    @Schema(description = "Nombre del cliente (opcional, usado para auto-creación en pruebas/demos)")
    String clientName;

    /** Maps to branch_id in BD. Named warehouseId for FE compatibility. */
    @Schema(description = "UUID o Código del branch/almacén (requerido si scopeType=WAREHOUSE)")
    String warehouseId;

    @Schema(description = "Nombre del almacén/branch (opcional, usado para auto-creación en pruebas/demos)")
    String warehouseName;

    @Size(max = 2000)
    @Schema(description = "Notas operativas del proveedor")
    String notes;

    @Valid
    @Schema(description = "Datos del contacto principal del proveedor")
    SupplierContactRequest contact;

    @Valid
    @Schema(description = "Dirección del proveedor")
    SupplierAddressRequest address;

    @Valid
    @Schema(description = "Condiciones comerciales del proveedor")
    SupplierCommercialTermsRequest commercialTerms;

    @Schema(description = "Versión para optimistic locking")
    Long version;
}
