package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.security.DriverCheckinSubmissionRequest;
import com.fourguard.wms.application.dto.request.security.GeneratePassRequest;
import com.fourguard.wms.application.dto.request.security.GuardCheckinCompletionRequest;
import com.fourguard.wms.application.dto.response.security.PassResponse;

import com.fourguard.wms.application.dto.request.security.GuardCheckOutRequest;

import com.fourguard.wms.application.dto.response.security.SecurityGatePublicCatalogsResponse;

import java.util.List;
import java.util.UUID;

public interface SecurityGateUseCase {

    SecurityGatePublicCatalogsResponse getPublicCatalogs(UUID organizationId);

    PassResponse generatePass(GeneratePassRequest request);

    PassResponse getPassByToken(String token);

    PassResponse submitDriverCheckin(String token, DriverCheckinSubmissionRequest request);

    List<PassResponse> getActivePasses(UUID organizationId, UUID branchId);

    List<PassResponse> getInYardPasses(UUID organizationId, UUID branchId);

    List<PassResponse> getHistoryPasses(UUID organizationId, UUID branchId, String search);

    PassResponse completeCheckin(String token, GuardCheckinCompletionRequest request);

    PassResponse checkOut(String tokenOrFolio, GuardCheckOutRequest request);

    void cancelPass(UUID passId);
}
