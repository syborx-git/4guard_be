package com.fourguard.wms.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fourguard.wms.domain.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserUpdateRequest {
    @NotNull(message = "User ID cannot be null for update")
    UUID id;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username;

    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    String email;

    @Size(min = 8, message = "Password must be at least 8 characters long")
    String password; // This will be hashed in the service layer if provided

    @Size(max = 50, message = "First name cannot exceed 50 characters")
    String firstName;

    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    String lastName;

    UUID organizationId;
    UUID branchId;
    UUID roleId;
    UserStatus status;
    Boolean isEnabled;
}