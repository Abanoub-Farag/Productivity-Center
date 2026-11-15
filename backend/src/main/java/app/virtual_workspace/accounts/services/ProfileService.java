package app.virtual_workspace.accounts.services;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import app.virtual_workspace.accounts.dtos.data.UpdateUserDataDto;
import app.virtual_workspace.accounts.dtos.profile.UpdateUserProfileDto;
import app.virtual_workspace.accounts.dtos.profile.UserProfileDataDto;
import app.virtual_workspace.accounts.mappers.ProfileMapper;
import app.virtual_workspace.accounts.models.Profile;
import app.virtual_workspace.accounts.repositories.ProfileRepository;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.shared.services.FileStorageService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserService userService;
    private final ProfileMapper profileMapper;
    private final RedisTemplate redisTemplate;
    private final FileStorageService fileStorageService;

    public UserProfileDataDto getProfile(Long userId) {
        String cacheKey = "app:prod:user:" + userId;

        UserProfileDataDto userProfileDataDto = null;
        try{
            userProfileDataDto = (UserProfileDataDto) redisTemplate.opsForValue().get(cacheKey);
        }catch(Exception e){
            // Do nothing for now
        }
        if (userProfileDataDto != null){
            return userProfileDataDto;
        }
        
        Profile profile = profileRepository.findProfileAndUserByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Profile for user id: " + userId + " is not found"));
        userProfileDataDto = profileMapper.toUserProfileDataDto(profile.getUser(), profile);

        try{
            redisTemplate.opsForValue().set(cacheKey, userProfileDataDto, Duration.ofMinutes(10));
        }catch(Exception e){
            // Do nothing for now
        }

        return userProfileDataDto;
    }

    public UserProfileDataDto updateProfile(Long userId, UpdateUserProfileDto updateDto) {

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Profile for user id: " + userId + " is not found"));

        if (updateDto.getBio() != null) {
            profile.setBio(updateDto.getBio());
        }
        if (updateDto.getGender() != null) {
            profile.setGender(updateDto.getGender());
        }
        if (updateDto.getDateOfBirth() != null) {
            profile.setDateOfBirth(updateDto.getDateOfBirth());
        }
        if (updateDto.getProfilePicture() != null && !updateDto.getProfilePicture().isEmpty()){
            if (profile.getImageUrl() != null) {
                fileStorageService.deleteFile(profile.getImageUrl());
            }
            String profileUrl = fileStorageService.storeFile(updateDto.getProfilePicture());
            profile.setImageUrl(profileUrl);
        }

        profileRepository.save(profile);

        UpdateUserDataDto userUpdateDataDto = userService.updateData(userId, updateDto.getFirstName(),
                updateDto.getLastName());

        UserProfileDataDto userProfileDataDto = profileMapper.toUserProfileDataDto(userUpdateDataDto, profile);

        String cacheKey = "app:prod:user:" + userId;
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            // Do nothing for now
        }

        return userProfileDataDto;
    }
}