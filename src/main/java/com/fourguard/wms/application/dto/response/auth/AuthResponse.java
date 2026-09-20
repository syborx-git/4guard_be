package com.fourguard.wms.application.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Response DTO for a successful login. */
@Getter
@Builder
public class AuthResponse {
    private final String accessToken;
    private final String refreshToken;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime expiresAt;

    private final UserInfoResponse user;
}
