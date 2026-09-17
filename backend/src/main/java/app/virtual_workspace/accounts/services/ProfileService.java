package app.virtual_workspace.accounts.services;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.accounts.dtos.profile.UpdateUserProfileDto;
import app.virtual_workspace.accounts.dtos.profile.UserProfileDto;
import app.virtual_workspace.accounts.mappers.ProfileMapper;
import app.virtual_workspace.accounts.models.Profile;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.repositories.ProfileRepository;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserService userService;
    private final ProfileMapper profileMapper;
    private final UserAuthService userAuthService;

    public void createProfile(Long userId) {
        User user = userService.findUserById(userId);

        Profile profile = new Profile();
        profile.setUser(user);
        profileRepository.save(profile);
    }

    @Cacheable(value = "profiles", key = "#userId")
    public UserProfileDto getProfile(Long userId) {
        Profile profile = profileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profille for user id: " + userId + " is not found"));
        return profileMapper.toUserProfileDto(profile.getUser(), profile);
    }

    @Transactional
    @CachePut(value = "profiles", key = "@userAuthService.getAuthenticatedUser().getId()")
    public UserProfileDto updateProfile(UpdateUserProfileDto updateDto) {
        User user = userAuthService.getAuthenticatedUser();

        Profile profile = profileRepository.findByUserIdWithUser(user.getId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Profille for user id: " + user.getId() + " is not found"));

        if (updateDto.getFirstName() != null && !updateDto.getFirstName().isBlank()) {
            user.setFirstName(updateDto.getFirstName());
        }
        if (updateDto.getLastName() != null && !updateDto.getLastName().isBlank()) {
            user.setLastName(updateDto.getLastName());
        }

        if (updateDto.getBio() != null) {
            profile.setBio(updateDto.getBio());
        }
        if (updateDto.getGender() != null) {
            profile.setGender(updateDto.getGender());
        }
        if (updateDto.getDateOfBirth() != null) {
            profile.setDateOfBirth(updateDto.getDateOfBirth());
        }

        userService.saveUser(user);
        profileRepository.save(profile);

        return profileMapper.toUserProfileDto(user, profile);
    }
}
