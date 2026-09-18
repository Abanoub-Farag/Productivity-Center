package app.virtual_workspace.accounts.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.accounts.dtos.profile.UpdateUserProfileDto;
import app.virtual_workspace.accounts.dtos.profile.UserProfileDataDto;
import app.virtual_workspace.accounts.services.ProfileService;
import app.virtual_workspace.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileDataDto>> getProfile(@PathVariable Long userId) {
        UserProfileDataDto profile = profileService.getProfile(userId);

        ApiResponse<UserProfileDataDto> response = ApiResponse.<UserProfileDataDto>builder()
                .status(HttpStatus.OK.value())
                .message("Returned user profile successfully")
                .data(profile)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("")
    public ResponseEntity<ApiResponse<UserProfileDataDto>> updateProfile(
            @Valid @RequestBody UpdateUserProfileDto updateDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserProfileDataDto profile = profileService.updateProfile(userPrincipal.getId(), updateDto);

        ApiResponse<UserProfileDataDto> response = ApiResponse.<UserProfileDataDto>builder()
                .status(HttpStatus.OK.value())
                .message("Update user profile successfully")
                .data(profile)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
