package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.auth.AuthingLoginRequest;
import com.aniglow.dto.auth.AuthResponse;
import com.aniglow.dto.auth.LoginRequest;
import com.aniglow.dto.auth.ProfileUpdateRequest;
import com.aniglow.dto.auth.RegisterRequest;
import com.aniglow.entity.User;
import com.aniglow.repository.UserRepository;
import com.aniglow.security.JwtUtils;
import com.aniglow.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "用户认证相关接口")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录")
    @Transactional
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 更新最后登录时间
        userRepository.updateLastLoginTime(userDetails.getId(), LocalDateTime.now());

        String jwt = jwtUtils.generateToken(userDetails);
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();

        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        AuthResponse response = buildAuthResponse(user, jwt, refreshToken, roles);

        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    /**
     * 传统用户名+邮箱+密码注册。
     * 注意：当前前端注册流程走 Authing（手机号+验证码），通过 /api/auth/authing-login 桥接后端。
     * 此接口保留用于传统注册场景，不作为前端主流程使用。
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户（传统方式）")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户名已存在"));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("邮箱已被注册"));
        }

        // 创建新用户
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .isActive(true)
                .build();

        userRepository.save(user);

        // 自动登录
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String jwt = jwtUtils.generateToken(userDetails);
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);

        AuthResponse response = buildAuthResponse(user, jwt, refreshToken, List.of("ROLE_USER"));

        return ResponseEntity.ok(ApiResponse.success("注册成功", response));
    }

    @PostMapping("/authing-login")
    @Operation(summary = "Authing 登录桥接", description = "通过 Authing 认证后获取后端 JWT")
    @Transactional
    public ResponseEntity<ApiResponse<AuthResponse>> authingLogin(@Valid @RequestBody AuthingLoginRequest request) {
        User user = findAuthingUser(request)
                .orElseGet(() -> createAuthingUser(request));

        boolean changed = false;
        if (hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            user.setPhone(request.getPhone());
            changed = true;
        }
        if (hasText(request.getAuthingId()) && !request.getAuthingId().equals(user.getAuthingId())) {
            user.setAuthingId(request.getAuthingId());
            changed = true;
        }
        // 仅在用户尚未设置个性化资料时，使用 Authing 提供的信息作为初始值
        // 一旦用户在番舍内设置了昵称/头像，后续登录不再覆盖
        if (!hasText(user.getDisplayName()) && isUsableDisplayName(request.getUsername(), request.getPhone())) {
            user.setDisplayName(request.getUsername().trim());
            changed = true;
        }
        if (!hasText(user.getAvatarUrl()) && hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }

        userRepository.updateLastLoginTime(user.getId(), LocalDateTime.now());

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        String jwt = jwtUtils.generateToken(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();

        AuthResponse response = buildAuthResponse(user, jwt, null, roles);

        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户资料")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthResponse>> me(Authentication authentication) {
        User user = currentUser(authentication);
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(buildAuthResponse(user, null, null, roles)));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新当前用户资料", description = "同步昵称和头像到后端，保证换设备后仍能恢复")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthResponse>> updateProfile(
            Authentication authentication,
            @RequestBody ProfileUpdateRequest request
    ) {
        User user = currentUser(authentication);
        if (hasText(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        user.setAvatarUrl(request.getAvatarUrl() == null ? "" : request.getAvatarUrl().trim());
        userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();
        return ResponseEntity.ok(ApiResponse.success("资料已更新", buildAuthResponse(user, null, null, roles)));
    }

    private User createAuthingUser(AuthingLoginRequest request) {
        String phone = normalizePhone(request.getPhone());
        String username = hasText(phone) ? phone : "auth_" + sanitizeId(request.getAuthingId());

        // 确保用户名唯一
        String finalUsername = username;
        if (userRepository.existsByUsername(username)) {
            finalUsername = username + "_" + request.getAuthingId().substring(0, Math.min(8, request.getAuthingId().length()));
        }

        String email = request.getEmail();
        if (email == null || email.isEmpty()) {
            email = (request.getPhone() != null ? request.getPhone() : request.getAuthingId().replaceAll("[^a-zA-Z0-9]", "")) + "@authing.user";
        }

        // 确保邮箱唯一
        if (userRepository.existsByEmail(email)) {
            email = UUID.randomUUID().toString().substring(0, 8) + "@authing.user";
        }

        User user = User.builder()
                .username(finalUsername)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .phone(phone)
                .displayName(isUsableDisplayName(request.getUsername(), phone) ? request.getUsername().trim() : null)
                .authingId(request.getAuthingId())
                .avatarUrl(request.getAvatarUrl())
                .isActive(true)
                .role(User.Role.USER)
                .build();

        return userRepository.save(user);
    }

    private Optional<User> findAuthingUser(AuthingLoginRequest request) {
        Optional<User> byAuthingId = userRepository.findByAuthingId(request.getAuthingId());
        if (byAuthingId.isPresent()) {
            return byAuthingId;
        }
        String phone = normalizePhone(request.getPhone());
        if (hasText(phone)) {
            return userRepository.findByPhone(phone)
                    .or(() -> userRepository.findByUsername(phone));
        }
        return Optional.empty();
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new IllegalStateException("用户未登录");
        }
        return userRepository.findById(userDetails.getId()).orElseThrow();
    }

    private AuthResponse buildAuthResponse(User user, String token, String refreshToken, List<String> roles) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .type(token == null ? null : "Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .displayName(resolveDisplayName(user))
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .profileComplete(isProfileComplete(user))
                .email(user.getEmail())
                .roles(roles)
                .expiresIn(token == null ? null : jwtUtils.getExpirationTime())
                .build();
    }

    private boolean isProfileComplete(User user) {
        return hasText(user.getDisplayName()) && !user.getDisplayName().equals(user.getPhone());
    }

    private String resolveDisplayName(User user) {
        if (hasText(user.getDisplayName())) return user.getDisplayName();
        String username = user.getUsername();
        if (hasText(username) && !username.equals(user.getPhone()) && !username.matches("^1\\d{10}$")) {
            return username;
        }
        return "";
    }

    private boolean isUsableDisplayName(String name, String phone) {
        return hasText(name) && !name.trim().equals(phone) && !"番舍同好".equals(name.trim());
    }

    private String normalizePhone(String phone) {
        return hasText(phone) ? phone.trim() : null;
    }

    private String sanitizeId(String id) {
        if (!hasText(id)) return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return id.replaceAll("[^a-zA-Z0-9]", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
