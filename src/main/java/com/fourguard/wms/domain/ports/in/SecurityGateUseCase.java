package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.security.DriverCheckinSubmissionRequest;
import com.fourguard.wms.application.dto.request.security.GeneratePassRequest;
import com.fourguard.wms.application.dto.request.security.GuardCheckinCompletionRequest;
import com.fourguard.wms.application.dto.response.security.PassResponse;

import java.util.List;
import java.util.UUID;

public interface SecurityGateUseCase {

    PassResponse generatePass(GeneratePassRequest request);

    PassResponse getPassByToken(String token);

    PassResponse submitDriverCheckin(String token, DriverCheckinSubmissionRequest request);

    List<PassResponse> getActivePasses(UUID organizationId, UUID branchId);

    PassResponse completeCheckin(String token, GuardCheckinCompletionRequest request);
}
