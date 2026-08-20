package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.common.PhysicalDestinationDto;
import com.fourguard.wms.application.dto.request.CreateClientRequest;
import com.fourguard.wms.application.dto.request.UpdateClientRequest;
import com.fourguard.wms.application.dto.response.ClientResponse;
import com.fourguard.wms.application.dto.response.audit.ClientAuditResponse;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada — Casos de Uso de Gestión de Clientes Depositantes 3PL. */
public interface ClientUseCase {

    // ── CRUD Principal ────────────────────────────────────────────────────────
    ClientResponse createClient(CreateClientRequest request);
    ClientResponse updateClient(UpdateClientRequest request);
    ClientResponse getClientById(UUID id);
    List<ClientResponse> getClientsByOrganizationId(UUID organizationId);
    List<ClientResponse> getAllClients();
    void deleteClient(UUID id);

    /** Alterna el estado ACTIVE ↔ INACTIVE de un cliente (baja lógica). */
    ClientResponse toggleClientStatus(UUID id);

    // ── Auditoría ─────────────────────────────────────────────────────────────
    List<ClientAuditResponse> getClientAuditLogs(UUID id);

    // ── Gestión Granular de Destinos Físicos (Ship-to Locations) ─────────────
    List<PhysicalDestinationDto> getClientDestinations(UUID clientId);
    PhysicalDestinationDto addDestination(UUID clientId, PhysicalDestinationDto destinationDto);
    PhysicalDestinationDto updateDestination(UUID clientId, UUID destinationId, PhysicalDestinationDto destinationDto);
    void deleteDestination(UUID clientId, UUID destinationId);
}
