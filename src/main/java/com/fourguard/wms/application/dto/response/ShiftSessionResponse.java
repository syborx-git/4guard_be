package com.fourguard.wms.application.dto.response;

import com.fourguard.wms.domain.enums.ShiftSessionSource;
import com.fourguard.wms.domain.enums.ShiftSessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class ShiftSessionResponse {
    private final UUID id;
    private final UUID organizationId;
    private final UUID branchId;
    private final String branchName;
    private final UUID shiftId;
    private final String shiftName;
    private final LocalDate businessDate;
    private final OffsetDateTime startedAt;
    private final OffsetDateTime endedAt;
    private final ShiftSessionStatus status;
    private final ShiftSessionSource source;
    private final UUID openedBy;
    private final UUID closedBy;
}
