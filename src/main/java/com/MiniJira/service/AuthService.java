package com.minijira.service;

import com.minijira.dto.request.LoginRequest;
import com.minijira.dto.request.RegisterRequest;
import com.minijira.dto.response.AuthResponse;
import com.minijira.dto.response.UserResponse;
import com.minijira.entity.Organization;
import com.minijira.entity.RefreshToken;
import com.minijira.entity.User;
import com.minijira.enums.Role;
import com.minijira.exception.EmailAlreadyExistsException;
import com.minijira.exception.InvalidCredentialsException;
import com.minijira.exception.ResourceNotFoundException;
import com.minijira.repository.OrganizationRepository;
import com.minijira.repository.RefreshTokenRepository;
import com.minijira.repository.UserRepository;
import com.minijira.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", request.getOrganizationId()));

        User user = User.builder()
                .organization(organization)
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.MEMBER)
                .build();

        User saved = userRepository.save(user);

        return toResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String tokenString) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new com.minijira.exception.InvalidTokenException("Invalid refresh token"));

        if (token.isRevoked()) {
            // Token reuse detected! Someone used a revoked token.
            // Revoke ALL tokens for this user immediately.
            refreshTokenRepository.revokeAllUserTokens(token.getUser().getId());
            throw new com.minijira.exception.InvalidTokenException("Refresh token was revoked. All user sessions terminated.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new com.minijira.exception.InvalidTokenException("Refresh token has expired");
        }

        // Token is valid. Rotate it (invalidate old, issue new)
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        return generateAuthResponse(token.getUser());
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshTokenString = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .expiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1000000))
                .build();
        
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .user(toResponse(user))
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .build();
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .organizationId(user.getOrganization().getId())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
