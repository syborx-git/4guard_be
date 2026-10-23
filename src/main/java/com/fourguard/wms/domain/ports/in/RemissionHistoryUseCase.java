package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.response.history.RemissionHistoryPageResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface RemissionHistoryUseCase {

    RemissionHistoryPageResponse getRemissions(
            UUID organizationId,
            UUID branchId,
            OffsetDateTime from,
            OffsetDateTime to,
            UUID clientId,
            String status,
            String entityType,
            UUID shiftSessionId,
            String search,
            int page,
            int size);
}
