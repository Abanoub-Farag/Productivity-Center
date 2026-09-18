package app.virtual_workspace.accounts.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.accounts.dtos.auth.AuthRequestDto;
import app.virtual_workspace.accounts.dtos.auth.AuthResponseDto;
import app.virtual_workspace.accounts.dtos.auth.CreateUserRequestDto;
import app.virtual_workspace.accounts.events.UserRegisteredEvent;
import app.virtual_workspace.accounts.mappers.AuthMapper;
import app.virtual_workspace.accounts.models.RefreshToken;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.models.enums.Role;
import app.virtual_workspace.accounts.repositories.UserRepository;
import app.virtual_workspace.exceptions.custom.ResourceAlreadyExistsException;
import app.virtual_workspace.security.JwtService;

@ExtendWith(MockitoExtension.class)
public class UserAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private UserAuthService userAuthService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("register() tests")
    class RegisterTests {

        private CreateUserRequestDto createUserRequestDto;
        private User user;
        private User savedUser;
        private RefreshToken refreshToken;

        @BeforeEach
        void setUp() {
            createUserRequestDto = CreateUserRequestDto.builder()
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .password("Password123!")
                    .build();

            user = User.builder()
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .password("Password123!")
                    .role(Role.ROLE_USER)
                    .isActive(true)
                    .build();

            savedUser = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .password("encoded_password")
                    .role(Role.ROLE_USER)
                    .isActive(true)
                    .build();

            refreshToken = RefreshToken.builder()
                    .id(10L)
                    .token("mock-refresh-token")
                    .user(savedUser)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .build();
        }

        @Test
        @DisplayName("Should successfully register user and return AuthResponseDto")
        void register_shouldSucceed_whenEmailDoesNotExist() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(authMapper.registerDtoToModel(createUserRequestDto)).thenReturn(user);
            when(passwordEncoder.encode("Password123!")).thenReturn("encoded_password");
            when(userRepository.save(user)).thenReturn(savedUser);
            when(jwtService.generateToken("test@example.com", 1L)).thenReturn("mock-jwt-token");
            when(refreshTokenService.createRefreshToken(savedUser.getId())).thenReturn(refreshToken);

            AuthResponseDto response = userAuthService.register(createUserRequestDto);

            assertThat(response).isNotNull();
            assertThat(response.getJwtToken()).isEqualTo("mock-jwt-token");
            assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");

            verify(userRepository, times(1)).existsByEmail("test@example.com");
            verify(authMapper, times(1)).registerDtoToModel(createUserRequestDto);
            verify(passwordEncoder, times(1)).encode("Password123!");
            verify(userRepository, times(1)).save(user);
            verify(applicationEventPublisher, times(1)).publishEvent(new UserRegisteredEvent(1L));
            verify(jwtService, times(1)).generateToken("test@example.com", 1L);
            verify(refreshTokenService, times(1)).createRefreshToken(1L);
        }

        @Test
        @DisplayName("Should throw ResourceAlreadyExistsException when email already exists")
        void register_shouldThrowResourceAlreadyExistsException_whenEmailAlreadyExists() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            ResourceAlreadyExistsException exception = assertThrows(
                    ResourceAlreadyExistsException.class,
                    () -> userAuthService.register(createUserRequestDto)
            );

            assertThat(exception.getMessage())
                    .isEqualTo("An account with this email already exists. Please log in or use a different address.");

            verify(userRepository, times(1)).existsByEmail("test@example.com");
            verify(authMapper, never()).registerDtoToModel(any());
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
            verify(jwtService, never()).generateToken(any(), any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Should throw exception when password encoder fails downstream")
        void register_shouldThrowException_whenPasswordEncoderThrows() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(authMapper.registerDtoToModel(createUserRequestDto)).thenReturn(user);
            when(passwordEncoder.encode("Password123!")).thenThrow(new RuntimeException("Encoding failure"));

            assertThrows(RuntimeException.class, () -> userAuthService.register(createUserRequestDto));

            verify(userRepository, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
            verify(jwtService, never()).generateToken(any(), any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Should throw exception when userRepository.save fails downstream")
        void register_shouldThrowException_whenUserRepositorySaveThrows() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(authMapper.registerDtoToModel(createUserRequestDto)).thenReturn(user);
            when(passwordEncoder.encode("Password123!")).thenReturn("encoded_password");
            when(userRepository.save(user)).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class, () -> userAuthService.register(createUserRequestDto));

            verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
            verify(jwtService, never()).generateToken(any(), any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Should throw exception when ApplicationEventPublisher fails downstream")
        void register_shouldThrowException_whenEventPublisherThrows() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(authMapper.registerDtoToModel(createUserRequestDto)).thenReturn(user);
            when(passwordEncoder.encode("Password123!")).thenReturn("encoded_password");
            when(userRepository.save(user)).thenReturn(savedUser);
            org.mockito.Mockito.doThrow(new RuntimeException("Event publishing error"))
                    .when(applicationEventPublisher).publishEvent(any(UserRegisteredEvent.class));

            assertThrows(RuntimeException.class, () -> userAuthService.register(createUserRequestDto));

            verify(jwtService, never()).generateToken(any(), any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Should throw exception when JwtService fails downstream")
        void register_shouldThrowException_whenJwtServiceThrows() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(authMapper.registerDtoToModel(createUserRequestDto)).thenReturn(user);
            when(passwordEncoder.encode("Password123!")).thenReturn("encoded_password");
            when(userRepository.save(user)).thenReturn(savedUser);
            when(jwtService.generateToken("test@example.com", 1L))
                    .thenThrow(new RuntimeException("JWT signing error"));

            assertThrows(RuntimeException.class, () -> userAuthService.register(createUserRequestDto));

            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Should throw exception when RefreshTokenService fails downstream")
        void register_shouldThrowException_whenRefreshTokenServiceThrows() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(authMapper.registerDtoToModel(createUserRequestDto)).thenReturn(user);
            when(passwordEncoder.encode("Password123!")).thenReturn("encoded_password");
            when(userRepository.save(user)).thenReturn(savedUser);
            when(jwtService.generateToken("test@example.com", 1L)).thenReturn("mock-jwt-token");
            when(refreshTokenService.createRefreshToken(savedUser.getId()))
                    .thenThrow(new RuntimeException("Refresh token store failure"));

            assertThrows(RuntimeException.class, () -> userAuthService.register(createUserRequestDto));
        }
    }

    @Nested
    @DisplayName("login() tests")
    class LoginTests {

        private AuthRequestDto authRequestDto;
        private User user;
        private RefreshToken refreshToken;

        @BeforeEach
        void setUp() {
            authRequestDto = AuthRequestDto.builder()
                    .email("test@example.com")
                    .password("Password123!")
                    .build();

            user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .password("encoded_password")
                    .role(Role.ROLE_USER)
                    .isActive(true)
                    .build();

            refreshToken = RefreshToken.builder()
                    .id(10L)
                    .token("mock-refresh-token")
                    .user(user)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .build();
        }

        @Test
        @DisplayName("Should successfully authenticate, find user, and return AuthResponseDto")
        void login_shouldSucceed_whenCredentialsAreValidAndUserExists() {
            when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(jwtService.generateToken("test@example.com", 1L)).thenReturn("mock-jwt-token");
            when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);

            AuthResponseDto response = userAuthService.login(authRequestDto);

            assertThat(response).isNotNull();
            assertThat(response.getJwtToken()).isEqualTo("mock-jwt-token");
            assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");

            verify(authenticationManager, times(1)).authenticate(
                    new UsernamePasswordAuthenticationToken("test@example.com", "Password123!")
            );
            verify(userRepository, times(1)).findUserByEmail("test@example.com");
            verify(jwtService, times(1)).generateToken("test@example.com", 1L);
            verify(refreshTokenService, times(1)).createRefreshToken(1L);
        }

        @Test
        @DisplayName("Should throw InsufficientAuthenticationException when user not found in repository")
        void login_shouldThrowInsufficientAuthenticationException_whenUserNotFound() {
            when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.empty());

            InsufficientAuthenticationException exception = assertThrows(
                    InsufficientAuthenticationException.class,
                    () -> userAuthService.login(authRequestDto)
            );

            assertThat(exception.getMessage()).isEqualTo("User not found");

            verify(authenticationManager, times(1)).authenticate(
                    new UsernamePasswordAuthenticationToken("test@example.com", "Password123!")
            );
            verify(userRepository, times(1)).findUserByEmail("test@example.com");
            verify(jwtService, never()).generateToken(any(), any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Should propagate BadCredentialsException when authenticationManager fails")
        void login_shouldThrowAuthenticationException_whenCredentialsAreInvalid() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            BadCredentialsException exception = assertThrows(
                    BadCredentialsException.class,
                    () -> userAuthService.login(authRequestDto)
            );

            assertThat(exception.getMessage()).isEqualTo("Bad credentials");

            verify(userRepository, never()).findUserByEmail(any());
            verify(jwtService, never()).generateToken(any(), any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Should propagate DisabledException when user account is disabled")
        void login_shouldThrowDisabledException_whenAccountIsDisabled() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new DisabledException("User account is disabled"));

            DisabledException exception = assertThrows(
                    DisabledException.class,
                    () -> userAuthService.login(authRequestDto)
            );

            assertThat(exception.getMessage()).isEqualTo("User account is disabled");

            verify(userRepository, never()).findUserByEmail(any());
            verify(jwtService, never()).generateToken(any(), any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Should throw exception when JwtService fails downstream")
        void login_shouldThrowException_whenJwtServiceFails() {
            when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(jwtService.generateToken("test@example.com", 1L)).thenThrow(new RuntimeException("JWT signing error"));

            assertThrows(RuntimeException.class, () -> userAuthService.login(authRequestDto));

            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Should throw exception when RefreshTokenService fails downstream")
        void login_shouldThrowException_whenRefreshTokenServiceFails() {
            when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(jwtService.generateToken("test@example.com", 1L)).thenReturn("mock-jwt-token");
            when(refreshTokenService.createRefreshToken(1L)).thenThrow(new RuntimeException("Redis connection error"));

            assertThrows(RuntimeException.class, () -> userAuthService.login(authRequestDto));
        }
    }

    @Nested
    @DisplayName("getAuthenticatedUser() tests")
    class GetAuthenticatedUserTests {

        private User user;
        private UserPrincipal userPrincipal;

        @BeforeEach
        void setUp() {
            user = User.builder()
                    .id(1L)
                    .email("auth@example.com")
                    .firstName("Jane")
                    .lastName("Doe")
                    .password("encoded_password")
                    .role(Role.ROLE_USER)
                    .isActive(true)
                    .build();

            userPrincipal = UserPrincipal.builder()
                    .id(1L)
                    .email("auth@example.com")
                    .password("encoded_password")
                    .role(Role.ROLE_USER)
                    .active(true)
                    .build();
        }

        @Test
        @DisplayName("Should throw InsufficientAuthenticationException when Authentication is null")
        void getAuthenticatedUser_shouldThrowInsufficientAuthenticationException_whenAuthenticationIsNull() {
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(securityContext);

            InsufficientAuthenticationException exception = assertThrows(
                    InsufficientAuthenticationException.class,
                    () -> userAuthService.getAuthenticatedUser()
            );

            assertThat(exception.getMessage()).isEqualTo("User not authenticated");
            verify(userRepository, never()).findUserByEmail(any());
        }

        @Test
        @DisplayName("Should throw InsufficientAuthenticationException when isAuthenticated is false")
        void getAuthenticatedUser_shouldThrowInsufficientAuthenticationException_whenNotAuthenticated() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(false);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            InsufficientAuthenticationException exception = assertThrows(
                    InsufficientAuthenticationException.class,
                    () -> userAuthService.getAuthenticatedUser()
            );

            assertThat(exception.getMessage()).isEqualTo("User not authenticated");
            verify(userRepository, never()).findUserByEmail(any());
        }

        @Test
        @DisplayName("Should throw InsufficientAuthenticationException when Principal is not an instance of UserPrincipal")
        void getAuthenticatedUser_shouldThrowInsufficientAuthenticationException_whenPrincipalIsNotUserPrincipal() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            InsufficientAuthenticationException exception = assertThrows(
                    InsufficientAuthenticationException.class,
                    () -> userAuthService.getAuthenticatedUser()
            );

            assertThat(exception.getMessage()).isEqualTo("User not authenticated");
            verify(userRepository, never()).findUserByEmail(any());
        }

        @Test
        @DisplayName("Should throw InsufficientAuthenticationException when Principal is null")
        void getAuthenticatedUser_shouldThrowInsufficientAuthenticationException_whenPrincipalIsNull() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(null);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            InsufficientAuthenticationException exception = assertThrows(
                    InsufficientAuthenticationException.class,
                    () -> userAuthService.getAuthenticatedUser()
            );

            assertThat(exception.getMessage()).isEqualTo("User not authenticated");
            verify(userRepository, never()).findUserByEmail(any());
        }

        @Test
        @DisplayName("Should throw InsufficientAuthenticationException when authenticated user is not found in repository")
        void getAuthenticatedUser_shouldThrowInsufficientAuthenticationException_whenUserNotFoundInRepository() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userPrincipal);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(userRepository.findUserByEmail("auth@example.com")).thenReturn(Optional.empty());

            InsufficientAuthenticationException exception = assertThrows(
                    InsufficientAuthenticationException.class,
                    () -> userAuthService.getAuthenticatedUser()
            );

            assertThat(exception.getMessage()).isEqualTo("User not found");
            verify(userRepository, times(1)).findUserByEmail("auth@example.com");
        }

        @Test
        @DisplayName("Should return User when authentication is valid and user exists in repository")
        void getAuthenticatedUser_shouldReturnUser_whenAuthenticationIsValidAndUserExists() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userPrincipal);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(userRepository.findUserByEmail("auth@example.com")).thenReturn(Optional.of(user));

            User result = userAuthService.getAuthenticatedUser();

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("auth@example.com");
            verify(userRepository, times(1)).findUserByEmail("auth@example.com");
        }
    }

    @Nested
    @DisplayName("isOwner() tests")
    class IsOwnerTests {

        private User authenticatedUser;
        private UserPrincipal userPrincipal;

        @BeforeEach
        void setUp() {
            authenticatedUser = User.builder()
                    .id(42L)
                    .email("owner@example.com")
                    .firstName("Owner")
                    .lastName("User")
                    .password("encoded_password")
                    .role(Role.ROLE_USER)
                    .isActive(true)
                    .build();

            userPrincipal = UserPrincipal.builder()
                    .id(42L)
                    .email("owner@example.com")
                    .password("encoded_password")
                    .role(Role.ROLE_USER)
                    .active(true)
                    .build();
        }

        private void setupSecurityContext() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userPrincipal);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(userRepository.findUserByEmail("owner@example.com")).thenReturn(Optional.of(authenticatedUser));
        }

        @Test
        @DisplayName("Should return true when target userId equals authenticated userId")
        void isOwner_shouldReturnTrue_whenUserIdMatchesAuthenticatedUser() {
            setupSecurityContext();

            boolean result = userAuthService.isOwner(42L);

            assertTrue(result);
            verify(userRepository, times(1)).findUserByEmail("owner@example.com");
        }

        @Test
        @DisplayName("Should return false when target userId does not match authenticated userId")
        void isOwner_shouldReturnFalse_whenUserIdDoesNotMatchAuthenticatedUser() {
            setupSecurityContext();

            boolean result = userAuthService.isOwner(99L);

            assertFalse(result);
            verify(userRepository, times(1)).findUserByEmail("owner@example.com");
        }

        @Test
        @DisplayName("Should return false for zero, negative, and extreme boundary ID values")
        void isOwner_shouldReturnFalse_whenUserIdIsBoundaryValue() {
            setupSecurityContext();

            assertFalse(userAuthService.isOwner(0L));
            assertFalse(userAuthService.isOwner(-1L));
            assertFalse(userAuthService.isOwner(Long.MAX_VALUE));
            assertFalse(userAuthService.isOwner(Long.MIN_VALUE));
        }

        @Test
        @DisplayName("Should return true when both target userId and authenticated userId are Long.MAX_VALUE")
        void isOwner_shouldReturnTrue_whenBothIdsAreLongMaxValue() {
            User maxIdUser = User.builder().id(Long.MAX_VALUE).email("max@example.com").build();
            UserPrincipal maxIdPrincipal = UserPrincipal.builder().id(Long.MAX_VALUE).email("max@example.com").build();

            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(maxIdPrincipal);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(userRepository.findUserByEmail("max@example.com")).thenReturn(Optional.of(maxIdUser));

            assertTrue(userAuthService.isOwner(Long.MAX_VALUE));
        }

        @Test
        @DisplayName("Should return false when target userId is null (boundary case)")
        void isOwner_shouldReturnFalse_whenTargetUserIdIsNull() {
            setupSecurityContext();

            assertFalse(userAuthService.isOwner(null));
        }

        @Test
        @DisplayName("Should return false when authenticated user ID is null and target userId is non-null")
        void isOwner_shouldReturnFalse_whenAuthenticatedUserHasNullId() {
            User nullIdUser = User.builder().id(null).email("owner@example.com").build();
            UserPrincipal principal = UserPrincipal.builder().id(null).email("owner@example.com").build();

            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(principal);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(userRepository.findUserByEmail("owner@example.com")).thenReturn(Optional.of(nullIdUser));

            assertFalse(userAuthService.isOwner(42L));
        }

        @Test
        @DisplayName("Should propagate InsufficientAuthenticationException when user is unauthenticated")
        void isOwner_shouldThrowInsufficientAuthenticationException_whenUnauthenticated() {
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(securityContext);

            assertThrows(
                    InsufficientAuthenticationException.class,
                    () -> userAuthService.isOwner(42L)
            );

            verify(userRepository, never()).findUserByEmail(any());
        }
    }
}