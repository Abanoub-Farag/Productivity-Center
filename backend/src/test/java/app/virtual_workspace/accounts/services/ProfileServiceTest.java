package app.virtual_workspace.accounts.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.virtual_workspace.accounts.dtos.data.UpdateUserDataDto;
import app.virtual_workspace.accounts.dtos.profile.UpdateUserProfileDto;
import app.virtual_workspace.accounts.dtos.profile.UserProfileDataDto;
import app.virtual_workspace.accounts.mappers.ProfileMapper;
import app.virtual_workspace.accounts.models.Profile;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.models.enums.Gender;
import app.virtual_workspace.accounts.repositories.ProfileRepository;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserService userService;

    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private ProfileService profileService;

    private User sampleUser;
    private Profile sampleProfile;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        sampleProfile = new Profile(
                10L,
                "Initial bio",
                Gender.MALE,
                LocalDate.of(1995, 5, 15),
                sampleUser
        );
    }

    @Nested
    @DisplayName("getProfile() tests")
    class GetProfileTests {

        @Test
        @DisplayName("Should return UserProfileDataDto when profile exists for user")
        void getProfile_shouldReturnProfileDto_whenProfileExists() {
            UserProfileDataDto expectedDto = new UserProfileDataDto();
            expectedDto.setBio("Initial bio");
            expectedDto.setGender(Gender.MALE);
            expectedDto.setDateOfBirth(LocalDate.of(1995, 5, 15));
            expectedDto.setFirstName("John");
            expectedDto.setLastName("Doe");

            when(profileRepository.findProfileAndUserByUserId(1L)).thenReturn(Optional.of(sampleProfile));
            when(profileMapper.toUserProfileDataDto(sampleUser, sampleProfile)).thenReturn(expectedDto);

            UserProfileDataDto result = profileService.getProfile(1L);

            assertThat(result).isNotNull();
            assertThat(result.getBio()).isEqualTo("Initial bio");
            assertThat(result.getGender()).isEqualTo(Gender.MALE);
            verify(profileRepository, times(1)).findProfileAndUserByUserId(1L);
            verify(profileMapper, times(1)).toUserProfileDataDto(sampleUser, sampleProfile);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when profile does not exist")
        void getProfile_shouldThrowResourceNotFoundException_whenNotFound() {
            when(profileRepository.findProfileAndUserByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfile(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Profille for user id: 999 is not found");

            verify(profileRepository, times(1)).findProfileAndUserByUserId(999L);
            verify(profileMapper, never()).toUserProfileDataDto(any(User.class), any(Profile.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for boundary values: 0L, negative, MIN, MAX, and null")
        void getProfile_shouldThrowResourceNotFoundException_forBoundaryValues() {
            when(profileRepository.findProfileAndUserByUserId(0L)).thenReturn(Optional.empty());
            when(profileRepository.findProfileAndUserByUserId(-1L)).thenReturn(Optional.empty());
            when(profileRepository.findProfileAndUserByUserId(Long.MAX_VALUE)).thenReturn(Optional.empty());
            when(profileRepository.findProfileAndUserByUserId(Long.MIN_VALUE)).thenReturn(Optional.empty());
            when(profileRepository.findProfileAndUserByUserId(null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfile(0L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Profille for user id: 0 is not found");

            assertThatThrownBy(() -> profileService.getProfile(-1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Profille for user id: -1 is not found");

            assertThatThrownBy(() -> profileService.getProfile(Long.MAX_VALUE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Profille for user id: " + Long.MAX_VALUE + " is not found");

            assertThatThrownBy(() -> profileService.getProfile(Long.MIN_VALUE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Profille for user id: " + Long.MIN_VALUE + " is not found");

            assertThatThrownBy(() -> profileService.getProfile(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Profille for user id: null is not found");
        }

        @Test
        @DisplayName("Should propagate exception when repository query fails downstream")
        void getProfile_shouldPropagateException_whenRepositoryThrows() {
            when(profileRepository.findProfileAndUserByUserId(1L))
                    .thenThrow(new RuntimeException("Database failure"));

            assertThatThrownBy(() -> profileService.getProfile(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database failure");
        }
    }

    @Nested
    @DisplayName("updateProfile() tests")
    class UpdateProfileTests {

        private UpdateUserProfileDto updateDto;
        private UpdateUserDataDto updatedUserDto;
        private UserProfileDataDto expectedResultDto;

        @BeforeEach
        void setUp() {
            updateDto = UpdateUserProfileDto.builder()
                    .bio("Updated bio")
                    .gender(Gender.FEMALE)
                    .dateOfBirth(LocalDate.of(2000, 1, 1))
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            updatedUserDto = UpdateUserDataDto.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("john@example.com")
                    .build();

            expectedResultDto = new UserProfileDataDto();
            expectedResultDto.setBio("Updated bio");
            expectedResultDto.setGender(Gender.FEMALE);
            expectedResultDto.setDateOfBirth(LocalDate.of(2000, 1, 1));
            expectedResultDto.setFirstName("Jane");
            expectedResultDto.setLastName("Smith");
        }

        @Test
        @DisplayName("Should update all profile fields when all dto fields are non-null")
        void updateProfile_shouldUpdateAllFields_whenAllDtoFieldsProvided() {
            when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
            when(profileRepository.save(sampleProfile)).thenReturn(sampleProfile);
            when(userService.updateData(1L, "Jane", "Smith")).thenReturn(updatedUserDto);
            when(profileMapper.toUserProfileDataDto(updatedUserDto, sampleProfile)).thenReturn(expectedResultDto);

            UserProfileDataDto result = profileService.updateProfile(1L, updateDto);

            assertThat(result).isNotNull();
            assertThat(sampleProfile.getBio()).isEqualTo("Updated bio");
            assertThat(sampleProfile.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(sampleProfile.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));

            verify(profileRepository, times(1)).findByUserId(1L);
            verify(profileRepository, times(1)).save(sampleProfile);
            verify(userService, times(1)).updateData(1L, "Jane", "Smith");
            verify(profileMapper, times(1)).toUserProfileDataDto(updatedUserDto, sampleProfile);
        }

        @Test
        @DisplayName("Should not modify profile fields when dto fields are null (all null branches)")
        void updateProfile_shouldNotModifyProfileFields_whenDtoFieldsAreNull() {
            UpdateUserProfileDto nullFieldsDto = UpdateUserProfileDto.builder()
                    .bio(null)
                    .gender(null)
                    .dateOfBirth(null)
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
            when(profileRepository.save(sampleProfile)).thenReturn(sampleProfile);
            when(userService.updateData(1L, "Jane", "Smith")).thenReturn(updatedUserDto);
            when(profileMapper.toUserProfileDataDto(updatedUserDto, sampleProfile)).thenReturn(expectedResultDto);

            profileService.updateProfile(1L, nullFieldsDto);

            assertThat(sampleProfile.getBio()).isEqualTo("Initial bio");
            assertThat(sampleProfile.getGender()).isEqualTo(Gender.MALE);
            assertThat(sampleProfile.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 15));

            verify(profileRepository, times(1)).save(sampleProfile);
        }

        @Test
        @DisplayName("Should update only bio when only bio is non-null")
        void updateProfile_shouldUpdateOnlyBio_whenOnlyBioProvided() {
            UpdateUserProfileDto bioOnlyDto = UpdateUserProfileDto.builder()
                    .bio("New Bio Only")
                    .gender(null)
                    .dateOfBirth(null)
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
            when(profileRepository.save(sampleProfile)).thenReturn(sampleProfile);
            when(userService.updateData(1L, "Jane", "Smith")).thenReturn(updatedUserDto);
            when(profileMapper.toUserProfileDataDto(updatedUserDto, sampleProfile)).thenReturn(expectedResultDto);

            profileService.updateProfile(1L, bioOnlyDto);

            assertThat(sampleProfile.getBio()).isEqualTo("New Bio Only");
            assertThat(sampleProfile.getGender()).isEqualTo(Gender.MALE);
            assertThat(sampleProfile.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 15));
        }

        @Test
        @DisplayName("Should update only gender when only gender is non-null")
        void updateProfile_shouldUpdateOnlyGender_whenOnlyGenderProvided() {
            UpdateUserProfileDto genderOnlyDto = UpdateUserProfileDto.builder()
                    .bio(null)
                    .gender(Gender.FEMALE)
                    .dateOfBirth(null)
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
            when(profileRepository.save(sampleProfile)).thenReturn(sampleProfile);
            when(userService.updateData(1L, "Jane", "Smith")).thenReturn(updatedUserDto);
            when(profileMapper.toUserProfileDataDto(updatedUserDto, sampleProfile)).thenReturn(expectedResultDto);

            profileService.updateProfile(1L, genderOnlyDto);

            assertThat(sampleProfile.getBio()).isEqualTo("Initial bio");
            assertThat(sampleProfile.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(sampleProfile.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 15));
        }

        @Test
        @DisplayName("Should update only dateOfBirth when only dateOfBirth is non-null")
        void updateProfile_shouldUpdateOnlyDateOfBirth_whenOnlyDateOfBirthProvided() {
            LocalDate newDob = LocalDate.of(1990, 12, 31);
            UpdateUserProfileDto dobOnlyDto = UpdateUserProfileDto.builder()
                    .bio(null)
                    .gender(null)
                    .dateOfBirth(newDob)
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
            when(profileRepository.save(sampleProfile)).thenReturn(sampleProfile);
            when(userService.updateData(1L, "Jane", "Smith")).thenReturn(updatedUserDto);
            when(profileMapper.toUserProfileDataDto(updatedUserDto, sampleProfile)).thenReturn(expectedResultDto);

            profileService.updateProfile(1L, dobOnlyDto);

            assertThat(sampleProfile.getBio()).isEqualTo("Initial bio");
            assertThat(sampleProfile.getGender()).isEqualTo(Gender.MALE);
            assertThat(sampleProfile.getDateOfBirth()).isEqualTo(newDob);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when profile does not exist for update")
        void updateProfile_shouldThrowResourceNotFoundException_whenProfileNotFound() {
            when(profileRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.updateProfile(999L, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Profille for user id: 999 is not found");

            verify(profileRepository, times(1)).findByUserId(999L);
            verify(profileRepository, never()).save(any());
            verify(userService, never()).updateData(any(), any(), any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when userId is null (null boundary)")
        void updateProfile_shouldThrowResourceNotFoundException_whenUserIdIsNull() {
            when(profileRepository.findByUserId(null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.updateProfile(null, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Profille for user id: null is not found");

            verify(profileRepository, times(1)).findByUserId(null);
            verify(profileRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate exception when profileRepository.save fails downstream")
        void updateProfile_shouldPropagateException_whenRepositorySaveThrows() {
            when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
            when(profileRepository.save(sampleProfile)).thenThrow(new RuntimeException("Save error"));

            assertThatThrownBy(() -> profileService.updateProfile(1L, updateDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Save error");

            verify(userService, never()).updateData(any(), any(), any());
        }

        @Test
        @DisplayName("Should propagate exception when userService.updateData fails downstream")
        void updateProfile_shouldPropagateException_whenUserServiceThrows() {
            when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
            when(profileRepository.save(sampleProfile)).thenReturn(sampleProfile);
            when(userService.updateData(1L, "Jane", "Smith")).thenThrow(new RuntimeException("User update error"));

            assertThatThrownBy(() -> profileService.updateProfile(1L, updateDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User update error");

            verify(profileMapper, never()).toUserProfileDataDto(any(UpdateUserDataDto.class), any(Profile.class));
        }

        @Test
        @DisplayName("Should propagate exception when profileMapper fails downstream")
        void updateProfile_shouldPropagateException_whenMapperThrows() {
            when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
            when(profileRepository.save(sampleProfile)).thenReturn(sampleProfile);
            when(userService.updateData(1L, "Jane", "Smith")).thenReturn(updatedUserDto);
            when(profileMapper.toUserProfileDataDto(updatedUserDto, sampleProfile))
                    .thenThrow(new RuntimeException("Mapping error"));

            assertThatThrownBy(() -> profileService.updateProfile(1L, updateDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping error");
        }
    }
}
