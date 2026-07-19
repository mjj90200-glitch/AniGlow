package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.auth.*;
import com.aniglow.security.AuthBridgeVerifier;
import com.aniglow.security.UserDetailsImpl;
import com.aniglow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthBridgeVerifier authBridgeVerifier;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return success("登录成功", authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return success("注册成功", authService.register(request));
    }

    @PostMapping("/authing-login")
    public ResponseEntity<ApiResponse<AuthResponse>> authingLogin(
            @RequestHeader(value = "X-Auth-Bridge-Secret", required = false) String bridgeSecret,
            @Valid @RequestBody AuthingLoginRequest request) {
        authBridgeVerifier.requireValid(bridgeSecret);
        return success("登录成功", authService.authingLogin(request));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthResponse>> me(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success(authService.me(userDetails.getId())));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthResponse>> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody ProfileUpdateRequest request) {
        return success("资料已更新", authService.updateProfile(userDetails.getId(), request));
    }

    private <T> ResponseEntity<ApiResponse<T>> success(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
}
