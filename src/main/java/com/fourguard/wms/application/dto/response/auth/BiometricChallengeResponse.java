package com.fourguard.wms.application.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricChallengeResponse {
    private String challenge;
    private Long timeout;
    private String rpId;
}

