package app.virtual_workspace.accounts.services;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.accounts.dtos.auth.AuthResponseDto;
import app.virtual_workspace.accounts.models.RefreshToken;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.repositories.RefreshTokenRepository;
import app.virtual_workspace.accounts.repositories.UserRepository;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.exceptions.custom.TokenRefreshException;
import app.virtual_workspace.security.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refreshExpiration}")
    private Long refreshExpiration;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var refreshToken = refreshTokenRepository.findByUserId(userId)
                .orElseGet(() -> RefreshToken.builder()
                        .user(user)
                        .build());

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration));

        return refreshTokenRepository.save(refreshToken);
    }

    public boolean isTokenExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }

    @Transactional
    public AuthResponseDto refreshToken(Map<String, String> payload) {

        String requestToken = payload.get("refreshToken");

        var refreshToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token."));

        if (isTokenExpired(refreshToken)) {
            throw new TokenRefreshException("Refresh token expired. Please login again.");
        }

        User user = refreshToken.getUser();
        String newJwt = jwtService.generateToken(user.getEmail(), user.getId());

        String newRefreshToken = UUID.randomUUID().toString();
        refreshToken.setToken(newRefreshToken);
        refreshTokenRepository.save(refreshToken);

        return AuthResponseDto.builder().jwtToken(newJwt).refreshToken(newRefreshToken).build();

    }

    @Transactional
    public void revokeToken(String tokenValue) {
        var token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token."));

        refreshTokenRepository.delete(token);
    }

}
