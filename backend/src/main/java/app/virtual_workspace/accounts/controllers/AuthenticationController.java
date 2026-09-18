package app.virtual_workspace.accounts.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.virtual_workspace.accounts.dtos.auth.AuthRequestDto;
import app.virtual_workspace.accounts.dtos.auth.AuthResponseDto;
import app.virtual_workspace.accounts.dtos.auth.CreateUserRequestDto;
import app.virtual_workspace.accounts.dtos.auth.LogoutRequestDto;
import app.virtual_workspace.accounts.dtos.data.UserDataDto;
import app.virtual_workspace.accounts.services.RefreshTokenService;
import app.virtual_workspace.accounts.services.UserAuthService;
import app.virtual_workspace.accounts.services.UserService;
import app.virtual_workspace.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserAuthService userAuthService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(@Valid @RequestBody CreateUserRequestDto request) {
        AuthResponseDto authResponseDto = userAuthService.register(request);
        ApiResponse<AuthResponseDto> response = ApiResponse.<AuthResponseDto>builder()
                .status(HttpStatus.CREATED.value())
                .message("User registered successfully")
                .data(authResponseDto)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody AuthRequestDto user) {
        AuthResponseDto authResponseDto = userAuthService.login(user);
        ApiResponse<AuthResponseDto> response = ApiResponse.<AuthResponseDto>builder()
                .status(HttpStatus.OK.value())
                .message("User logged in successfully")
                .data(authResponseDto)
                .build();

        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDto>> refreshToken(@RequestBody Map<String, String> payload) {
        AuthResponseDto authResponseDto = refreshTokenService.refreshToken(payload);

        ApiResponse<AuthResponseDto> response = ApiResponse.<AuthResponseDto>builder()
                .status(HttpStatus.OK.value())
                .message("Token refreshed successfully")
                .data(authResponseDto)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequestDto request) {
        refreshTokenService.revokeToken(request.refreshToken());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Logged out successfully.")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/data")
    public ResponseEntity<ApiResponse<UserDataDto>> userData(@PathVariable Long userId) {
        UserDataDto userDataDto = userService.userData(userId);
        ApiResponse<UserDataDto> response = ApiResponse.<UserDataDto>builder()
                .status(HttpStatus.OK.value())
                .message("Retrieved User data successfully")
                .data(userDataDto)
                .build();

        return ResponseEntity.ok().body(response);
    }
}
