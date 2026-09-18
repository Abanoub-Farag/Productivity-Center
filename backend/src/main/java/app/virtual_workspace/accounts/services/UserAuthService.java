package app.virtual_workspace.accounts.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.accounts.dtos.auth.AuthRequestDto;
import app.virtual_workspace.accounts.dtos.auth.AuthResponseDto;
import app.virtual_workspace.accounts.dtos.auth.CreateUserRequestDto;
import app.virtual_workspace.accounts.events.UserRegisteredEvent;
import app.virtual_workspace.accounts.mappers.AuthMapper;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.repositories.UserRepository;
import app.virtual_workspace.exceptions.custom.ResourceAlreadyExistsException;
import app.virtual_workspace.security.JwtService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public AuthResponseDto register(CreateUserRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "An account with this email already exists. Please log in or use a different address.");
        }

        User user = authMapper.registerDtoToModel(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        UserRegisteredEvent event = new UserRegisteredEvent(savedUser.getId());
        applicationEventPublisher.publishEvent(event);

        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getId());
        String refreshToken = refreshTokenService.createRefreshToken(savedUser.getId()).getToken();

        return AuthResponseDto.builder().jwtToken(token).refreshToken(refreshToken).build();
    }

    @Transactional
    public AuthResponseDto login(AuthRequestDto request) {
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findUserByEmail(request.getEmail())
                .orElseThrow(() -> new InsufficientAuthenticationException("User not found"));

        String token = jwtService.generateToken(user.getEmail(), user.getId());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
        return AuthResponseDto.builder().jwtToken(token).refreshToken(refreshToken).build();
    }

    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new InsufficientAuthenticationException("User not authenticated");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findUserByEmail(principal.getEmail())
                .orElseThrow(() -> new InsufficientAuthenticationException("User not found"));
    }

    public boolean isOwner(Long userId) {
        User user = getAuthenticatedUser();

        return java.util.Objects.equals(userId, user.getId());
    }
}