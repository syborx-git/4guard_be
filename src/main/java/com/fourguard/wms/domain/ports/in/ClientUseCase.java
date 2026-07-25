package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreateClientRequest;
import com.fourguard.wms.application.dto.request.UpdateClientRequest;
import com.fourguard.wms.application.dto.response.ClientResponse;
import com.fourguard.wms.application.dto.response.audit.ClientAuditResponse;

import java.util.List;
import java.util.UUID;

public interface ClientUseCase {
    ClientResponse createClient(CreateClientRequest request);
    ClientResponse updateClient(UpdateClientRequest request);
    ClientResponse getClientById(UUID id);
    List<ClientResponse> getClientsByOrganizationId(UUID organizationId);
    List<ClientResponse> getAllClients();
    List<ClientAuditResponse> getClientAuditLogs(UUID id);
    void deleteClient(UUID id);
}
