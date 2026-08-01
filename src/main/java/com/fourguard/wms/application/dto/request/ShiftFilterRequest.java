package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.ShiftScopeType;
import com.fourguard.wms.domain.enums.ShiftStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftFilterRequest {

    private UUID branchId;
    private UUID warehouseSectionId;
    private ShiftStatus status;
    private ShiftScopeType scopeType;
    private String dayOfWeek;
    private String search;
}
