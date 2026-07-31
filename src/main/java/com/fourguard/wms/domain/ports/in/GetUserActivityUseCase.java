package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.response.audit.UserActivityAuditResponse;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Input Port — Use Case for fetching user activity audit logs. */
public interface GetUserActivityUseCase {
    List<UserActivityAuditResponse> getUserActivityLogs(
            UUID userId,
            String action,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            Principal principal);
}
