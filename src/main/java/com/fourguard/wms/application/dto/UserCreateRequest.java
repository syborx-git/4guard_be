package com.fourguard.wms.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fourguard.wms.domain.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCreateRequest {
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    String password; // This will be hashed in the service layer

    @NotBlank(message = "First name cannot be blank")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    String firstName;

    @NotBlank(message = "Last name cannot be blank")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    String lastName;

    @NotNull(message = "Organization ID cannot be null")
    UUID organizationId;

    @NotNull(message = "Branch ID cannot be null")
    UUID branchId;

    @NotNull(message = "Role ID cannot be null")
    UUID roleId;

    UserStatus status; // Optional, default to PENDING or ACTIVE in service
    Boolean isEnabled; // Optional, default to true in service
}