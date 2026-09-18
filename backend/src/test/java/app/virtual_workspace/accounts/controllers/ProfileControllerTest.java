package app.virtual_workspace.accounts.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.accounts.dtos.profile.UpdateUserProfileDto;
import app.virtual_workspace.accounts.dtos.profile.UserProfileDataDto;
import app.virtual_workspace.accounts.models.enums.Gender;
import app.virtual_workspace.accounts.models.enums.Role;
import app.virtual_workspace.accounts.services.ProfileService;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.shared.dtos.ApiResponse;

@ExtendWith(MockitoExtension.class)
public class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    private UserProfileDataDto sampleProfileData;
    private UserPrincipal samplePrincipal;

    @BeforeEach
    void setUp() {
        sampleProfileData = new UserProfileDataDto(
                "Software Engineer",
                Gender.MALE,
                LocalDate.of(1995, 5, 20),
                "test@example.com",
                "John",
                "Doe",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        samplePrincipal = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashed_password")
                .active(true)
                .role(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("getProfile() tests")
    class GetProfileTests {

        @Test
        @DisplayName("Should return 200 OK with UserProfileDataDto when user exists")
        void getProfile_shouldReturnOk_whenUserExists() {
            when(profileService.getProfile(1L)).thenReturn(sampleProfileData);

            ResponseEntity<ApiResponse<UserProfileDataDto>> response = profileController.getProfile(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Returned user profile successfully");
            assertThat(response.getBody().getData()).isEqualTo(sampleProfileData);
            assertThat(response.getBody().getData().getEmail()).isEqualTo("test@example.com");

            verify(profileService, times(1)).getProfile(1L);
        }

        @Test
        @DisplayName("Should query profileService correctly for boundary user IDs (0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)")
        void getProfile_boundaryIds_shouldQueryServiceCorrectly() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE};

            for (long id : boundaryIds) {
                UserProfileDataDto dto = new UserProfileDataDto();
                dto.setEmail("id" + id + "@example.com");
                when(profileService.getProfile(id)).thenReturn(dto);

                ResponseEntity<ApiResponse<UserProfileDataDto>> response = profileController.getProfile(id);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getData().getEmail()).isEqualTo("id" + id + "@example.com");
                verify(profileService, times(1)).getProfile(id);
            }
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when profile does not exist")
        void getProfile_shouldPropagateException_whenUserNotFound() {
            when(profileService.getProfile(999L))
                    .thenThrow(new ResourceNotFoundException("User profile not found with id: 999"));

            assertThatThrownBy(() -> profileController.getProfile(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User profile not found with id: 999");

            verify(profileService, times(1)).getProfile(999L);
        }

        @Test
        @DisplayName("Should propagate downstream RuntimeException during profile retrieval")
        void getProfile_shouldPropagateGenericException_whenDownstreamFails() {
            when(profileService.getProfile(1L)).thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> profileController.getProfile(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }

        @Test
        @DisplayName("Should handle null userId and call profileService")
        void getProfile_shouldHandleNullUserId() {
            when(profileService.getProfile(null))
                    .thenThrow(new ResourceNotFoundException("User profile not found with id: null"));

            assertThatThrownBy(() -> profileController.getProfile(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User profile not found with id: null");

            verify(profileService, times(1)).getProfile(null);
        }
    }

    @Nested
    @DisplayName("updateProfile() tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should return 200 OK with updated UserProfileDataDto when request is valid")
        void updateProfile_shouldReturnOk_whenRequestIsValid() {
            UpdateUserProfileDto updateDto = UpdateUserProfileDto.builder()
                    .bio("Updated Bio")
                    .gender(Gender.FEMALE)
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            UserProfileDataDto updatedProfile = new UserProfileDataDto(
                    "Updated Bio",
                    Gender.FEMALE,
                    LocalDate.of(1990, 1, 1),
                    "test@example.com",
                    "Jane",
                    "Smith",
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            when(profileService.updateProfile(1L, updateDto)).thenReturn(updatedProfile);

            ResponseEntity<ApiResponse<UserProfileDataDto>> response =
                    profileController.updateProfile(updateDto, samplePrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Update user profile successfully");
            assertThat(response.getBody().getData()).isEqualTo(updatedProfile);
            assertThat(response.getBody().getData().getBio()).isEqualTo("Updated Bio");
            assertThat(response.getBody().getData().getFirstName()).isEqualTo("Jane");

            verify(profileService, times(1)).updateProfile(1L, updateDto);
        }

        @Test
        @DisplayName("Should update profile correctly for boundary principal IDs (0L, -1L, Long.MAX_VALUE)")
        void updateProfile_boundaryPrincipalIds_shouldQueryServiceCorrectly() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};
            UpdateUserProfileDto updateDto = UpdateUserProfileDto.builder().bio("Boundary Bio").build();

            for (long id : boundaryIds) {
                UserPrincipal principal = UserPrincipal.builder().id(id).build();
                UserProfileDataDto resultDto = new UserProfileDataDto();
                when(profileService.updateProfile(id, updateDto)).thenReturn(resultDto);

                ResponseEntity<ApiResponse<UserProfileDataDto>> response =
                        profileController.updateProfile(updateDto, principal);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(profileService, times(1)).updateProfile(id, updateDto);
            }
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when user is not found during update")
        void updateProfile_shouldPropagateException_whenUserNotFound() {
            UpdateUserProfileDto updateDto = UpdateUserProfileDto.builder().build();

            when(profileService.updateProfile(eq(1L), any()))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 1"));

            assertThatThrownBy(() -> profileController.updateProfile(updateDto, samplePrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with id: 1");

            verify(profileService, times(1)).updateProfile(1L, updateDto);
        }

        @Test
        @DisplayName("Should throw NullPointerException when userPrincipal is null")
        void updateProfile_shouldThrowNpe_whenUserPrincipalIsNull() {
            UpdateUserProfileDto updateDto = UpdateUserProfileDto.builder().build();

            assertThatThrownBy(() -> profileController.updateProfile(updateDto, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should propagate downstream RuntimeException during profile update")
        void updateProfile_shouldPropagateGenericException_whenDownstreamFails() {
            UpdateUserProfileDto updateDto = UpdateUserProfileDto.builder().build();
            when(profileService.updateProfile(eq(1L), any()))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> profileController.updateProfile(updateDto, samplePrincipal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }
}
