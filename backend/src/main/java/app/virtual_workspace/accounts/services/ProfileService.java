package app.virtual_workspace.accounts.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.accounts.dtos.data.UpdateUserDataDto;
import app.virtual_workspace.accounts.dtos.profile.UpdateUserProfileDto;
import app.virtual_workspace.accounts.dtos.profile.UserProfileDataDto;
import app.virtual_workspace.accounts.mappers.ProfileMapper;
import app.virtual_workspace.accounts.models.Profile;
import app.virtual_workspace.accounts.repositories.ProfileRepository;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserService userService;
    private final ProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public UserProfileDataDto getProfile(Long userId) {
        Profile profile = profileRepository.findProfileAndUserByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profille for user id: " + userId + " is not found"));
        return profileMapper.toUserProfileDataDto(profile.getUser(), profile);
    }

    @Transactional
    public UserProfileDataDto updateProfile(Long userId, UpdateUserProfileDto updateDto) {

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Profille for user id: " + userId + " is not found"));

        if (updateDto.getBio() != null) {
            profile.setBio(updateDto.getBio());
        }
        if (updateDto.getGender() != null) {
            profile.setGender(updateDto.getGender());
        }
        if (updateDto.getDateOfBirth() != null) {
            profile.setDateOfBirth(updateDto.getDateOfBirth());
        }

        profileRepository.save(profile);

        UpdateUserDataDto userUpdateDataDto = userService.updateData(userId, updateDto.getFirstName(),
                updateDto.getLastName());

        return profileMapper.toUserProfileDataDto(userUpdateDataDto, profile);
    }
}