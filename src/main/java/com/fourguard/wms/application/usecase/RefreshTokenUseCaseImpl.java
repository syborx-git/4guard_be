package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.auth.RefreshTokenRequest;
import com.fourguard.wms.application.dto.response.auth.AuthResponse;
import com.fourguard.wms.application.dto.response.auth.UserInfoResponse;
import com.fourguard.wms.application.mapper.UserMapper;
import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.domain.exception.InvalidCredentialsException;
import com.fourguard.wms.domain.ports.in.RefreshTokenUseCase;
import com.fourguard.wms.domain.ports.out.ForkliftOperatorRepositoryPort;
import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ForkliftOperatorEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.security.jwt.JwtProperties;
import com.fourguard.wms.infrastructure.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final ForkliftOperatorRepositoryPort forkliftOperatorRepositoryPort;
    private final TokenBlacklistPort tokenBlacklistPort;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        try {
            String username = jwtService.extractUsername(refreshToken);
            
            UserEntity user = userRepositoryPort.findByUsername(username)
                    .orElseThrow(() -> new InvalidCredentialsException("Token inválido"));

            if (!Boolean.TRUE.equals(user.getIsEnabled()) || user.getStatus() != UserStatus.ACTIVE) {
                throw new InvalidCredentialsException("La cuenta está desactivada o suspendida");
            }

            Date issuedAt = jwtService.extractIssuedAt(refreshToken);
            if (tokenBlacklistPort.isUserRevoked(user.getId(), issuedAt)) {
                throw new InvalidCredentialsException("La sesión ha sido revocada");
            }

            if (!jwtService.isTokenValid(refreshToken, user.getUsername())) {
                throw new InvalidCredentialsException("Token inválido o expirado");
            }

            // Generate new access token; preserve incoming refresh token only for CHAT_ONLY (strict 7-day ceiling)
            String newAccessToken = jwtService.generateAccessToken(user);
            String roleName = user.getRole() != null ? user.getRole().getName() : "";
            String newRefreshToken = "CHAT_ONLY".equalsIgnoreCase(roleName)
                    ? refreshToken
                    : jwtService.generateRefreshToken(user);

            LocalDateTime expiresAt = LocalDateTime.now().plusNanos(jwtProperties.getAccessTokenExpiration() * 1_000_000L);
            UserInfoResponse userInfo = userMapper.toUserInfoResponse(user);
            try {
                Optional<ForkliftOperatorEntity> opOpt = forkliftOperatorRepositoryPort.findByUserId(user.getId())
                        .or(() -> forkliftOperatorRepositoryPort.findByCodeIgnoreCase(user.getUsername()));
                if (opOpt.isPresent()) {
                    ForkliftOperatorEntity op = opOpt.get();
                    userInfo = userInfo.toBuilder()
                            .forkliftOperatorId(op.getId())
                            .employeeCode(op.getCode())
                            .build();
                }
            } catch (Exception ignored) {}

            log.info("[AUTH] Refresh token successful for: {}", user.getUsername());

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresAt(expiresAt)
                    .user(userInfo)
                    .build();

        } catch (InvalidCredentialsException ex) {
            log.warn("[AUTH] Refresh token rejected: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.warn("[AUTH] Refresh token validation failed: {}", ex.getMessage());
            throw new InvalidCredentialsException("Token de refresco inválido o expirado");
        }
    }
}
