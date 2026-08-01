package com.fourguard.wms.application.dto.response;

import com.fourguard.wms.domain.enums.ShiftScopeType;
import com.fourguard.wms.domain.enums.ShiftStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class ShiftResponse {
    private final UUID id;
    private final String code;
    private final String name;
    private final String description;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer restBreakMinutes;
    private final Integer toleranceMinutes;
    private final Boolean isOvernight;
    private final Integer netDurationMinutes;
    private final ShiftStatus status;
    private final ShiftScopeType scopeType;
    private final UUID branchId;
    private final String branchName;
    private final UUID warehouseSectionId;
    private final String warehouseSectionName;
    private final Set<String> operatingDays;
    private final Boolean isDeleted;
    private final Long version;
    private final OffsetDateTime createdAt;
    private final String createdBy;
    private final OffsetDateTime updatedAt;
    private final String updatedBy;
}
