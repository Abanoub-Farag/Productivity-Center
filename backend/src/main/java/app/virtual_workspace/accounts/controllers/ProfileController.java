package app.virtual_workspace.accounts.controllers;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.accounts.dtos.profile.UpdateUserProfileDto;
import app.virtual_workspace.accounts.dtos.profile.UserProfileDataDto;
import app.virtual_workspace.accounts.services.ProfileService;
import app.virtual_workspace.shared.dtos.ApiResponse;
import app.virtual_workspace.shared.services.FileStorageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag (
    name = "Profiles",
    description = "Operations for managing users profile"
)
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final FileStorageService fileStorageService;

    @SecurityRequirement (name = "bearerAuth")
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

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileDataDto>> updateProfileMultipart(
            @Valid @ModelAttribute UpdateUserProfileDto updateDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserProfileDataDto profile = profileService.updateProfile(userPrincipal.getId(), updateDto);

        ApiResponse<UserProfileDataDto> response = ApiResponse.<UserProfileDataDto>builder()
                .status(HttpStatus.OK.value())
                .message("Update user profile successfully")
                .data(profile)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping(value = "/pfp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileDataDto>> uploadProfilePicture(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        MultipartFile uploadFile = file != null ? file : profilePicture;
        if (uploadFile == null || uploadFile.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        UpdateUserProfileDto updateDto = UpdateUserProfileDto.builder()
                .profilePicture(uploadFile)
                .build();
        UserProfileDataDto profile = profileService.updateProfile(userPrincipal.getId(), updateDto);

        ApiResponse<UserProfileDataDto> response = ApiResponse.<UserProfileDataDto>builder()
                .status(HttpStatus.OK.value())
                .message("Profile picture uploaded successfully")
                .data(profile)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/pfp/{fileName:.+}")
    public ResponseEntity<Resource> serveProfileImage(@PathVariable String fileName) {
        Resource file = fileStorageService.loadFileAsResource(fileName);

        String contentType = "image/jpeg";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            contentType = "image/png";
        } else if (lower.endsWith(".webp")) {
            contentType = "image/webp";
        } else if (lower.endsWith(".gif")) {
            contentType = "image/gif";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

}
