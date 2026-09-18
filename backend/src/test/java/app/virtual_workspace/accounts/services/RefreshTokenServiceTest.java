package app.virtual_workspace.accounts.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import app.virtual_workspace.accounts.dtos.auth.AuthResponseDto;
import app.virtual_workspace.accounts.models.RefreshToken;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.repositories.RefreshTokenRepository;
import app.virtual_workspace.accounts.repositories.UserRepository;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.exceptions.custom.TokenRefreshException;
import app.virtual_workspace.security.JwtService;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User sampleUser;
    private final Long refreshExpirationMs = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", refreshExpirationMs);

        sampleUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Nested
    @DisplayName("createRefreshToken() tests")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("Should create and return new RefreshToken when user has no existing token")
        void createRefreshToken_shouldCreateNewToken_whenNoExistingToken() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RefreshToken result = refreshTokenService.createRefreshToken(1L);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isNotNull();
            assertThat(result.getUser()).isEqualTo(sampleUser);
            assertThat(result.getExpiryDate()).isAfter(Instant.now());

            verify(userRepository, times(1)).findById(1L);
            verify(refreshTokenRepository, times(1)).findByUserId(1L);
            verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should reuse and update existing RefreshToken when user already has one")
        void createRefreshToken_shouldUpdateExistingToken_whenTokenExists() {
            RefreshToken existingToken = RefreshToken.builder()
                    .id(10L)
                    .token("old-token-value")
                    .user(sampleUser)
                    .expiryDate(Instant.now().minusSeconds(3600))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.of(existingToken));
            when(refreshTokenRepository.save(existingToken)).thenReturn(existingToken);

            RefreshToken result = refreshTokenService.createRefreshToken(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getToken()).isNotEqualTo("old-token-value");
            assertThat(result.getExpiryDate()).isAfter(Instant.now());

            verify(refreshTokenRepository, times(1)).save(existingToken);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user does not exist")
        void createRefreshToken_shouldThrowResourceNotFoundException_whenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.createRefreshToken(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository, times(1)).findById(999L);
            verify(refreshTokenRepository, never()).findByUserId(any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for boundary values: 0L, negative, and null")
        void createRefreshToken_shouldThrowResourceNotFoundException_forBoundaryIds() {
            when(userRepository.findById(0L)).thenReturn(Optional.empty());
            when(userRepository.findById(-1L)).thenReturn(Optional.empty());
            when(userRepository.findById(null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.createRefreshToken(0L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            assertThatThrownBy(() -> refreshTokenService.createRefreshToken(-1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            assertThatThrownBy(() -> refreshTokenService.createRefreshToken(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("Should propagate exception when repository save fails downstream")
        void createRefreshToken_shouldPropagateException_whenSaveThrows() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenThrow(new RuntimeException("DB save failure"));

            assertThatThrownBy(() -> refreshTokenService.createRefreshToken(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB save failure");
        }
    }

    @Nested
    @DisplayName("isTokenExpired() tests")
    class IsTokenExpiredTests {

        @Test
        @DisplayName("Should return true when token expiryDate is before now")
        void isTokenExpired_shouldReturnTrue_whenExpiryDateIsInPast() {
            RefreshToken expiredToken = RefreshToken.builder()
                    .expiryDate(Instant.now().minusSeconds(60))
                    .build();

            boolean result = refreshTokenService.isTokenExpired(expiredToken);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when token expiryDate is in the future")
        void isTokenExpired_shouldReturnFalse_whenExpiryDateIsInFuture() {
            RefreshToken validToken = RefreshToken.builder()
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .build();

            boolean result = refreshTokenService.isTokenExpired(validToken);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("refreshToken() tests")
    class RefreshTokenTests {

        private RefreshToken validToken;
        private RefreshToken expiredToken;
        private Map<String, String> payload;

        @BeforeEach
        void setUp() {
            validToken = RefreshToken.builder()
                    .id(10L)
                    .token("valid-refresh-token")
                    .user(sampleUser)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .build();

            expiredToken = RefreshToken.builder()
                    .id(11L)
                    .token("expired-refresh-token")
                    .user(sampleUser)
                    .expiryDate(Instant.now().minusSeconds(60))
                    .build();

            payload = new HashMap<>();
            payload.put("refreshToken", "valid-refresh-token");
        }

        @Test
        @DisplayName("Should issue new JWT and rotated refresh token when refresh token is valid")
        void refreshToken_shouldSucceed_whenTokenIsValid() {
            when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(validToken));
            when(jwtService.generateToken("user@example.com", 1L)).thenReturn("new-jwt-token");
            when(refreshTokenRepository.save(validToken)).thenReturn(validToken);

            AuthResponseDto response = refreshTokenService.refreshToken(payload);

            assertThat(response).isNotNull();
            assertThat(response.getJwtToken()).isEqualTo("new-jwt-token");
            assertThat(response.getRefreshToken()).isNotNull();
            assertThat(response.getRefreshToken()).isNotEqualTo("valid-refresh-token");

            verify(refreshTokenRepository, times(1)).findByToken("valid-refresh-token");
            verify(jwtService, times(1)).generateToken("user@example.com", 1L);
            verify(refreshTokenRepository, times(1)).save(validToken);
        }

        @Test
        @DisplayName("Should throw TokenRefreshException when token is expired")
        void refreshToken_shouldThrowTokenRefreshException_whenTokenIsExpired() {
            payload.put("refreshToken", "expired-refresh-token");
            when(refreshTokenRepository.findByToken("expired-refresh-token")).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> refreshTokenService.refreshToken(payload))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessage("Refresh token expired. Please login again.");

            verify(jwtService, never()).generateToken(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw TokenRefreshException when token is not found in repository")
        void refreshToken_shouldThrowTokenRefreshException_whenTokenNotFound() {
            payload.put("refreshToken", "non-existent-token");
            when(refreshTokenRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.refreshToken(payload))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessage("Invalid refresh token.");

            verify(jwtService, never()).generateToken(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw TokenRefreshException when payload has null or empty refreshToken key")
        void refreshToken_shouldThrowTokenRefreshException_whenPayloadTokenIsNullOrEmpty() {
            Map<String, String> nullTokenPayload = new HashMap<>();
            nullTokenPayload.put("refreshToken", null);

            when(refreshTokenRepository.findByToken(null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.refreshToken(nullTokenPayload))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessage("Invalid refresh token.");
        }

        @Test
        @DisplayName("Should propagate exception when jwtService fails downstream")
        void refreshToken_shouldPropagateException_whenJwtServiceThrows() {
            when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(validToken));
            when(jwtService.generateToken("user@example.com", 1L)).thenThrow(new RuntimeException("JWT signing error"));

            assertThatThrownBy(() -> refreshTokenService.refreshToken(payload))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("JWT signing error");

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("revokeToken() tests")
    class RevokeTokenTests {

        private RefreshToken existingToken;

        @BeforeEach
        void setUp() {
            existingToken = RefreshToken.builder()
                    .id(10L)
                    .token("revokable-token")
                    .user(sampleUser)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .build();
        }

        @Test
        @DisplayName("Should delete token when token exists")
        void revokeToken_shouldDeleteToken_whenTokenExists() {
            when(refreshTokenRepository.findByToken("revokable-token")).thenReturn(Optional.of(existingToken));

            refreshTokenService.revokeToken("revokable-token");

            verify(refreshTokenRepository, times(1)).findByToken("revokable-token");
            verify(refreshTokenRepository, times(1)).delete(existingToken);
        }

        @Test
        @DisplayName("Should throw TokenRefreshException when token to revoke is not found")
        void revokeToken_shouldThrowTokenRefreshException_whenTokenNotFound() {
            when(refreshTokenRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.revokeToken("non-existent-token"))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessage("Invalid refresh token.");

            verify(refreshTokenRepository, times(1)).findByToken("non-existent-token");
            verify(refreshTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw TokenRefreshException for null and empty token strings")
        void revokeToken_shouldThrowTokenRefreshException_forNullAndEmpty() {
            when(refreshTokenRepository.findByToken(null)).thenReturn(Optional.empty());
            when(refreshTokenRepository.findByToken("")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.revokeToken(null))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessage("Invalid refresh token.");

            assertThatThrownBy(() -> refreshTokenService.revokeToken(""))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessage("Invalid refresh token.");
        }

        @Test
        @DisplayName("Should propagate exception when repository delete fails downstream")
        void revokeToken_shouldPropagateException_whenDeleteThrows() {
            when(refreshTokenRepository.findByToken("revokable-token")).thenReturn(Optional.of(existingToken));
            org.mockito.Mockito.doThrow(new RuntimeException("DB delete failure"))
                    .when(refreshTokenRepository).delete(existingToken);

            assertThatThrownBy(() -> refreshTokenService.revokeToken("revokable-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB delete failure");
        }
    }
}
