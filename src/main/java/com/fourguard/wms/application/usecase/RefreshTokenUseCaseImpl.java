package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.auth.RefreshTokenRequest;
import com.fourguard.wms.application.dto.response.auth.AuthResponse;
import com.fourguard.wms.application.dto.response.auth.UserInfoResponse;
import com.fourguard.wms.application.mapper.UserMapper;
import com.fourguard.wms.domain.exception.InvalidCredentialsException;
import com.fourguard.wms.domain.ports.in.RefreshTokenUseCase;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.security.jwt.JwtProperties;
import com.fourguard.wms.infrastructure.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final UserRepositoryPort userRepositoryPort;
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

            if (!jwtService.isTokenValid(refreshToken, user.getUsername())) {
                throw new InvalidCredentialsException("Token inválido o expirado");
            }

            // Generate new access and refresh tokens
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            LocalDateTime expiresAt = LocalDateTime.now().plusNanos(jwtProperties.getAccessTokenExpiration() * 1_000_000L);
            UserInfoResponse userInfo = userMapper.toUserInfoResponse(user);

            log.info("[AUTH] Refresh token successful for: {}", user.getUsername());

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresAt(expiresAt)
                    .user(userInfo)
                    .build();

        } catch (Exception ex) {
            log.warn("[AUTH] Refresh token validation failed: {}", ex.getMessage());
            throw new InvalidCredentialsException("Token de refresco inválido o expirado");
        }
    }
}
