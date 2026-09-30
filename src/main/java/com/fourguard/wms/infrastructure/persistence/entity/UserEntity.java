package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "wms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_user_email",    columnNames = "email")
        })
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class UserEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    @Column(name = "change_password_required", nullable = false)
    @Builder.Default
    private Boolean changePasswordRequired = true;

    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    // ── Login lockout ──────────────────────────────────────────────────────────

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "permanently_locked", nullable = false)
    @Builder.Default
    private Boolean permanentlyLocked = false;

    @PrePersist
    protected void applyInsertDefaults() {
        if (isEnabled == null) isEnabled = false;
        if (changePasswordRequired == null) changePasswordRequired = true;
        if (failedAttempts == null) failedAttempts = 0;
        if (permanentlyLocked == null) permanentlyLocked = false;
    }

    public String getFullName() {
        if (firstName == null && lastName == null) return username != null ? username : "";
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }
}
