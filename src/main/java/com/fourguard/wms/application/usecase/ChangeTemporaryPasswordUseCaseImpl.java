package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.in.ChangeTemporaryPasswordUseCase;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeTemporaryPasswordUseCaseImpl implements ChangeTemporaryPasswordUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final com.fourguard.wms.shared.audit.AuditService auditService;

    @Override
    @Transactional
    public void changePassword(String username, String newPassword) {
        log.info("[AUTH] User '{}' requested password update to permanent", username);

        UserEntity userEntity = userRepositoryPort.findByUsername(username)
                .or(() -> userRepositoryPort.findByUsernameOrEmail(username))
                .orElseThrow(() -> new EntityNotFoundException("User not found with identifier: " + username));

        userEntity.setPassword(passwordEncoder.encode(newPassword));
        userEntity.setChangePasswordRequired(false);
        userEntity.setUpdatedAt(OffsetDateTime.now());
        userEntity.setUpdatedBy(username);

        userRepositoryPort.save(userEntity);

        auditService.log(userEntity, "CHANGE_PASSWORD", "USER", userEntity.getId(), null, java.util.Map.of("username", username));

        log.info("[AUDIT] Temporary password successfully changed to permanent for user: {}", username);
    }
}
