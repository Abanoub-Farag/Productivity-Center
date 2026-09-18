package app.virtual_workspace.accounts.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import app.virtual_workspace.accounts.dtos.auth.AuthRequestDto;
import app.virtual_workspace.accounts.dtos.auth.AuthResponseDto;
import app.virtual_workspace.accounts.dtos.auth.CreateUserRequestDto;
import app.virtual_workspace.accounts.dtos.auth.LogoutRequestDto;
import app.virtual_workspace.accounts.dtos.data.UserDataDto;
import app.virtual_workspace.accounts.services.RefreshTokenService;
import app.virtual_workspace.accounts.services.UserAuthService;
import app.virtual_workspace.accounts.services.UserService;
import app.virtual_workspace.exceptions.custom.ResourceAlreadyExistsException;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.exceptions.custom.TokenRefreshException;
import app.virtual_workspace.shared.dtos.ApiResponse;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    @Mock
    private UserAuthService userAuthService;

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private AuthResponseDto sampleAuthResponse;

    @BeforeEach
    void setUp() {
        sampleAuthResponse = AuthResponseDto.builder()
                .jwtToken("mock_jwt_token")
                .refreshToken("mock_refresh_token")
                .build();
    }

    @Nested
    @DisplayName("register() tests")
    class RegisterTests {

        @Test
        @DisplayName("Should return 201 CREATED with AuthResponseDto when registration succeeds")
        void register_shouldReturnCreated_whenRequestIsValid() {
            CreateUserRequestDto request = CreateUserRequestDto.builder()
                    .email("newuser@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .password("Password123!")
                    .build();

            when(userAuthService.register(request)).thenReturn(sampleAuthResponse);

            ResponseEntity<ApiResponse<AuthResponseDto>> response = authenticationController.register(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CREATED.value());
            assertThat(response.getBody().getMessage()).isEqualTo("User registered successfully");
            assertThat(response.getBody().getData()).isEqualTo(sampleAuthResponse);
            assertThat(response.getBody().getData().getJwtToken()).isEqualTo("mock_jwt_token");
            assertThat(response.getBody().getData().getRefreshToken()).isEqualTo("mock_refresh_token");

            verify(userAuthService, times(1)).register(request);
        }

        @Test
        @DisplayName("Should propagate ResourceAlreadyExistsException when user email is duplicate")
        void register_shouldPropagateException_whenEmailAlreadyExists() {
            CreateUserRequestDto request = CreateUserRequestDto.builder()
                    .email("existing@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .password("Password123!")
                    .build();

            when(userAuthService.register(request))
                    .thenThrow(new ResourceAlreadyExistsException("User with email already exists"));

            assertThatThrownBy(() -> authenticationController.register(request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessage("User with email already exists");

            verify(userAuthService, times(1)).register(request);
        }

        @Test
        @DisplayName("Should propagate downstream RuntimeException during registration")
        void register_shouldPropagateGenericException_whenDownstreamFails() {
            CreateUserRequestDto request = CreateUserRequestDto.builder().build();
            when(userAuthService.register(any())).thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> authenticationController.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    @Nested
    @DisplayName("login() tests")
    class LoginTests {

        @Test
        @DisplayName("Should return 200 OK with AuthResponseDto when credentials are valid")
        void login_shouldReturnOk_whenCredentialsAreValid() {
            AuthRequestDto authRequest = AuthRequestDto.builder()
                    .email("test@example.com")
                    .password("Password123!")
                    .build();

            when(userAuthService.login(authRequest)).thenReturn(sampleAuthResponse);

            ResponseEntity<ApiResponse<AuthResponseDto>> response = authenticationController.login(authRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("User logged in successfully");
            assertThat(response.getBody().getData()).isEqualTo(sampleAuthResponse);

            verify(userAuthService, times(1)).login(authRequest);
        }

        @Test
        @DisplayName("Should propagate BadCredentialsException when password is incorrect")
        void login_shouldPropagateException_whenBadCredentials() {
            AuthRequestDto authRequest = AuthRequestDto.builder()
                    .email("test@example.com")
                    .password("WrongPassword")
                    .build();

            when(userAuthService.login(authRequest))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authenticationController.login(authRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Bad credentials");

            verify(userAuthService, times(1)).login(authRequest);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when user is not found")
        void login_shouldPropagateException_whenUserNotFound() {
            AuthRequestDto authRequest = AuthRequestDto.builder()
                    .email("nonexistent@example.com")
                    .password("Password123!")
                    .build();

            when(userAuthService.login(authRequest))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            assertThatThrownBy(() -> authenticationController.login(authRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(userAuthService, times(1)).login(authRequest);
        }
    }

    @Nested
    @DisplayName("refreshToken() tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should return 200 OK with refreshed AuthResponseDto when payload is valid")
        void refreshToken_shouldReturnOk_whenTokenIsValid() {
            Map<String, String> payload = Map.of("refreshToken", "valid_refresh_token");

            when(refreshTokenService.refreshToken(payload)).thenReturn(sampleAuthResponse);

            ResponseEntity<ApiResponse<AuthResponseDto>> response = authenticationController.refreshToken(payload);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Token refreshed successfully");
            assertThat(response.getBody().getData()).isEqualTo(sampleAuthResponse);

            verify(refreshTokenService, times(1)).refreshToken(payload);
        }

        @Test
        @DisplayName("Should propagate TokenRefreshException when token is expired or revoked")
        void refreshToken_shouldPropagateException_whenTokenIsExpired() {
            Map<String, String> payload = Map.of("refreshToken", "expired_token");

            when(refreshTokenService.refreshToken(payload))
                    .thenThrow(new TokenRefreshException("Refresh token is expired"));

            assertThatThrownBy(() -> authenticationController.refreshToken(payload))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessage("Refresh token is expired");

            verify(refreshTokenService, times(1)).refreshToken(payload);
        }

        @Test
        @DisplayName("Should handle empty payload and propagate downstream exception")
        void refreshToken_shouldPropagateException_whenPayloadIsEmpty() {
            Map<String, String> emptyPayload = Map.of();

            when(refreshTokenService.refreshToken(emptyPayload))
                    .thenThrow(new IllegalArgumentException("Refresh token must not be null or empty"));

            assertThatThrownBy(() -> authenticationController.refreshToken(emptyPayload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Refresh token must not be null or empty");
        }
    }

    @Nested
    @DisplayName("logout() tests")
    class LogoutTests {

        @Test
        @DisplayName("Should return 200 OK and null data when logout is successful")
        void logout_shouldReturnOk_whenTokenRevokedSuccessfully() {
            LogoutRequestDto logoutRequest = new LogoutRequestDto("token_to_revoke");

            ResponseEntity<ApiResponse<Void>> response = authenticationController.logout(logoutRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Logged out successfully.");
            assertThat(response.getBody().getData()).isNull();

            verify(refreshTokenService, times(1)).revokeToken("token_to_revoke");
        }

        @Test
        @DisplayName("Should propagate downstream exception when token revocation fails")
        void logout_shouldPropagateException_whenRevocationFails() {
            LogoutRequestDto logoutRequest = new LogoutRequestDto("invalid_token");

            doThrow(new RuntimeException("Token revocation failed"))
                    .when(refreshTokenService).revokeToken("invalid_token");

            assertThatThrownBy(() -> authenticationController.logout(logoutRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Token revocation failed");

            verify(refreshTokenService, times(1)).revokeToken("invalid_token");
        }
    }

    @Nested
    @DisplayName("userData() tests")
    class UserDataTests {

        @Test
        @DisplayName("Should return 200 OK with UserDataDto when user exists")
        void userData_shouldReturnOk_whenUserExists() {
            UserDataDto userDataDto = UserDataDto.builder()
                    .id(1L)
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(userService.userData(1L)).thenReturn(userDataDto);

            ResponseEntity<ApiResponse<UserDataDto>> response = authenticationController.userData(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Retrieved User data successfully");
            assertThat(response.getBody().getData()).isEqualTo(userDataDto);
            assertThat(response.getBody().getData().getId()).isEqualTo(1L);

            verify(userService, times(1)).userData(1L);
        }

        @Test
        @DisplayName("Should query userService correctly for boundary user IDs (0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)")
        void userData_boundaryIds_shouldQueryServiceCorrectly() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE};

            for (long id : boundaryIds) {
                UserDataDto dto = UserDataDto.builder().id(id).build();
                when(userService.userData(id)).thenReturn(dto);

                ResponseEntity<ApiResponse<UserDataDto>> response = authenticationController.userData(id);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getData().getId()).isEqualTo(id);
                verify(userService, times(1)).userData(id);
            }
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when user is not found")
        void userData_shouldPropagateException_whenUserNotFound() {
            when(userService.userData(999L))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

            assertThatThrownBy(() -> authenticationController.userData(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with id: 999");

            verify(userService, times(1)).userData(999L);
        }

        @Test
        @DisplayName("Should query userService when userId is null")
        void userData_shouldHandleNullUserId() {
            when(userService.userData(null))
                    .thenThrow(new ResourceNotFoundException("User not found with id: null"));

            assertThatThrownBy(() -> authenticationController.userData(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with id: null");

            verify(userService, times(1)).userData(null);
        }
    }
}
