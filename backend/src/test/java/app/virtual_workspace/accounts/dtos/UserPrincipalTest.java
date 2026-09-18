package app.virtual_workspace.accounts.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.models.enums.Role;

public class UserPrincipalTest {

    @Nested
    @DisplayName("create() factory method tests")
    class CreateFactoryTests {

        @Test
        @DisplayName("Should create UserPrincipal from User entity with all fields")
        void create_shouldMapAllFieldsFromUser() {
            User user = User.builder()
                    .id(10L)
                    .email("user@example.com")
                    .password("encoded_pass")
                    .isActive(true)
                    .role(Role.ROLE_USER)
                    .build();

            UserPrincipal principal = UserPrincipal.create(user);

            assertThat(principal).isNotNull();
            assertThat(principal.getId()).isEqualTo(10L);
            assertThat(principal.getEmail()).isEqualTo("user@example.com");
            assertThat(principal.getPassword()).isEqualTo("encoded_pass");
            assertThat(principal.isActive()).isTrue();
            assertThat(principal.getRole()).isEqualTo(Role.ROLE_USER);
        }

        @Test
        @DisplayName("Should create UserPrincipal when user is inactive and has ROLE_ADMIN")
        void create_shouldMapInactiveAdminUser() {
            User user = User.builder()
                    .id(20L)
                    .email("admin@example.com")
                    .password("admin_pass")
                    .isActive(false)
                    .role(Role.ROLE_ADMIN)
                    .build();

            UserPrincipal principal = UserPrincipal.create(user);

            assertThat(principal.getId()).isEqualTo(20L);
            assertThat(principal.isActive()).isFalse();
            assertThat(principal.getRole()).isEqualTo(Role.ROLE_ADMIN);
            assertThat(principal.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("getAuthorities() tests")
    class GetAuthoritiesTests {

        @Test
        @DisplayName("Should return authority matching role when role is present")
        void getAuthorities_shouldReturnAuthority_whenRoleIsPresent() {
            UserPrincipal principal = UserPrincipal.builder()
                    .role(Role.ROLE_USER)
                    .build();

            Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

            assertThat(authorities).hasSize(1);
            assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("Should return empty collection when role is null")
        void getAuthorities_shouldReturnEmpty_whenRoleIsNull() {
            UserPrincipal principal = UserPrincipal.builder()
                    .role(null)
                    .build();

            Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

            assertThat(authorities).isEmpty();
        }
    }

    @Nested
    @DisplayName("UserDetails contract methods tests")
    class UserDetailsContractTests {

        @Test
        @DisplayName("getUsername() should return email")
        void getUsername_shouldReturnEmail() {
            UserPrincipal principal = UserPrincipal.builder()
                    .email("principal@example.com")
                    .build();

            assertThat(principal.getUsername()).isEqualTo("principal@example.com");
        }

        @Test
        @DisplayName("isEnabled() should reflect active field")
        void isEnabled_shouldReflectActive() {
            UserPrincipal activePrincipal = UserPrincipal.builder().active(true).build();
            UserPrincipal inactivePrincipal = UserPrincipal.builder().active(false).build();

            assertThat(activePrincipal.isEnabled()).isTrue();
            assertThat(inactivePrincipal.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Account non-expired, non-locked, and credentials non-expired should always return true")
        void booleanFlags_shouldAlwaysReturnTrue() {
            UserPrincipal principal = UserPrincipal.builder().build();

            assertThat(principal.isAccountNonExpired()).isTrue();
            assertThat(principal.isAccountNonLocked()).isTrue();
            assertThat(principal.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("Constructor with JsonCreator should correctly populate all fields")
        void constructor_shouldPopulateAllFields() {
            UserPrincipal principal = new UserPrincipal(5L, "custom@test.com", "secret", true, Role.ROLE_USER);

            assertThat(principal.getId()).isEqualTo(5L);
            assertThat(principal.getEmail()).isEqualTo("custom@test.com");
            assertThat(principal.getPassword()).isEqualTo("secret");
            assertThat(principal.isActive()).isTrue();
            assertThat(principal.getRole()).isEqualTo(Role.ROLE_USER);
        }
    }
}
