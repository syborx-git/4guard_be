package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.response.BranchResponse;
import com.fourguard.wms.application.dto.response.audit.BranchAuditResponse;

import java.util.List;
import java.util.UUID;

public interface GetBranchUseCase {
    BranchResponse getBranchById(UUID id);
    List<BranchResponse> getBranchesByOrganizationId(UUID organizationId);
    List<BranchResponse> getAllBranches();
    List<BranchAuditResponse> getBranchAuditLogs(UUID id);
}
