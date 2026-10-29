package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.auth.LoginRequest;
import com.fourguard.wms.application.dto.response.auth.AuthResponse;
import com.fourguard.wms.application.dto.response.auth.UserInfoResponse;
import com.fourguard.wms.application.mapper.UserMapper;
import com.fourguard.wms.domain.exception.InvalidCredentialsException;
import com.fourguard.wms.domain.ports.in.LoginUseCase;
import com.fourguard.wms.domain.ports.out.ForkliftOperatorRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ForkliftOperatorEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.security.jwt.JwtProperties;
import com.fourguard.wms.infrastructure.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final ForkliftOperatorRepositoryPort forkliftOperatorRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final com.fourguard.wms.shared.audit.AuditService auditService;
    private final LoginLockService loginLockService;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("[AUTH] Attempting login for: {}", request.getIdentifier());

        // 1. Find user by username or email
        UserEntity user = userRepositoryPort.findByUsernameOrEmail(request.getIdentifier())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales incorrectas"));

        // 2. Check lockout status BEFORE password validation
        //    Throws AccountTemporarilyLockedException or AccountPermanentlyLockedException if locked.
        loginLockService.checkLockStatus(user);

        // 3. Check if account is active/enabled
        if (!user.getIsEnabled() || !"ACTIVE".equals(user.getStatus().name())) {
            log.warn("[AUTH] Account disabled or pending: {}", user.getUsername());
            throw new InvalidCredentialsException("La cuenta está desactivada o pendiente de activación");
        }

        // 4. Validate password — register failure if wrong
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("[AUTH] Password mismatch for user: {}", user.getUsername());
            loginLockService.registerFailedAttempt(user);
            throw new InvalidCredentialsException("Credenciales incorrectas");
        }

        // 5. Login successful — reset failure counter
        loginLockService.resetOnSuccess(user);

        // 6. Update last login
        user.setLastLogin(OffsetDateTime.now(ZoneOffset.UTC));
        userRepositoryPort.save(user);

        // 7. Generate tokens
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 8. Expiration timestamp
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusNanos(jwtProperties.getAccessTokenExpiration() * 1_000_000L);

        // 9. Map user details to DTO and enrich with ForkliftOperator identity if linked
        UserInfoResponse userInfo = userMapper.toUserInfoResponse(user);
        try {
            Optional<ForkliftOperatorEntity> opOpt = forkliftOperatorRepositoryPort.findByUserId(user.getId())
                    .or(() -> forkliftOperatorRepositoryPort.findByCodeIgnoreCase(user.getUsername()));
            if (opOpt.isPresent()) {
                ForkliftOperatorEntity op = opOpt.get();
                if (op.getUser() == null) {
                    op.setUser(user);
                    forkliftOperatorRepositoryPort.save(op);
                }
                userInfo = userInfo.toBuilder()
                        .forkliftOperatorId(op.getId())
                        .employeeCode(op.getCode())
                        .build();
            }
        } catch (Exception ex) {
            log.debug("[AUTH] Could not resolve forklift operator identity for {}: {}", user.getUsername(), ex.getMessage());
        }

        log.info("[AUTH] Successful login for: {}", user.getUsername());

        auditService.log(user, "LOGIN", "USER", user.getId(), null,
                java.util.Map.of("username", user.getUsername(), "email", user.getEmail()));

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .user(userInfo)
                .build();
    }
}
