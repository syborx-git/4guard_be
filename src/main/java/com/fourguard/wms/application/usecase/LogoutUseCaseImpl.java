package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.auth.LogoutRequest;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.InvalidCredentialsException;
import com.fourguard.wms.domain.ports.in.LogoutUseCase;
import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.security.jwt.JwtService;
import com.fourguard.wms.shared.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final TokenBlacklistPort tokenBlacklistPort;

    @Override
    @Transactional
    public void logout(LogoutRequest request, String currentUsername) {
        log.info("[AUTH] User '{}' requested secure logout (reason: {})", currentUsername, request != null ? request.getReason() : "UNSPECIFIED");

        String refreshToken = request != null ? request.getRefreshToken() : null;

        // Validate the refresh token structure and expiration
        try {
            String tokenUsername = jwtService.extractUsername(refreshToken);
            if (!tokenUsername.equals(currentUsername) || jwtService.isTokenExpired(refreshToken)) {
                log.warn("[AUTH] Refresh token mismatch or expired during logout for user: {}", currentUsername);
                throw new InvalidCredentialsException("Token de refresco inválido o expirado");
            }
        } catch (Exception ex) {
            log.warn("[AUTH] Failed to parse refresh token during logout for user: {}", currentUsername);
            throw new InvalidCredentialsException("Token de refresco inválido o expirado");
        }

        UserEntity userEntity = userRepositoryPort.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUsername));

        // Revoke all active session tokens in blacklist cache/storage
        tokenBlacklistPort.revokeUserSessions(userEntity.getId());

        // Determine specific audit action based on reason
        String reason = (request != null && request.getReason() != null) ? request.getReason().toUpperCase() : "MANUAL";
        String auditAction = "TIMEOUT".equals(reason) ? "SESSION_TIMEOUT_LOGOUT" : "LOGOUT";

        Map<String, Object> details = new HashMap<>();
        details.put("username", currentUsername);
        details.put("reason", reason);

        // Persist logout event to database audit log
        auditService.log(userEntity, auditAction, "USER", userEntity.getId(), null, details);

        log.info("[AUTH] User '{}' successfully logged out with action '{}' and sessions revoked", currentUsername, auditAction);
    }
}
