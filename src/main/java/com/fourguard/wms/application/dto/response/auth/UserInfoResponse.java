package com.fourguard.wms.application.dto.response.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/** User information returned inside the login response. */
@Getter
@Builder
public class UserInfoResponse {
    private final UUID   id;
    private final String username;
    private final String fullName;
    private final String email;
    private final String role;
    private final int    roleLevel;
    private final List<String> permissions;
    private final Boolean changePasswordRequired;
    private final UUID   organizationId;
    private final String organizationName;
    private final UUID   branchId;
    private final String branchName;
}

