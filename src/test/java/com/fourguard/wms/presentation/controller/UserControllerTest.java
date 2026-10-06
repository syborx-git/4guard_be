package com.fourguard.wms.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourguard.wms.application.dto.UserCreateRequest;
import com.fourguard.wms.application.dto.UserResponse;
import com.fourguard.wms.application.dto.UserUpdateRequest;
import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.domain.ports.in.CreateUserUseCase;
import com.fourguard.wms.domain.ports.in.DeleteUserUseCase;
import com.fourguard.wms.domain.ports.in.GetUserUseCase;
import com.fourguard.wms.domain.ports.in.UpdateUserUseCase;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.application.dto.request.auth.ChangePasswordRequest;
import com.fourguard.wms.presentation.advice.GlobalExceptionHandler;
import com.fourguard.wms.presentation.advice.DomainExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock private CreateUserUseCase createUserUseCase;
    @Mock private GetUserUseCase getUserUseCase;
    @Mock private UpdateUserUseCase updateUserUseCase;
    @Mock private DeleteUserUseCase deleteUserUseCase;
    @Mock private com.fourguard.wms.domain.ports.in.ResetUserPasswordUseCase resetUserPasswordUseCase;
    @Mock private com.fourguard.wms.domain.ports.in.ChangeTemporaryPasswordUseCase changeTemporaryPasswordUseCase;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler(), new DomainExceptionHandler())
                .build();

        userId = UUID.randomUUID();
        userResponse = UserResponse.builder()
                .id(userId)
                .username("john.doe")
                .email("john.doe@4guard.com")
                .firstName("John")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .isEnabled(true)
                .build();
    }

    @Test
    void whenCreateUser_thenReturn200() throws Exception {
        // Arrange
        UserCreateRequest request = UserCreateRequest.builder()
                .username("john.doe")
                .email("john.doe@4guard.com")
                .password("securePassword123")
                .firstName("John")
                .lastName("Doe")
                .organizationId(UUID.randomUUID())
                .branchId(UUID.randomUUID())
                .roleId(UUID.randomUUID())
                .build();

        when(createUserUseCase.createUser(any(UserCreateRequest.class))).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario creado con éxito"))
                .andExpect(jsonPath("$.data.username").value("john.doe"));
    }

    @Test
    void whenCreateUser_withInvalidData_thenReturn400() throws Exception {
        // Arrange
        UserCreateRequest request = UserCreateRequest.builder()
                .username("") // Blank username (invalid)
                .email("invalid-email") // Invalid email format
                .password("short") // Password too short
                .build();

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos de entrada"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void whenGetAllUsers_thenReturnList() throws Exception {
        // Arrange
        when(getUserUseCase.getAllUsers()).thenReturn(List.of(userResponse));

        // Act & Assert
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lista de usuarios recuperada con éxito"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].username").value("john.doe"));
    }

    @Test
    void whenGetUserById_withExistingId_thenReturnUser() throws Exception {
        // Arrange
        when(getUserUseCase.getUserById(userId)).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario encontrado con éxito"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()));
    }

    @Test
    void whenGetUserById_withNonExistingId_thenReturn404() throws Exception {
        // Arrange
        when(getUserUseCase.getUserById(userId)).thenThrow(new EntityNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void whenUpdateUser_thenReturn200() throws Exception {
        // Arrange
        UserUpdateRequest request = UserUpdateRequest.builder()
                .id(userId)
                .username("john.updated")
                .email("john.updated@4guard.com")
                .build();

        UserResponse updatedResponse = UserResponse.builder()
                .id(userId)
                .username("john.updated")
                .email("john.updated@4guard.com")
                .build();

        when(updateUserUseCase.updateUser(any(UserUpdateRequest.class))).thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario actualizado con éxito"))
                .andExpect(jsonPath("$.data.username").value("john.updated"));
    }

    @Test
    public void whenDeleteUser_thenReturn200() throws Exception {
        // Arrange
        doNothing().when(deleteUserUseCase).deleteUser(userId);

        // Act & Assert
        mockMvc.perform(delete("/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario eliminado con éxito"));
    }

    @Test
    void whenResetToTemp_byUsername_thenReturn200() throws Exception {
        // Arrange — endpoint público, sin autenticación
        when(resetUserPasswordUseCase.resetToTemporaryPassword("john.doe", "SYSTEM")).thenReturn("4G-temp123*");

        // Act & Assert
        mockMvc.perform(put("/users/reset-password-temp")
                        .param("usernameOrEmail", "john.doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contraseña temporal generada con éxito"))
                .andExpect(jsonPath("$.data").value("4G-temp123*"));
    }

    @Test
    void whenResetToTemp_byEmail_thenReturn200() throws Exception {
        // Arrange — endpoint público, sin autenticación
        when(resetUserPasswordUseCase.resetToTemporaryPassword("john.doe@4guard.com", "SYSTEM")).thenReturn("4G-abcd1234*");

        // Act & Assert
        mockMvc.perform(put("/users/reset-password-temp")
                        .param("usernameOrEmail", "john.doe@4guard.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("4G-abcd1234*"));
    }

    @Test
    void whenResetToTemp_withNotExistingUser_thenReturn404() throws Exception {
        // Arrange
        when(resetUserPasswordUseCase.resetToTemporaryPassword("noexiste", "SYSTEM"))
                .thenThrow(new EntityNotFoundException("No se encontró ningún usuario con el nombre de usuario o correo electrónico: 'noexiste'"));

        // Act & Assert
        mockMvc.perform(put("/users/reset-password-temp")
                        .param("usernameOrEmail", "noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No se encontró ningún usuario con el nombre de usuario o correo electrónico: 'noexiste'"));
    }

    @Test
    void whenChangePassword_thenReturn200() throws Exception {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .newPassword("newSecurePass123")
                .build();
        doNothing().when(changeTemporaryPasswordUseCase).changePassword("operator", "newSecurePass123");

        // Act & Assert
        mockMvc.perform(put("/users/change-password")
                        .principal(() -> "operator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contraseña actualizada con éxito"));
    }

    @Test
    void whenChangePassword_withInvalidData_thenReturn400() throws Exception {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .newPassword("short") // Invalid password (short)
                .build();

        // Act & Assert
        mockMvc.perform(put("/users/change-password")
                        .principal(() -> "operator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos de entrada"));
    }
}
