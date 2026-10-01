package com.aniglow.service;

import com.aniglow.dto.auth.*;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.repository.UserRepository;
import com.aniglow.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserDisplayNameResolver displayNameResolver;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        UserDetailsImpl details = (UserDetailsImpl) authentication.getPrincipal();
        userRepository.updateLastLoginTime(details.getId(), LocalDateTime.now());
        User user = findUser(details.getId());
        return response(user, tokenService.issue(details), roles(details));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        try {
            User user = userRepository.saveAndFlush(User.builder()
                    .username(username)
                    .displayName(normalizeDisplayName(request.getDisplayName()))
                    .password(passwordEncoder.encode(request.getPassword()))
                    .credentialsInitialized(true)
                    .role(User.Role.USER).isActive(true).build());
            UserDetailsImpl details = UserDetailsImpl.build(user);
            return response(user, tokenService.issue(details), List.of("ROLE_USER"));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }

    @Transactional
    public AuthResponse authingLogin(AuthingLoginRequest request) {
        User user = findAuthingUser(request).orElseGet(() -> createAuthingUser(request));
        boolean changed = false;
        String phone = normalizePhone(request.getPhone());
        if (displayNameResolver.hasText(phone) && !phone.equals(user.getPhone())) {
            user.setPhone(phone);
            changed = true;
        }
        if (!request.getAuthingId().equals(user.getAuthingId())) {
            user.setAuthingId(request.getAuthingId());
            changed = true;
        }
        if (!displayNameResolver.hasText(user.getDisplayName())
                && displayNameResolver.isSafePublicName(request.getUsername(), phone)) {
            user.setDisplayName(request.getUsername().trim());
            changed = true;
        }
        if (!displayNameResolver.hasText(user.getAvatarUrl()) && displayNameResolver.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
            changed = true;
        }
        if (changed) userRepository.save(user);
        userRepository.updateLastLoginTime(user.getId(), LocalDateTime.now());
        UserDetailsImpl details = UserDetailsImpl.build(user);
        return response(user, tokenService.issue(details), roles(details));
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        TokenService.RefreshedSession session = tokenService.refresh(request.getRefreshToken());
        UserDetailsImpl details = (UserDetailsImpl) session.userDetails();
        return response(findUser(details.getId()), session.tokens(), roles(details));
    }

    public void logout(LogoutRequest request) {
        tokenService.revoke(request.getAccessToken(), request.getRefreshToken());
    }

    @Transactional
    public AuthResponse setCredentials(Long userId, SetCredentialsRequest request) {
        User user = findUser(userId);
        String username = request.getUsername().trim();
        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("用户名已存在");
                });
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCredentialsInitialized(true);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("用户名已存在");
        }
        UserDetailsImpl details = UserDetailsImpl.build(user);
        return response(user, tokenService.issue(details), roles(details));
    }

    @Transactional(readOnly = true)
    public AuthResponse me(Long userId) {
        User user = findUser(userId);
        UserDetailsImpl details = UserDetailsImpl.build(user);
        return response(user, null, null, roles(details));
    }

    @Transactional
    public AuthResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = findUser(userId);
        if (displayNameResolver.hasText(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        user.setAvatarUrl(request.getAvatarUrl() == null ? "" : request.getAvatarUrl().trim());
        userRepository.save(user);
        UserDetailsImpl details = UserDetailsImpl.build(user);
        return response(user, null, null, roles(details));
    }

    private User createAuthingUser(AuthingLoginRequest request) {
        String phone = normalizePhone(request.getPhone());
        String username = displayNameResolver.hasText(phone) ? phone : "auth_" + sanitizeId(request.getAuthingId());
        if (userRepository.existsByUsername(username)) {
            username += "_" + request.getAuthingId().substring(0, Math.min(8, request.getAuthingId().length()));
        }
        String email = request.getEmail();
        if (!displayNameResolver.hasText(email)) {
            String identity = displayNameResolver.hasText(phone) ? phone : sanitizeId(request.getAuthingId());
            email = identity + "@authing.user";
        }
        if (userRepository.existsByEmail(email)) {
            email = UUID.randomUUID().toString().substring(0, 8) + "@authing.user";
        }
        return userRepository.save(User.builder()
                .username(username).email(email).password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .phone(phone)
                .displayName(displayNameResolver.isSafePublicName(request.getUsername(), phone)
                        ? request.getUsername().trim() : null)
                .authingId(request.getAuthingId()).avatarUrl(request.getAvatarUrl())
                .credentialsInitialized(false)
                .isActive(true).role(User.Role.USER).build());
    }

    private Optional<User> findAuthingUser(AuthingLoginRequest request) {
        Optional<User> byAuthingId = userRepository.findByAuthingId(request.getAuthingId());
        if (byAuthingId.isPresent()) return byAuthingId;
        String phone = normalizePhone(request.getPhone());
        if (!displayNameResolver.hasText(phone)) return Optional.empty();
        return userRepository.findByPhone(phone).or(() -> userRepository.findByUsername(phone));
    }

    private AuthResponse response(User user, TokenService.TokenPair tokens, List<String> roles) {
        return response(user, tokens == null ? null : tokens.accessToken(),
                tokens == null ? null : tokens.refreshToken(), roles);
    }

    private AuthResponse response(User user, String token, String refreshToken, List<String> roles) {
        return AuthResponse.builder()
                .token(token).refreshToken(refreshToken).type(token == null ? null : "Bearer")
                .id(user.getId()).username(user.getUsername()).displayName(displayNameResolver.resolveProfile(user))
                .avatarUrl(user.getAvatarUrl()).phone(user.getPhone())
                .profileComplete(displayNameResolver.hasText(user.getDisplayName())
                        && !user.getDisplayName().equals(user.getPhone()))
                .credentialsInitialized(Boolean.TRUE.equals(user.getCredentialsInitialized()))
                .email(user.getEmail()).roles(roles)
                .expiresIn(token == null ? null : tokenService.getAccessTokenExpiration()).build();
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", id));
    }

    private List<String> roles(UserDetailsImpl details) {
        return details.getAuthorities().stream().map(item -> item.getAuthority()).toList();
    }

    private String normalizePhone(String phone) {
        return displayNameResolver.hasText(phone) ? phone.trim() : null;
    }

    private String normalizeDisplayName(String displayName) {
        return displayNameResolver.hasText(displayName) ? displayName.trim() : null;
    }

    private String sanitizeId(String id) {
        if (!displayNameResolver.hasText(id)) {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        return id.replaceAll("[^a-zA-Z0-9]", "");
    }
}
