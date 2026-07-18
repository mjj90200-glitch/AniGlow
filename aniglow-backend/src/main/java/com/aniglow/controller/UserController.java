package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.user.UpdateUserRequest;
import com.aniglow.dto.user.UserDto;
import com.aniglow.entity.User;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.repository.RatingRepository;
import com.aniglow.repository.UserRepository;
import com.aniglow.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户", description = "用户信息管理接口")
public class UserController {

    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取登录用户的详细信息",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userDetails.getId()));
        return ResponseEntity.ok(ApiResponse.success(convertToDto(user)));
    }

    @PutMapping("/me")
    @Operation(summary = "更新当前用户信息", description = "修改当前登录用户的信息",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userDetails.getId()));

        // 更新邮箱
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("邮箱已被使用"));
            }
            user.setEmail(request.getEmail());
        }

        // 更新头像
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // 更新密码
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("用户信息已更新", convertToDto(updatedUser)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户公开信息", description = "获取指定用户的公开资料")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", id));
        return ResponseEntity.ok(ApiResponse.success(convertToDto(user)));
    }

    private UserDto convertToDto(User user) {
        long ratingsCount = ratingRepository.countByUserId(user.getId());
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .ratingsCount(ratingsCount)
                .build();
    }
}
