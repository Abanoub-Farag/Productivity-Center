package app.virtual_workspace.accounts.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.models.enums.Role;
import app.virtual_workspace.accounts.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashed_password")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("loadUserByUsername() tests")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Should return UserPrincipal when user exists by email")
        void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
            when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

            UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

            assertThat(userDetails).isNotNull();
            assertThat(userDetails).isInstanceOf(UserPrincipal.class);
            assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
            assertThat(userDetails.getPassword()).isEqualTo("hashed_password");
            assertThat(userDetails.isEnabled()).isTrue();

            verify(userRepository, times(1)).findUserByEmail("test@example.com");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user is not found")
        void loadUserByUsername_shouldThrowUsernameNotFoundException_whenUserNotFound() {
            when(userRepository.findUserByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("No user found with email: unknown@example.com");

            verify(userRepository, times(1)).findUserByEmail("unknown@example.com");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException for null and empty email strings (boundary values)")
        void loadUserByUsername_shouldThrowUsernameNotFoundException_forNullAndEmptyEmail() {
            when(userRepository.findUserByEmail(null)).thenReturn(Optional.empty());
            when(userRepository.findUserByEmail("")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(null))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("No user found with email: null");

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(""))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("No user found with email: ");
        }

        @Test
        @DisplayName("Should propagate exception when userRepository.findUserByEmail fails downstream")
        void loadUserByUsername_shouldPropagateException_whenRepositoryThrows() {
            when(userRepository.findUserByEmail("test@example.com"))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("test@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }
}
